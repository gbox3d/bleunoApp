package com.example.bleunotest

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.UUID

data class BleDevice(
    val address: String,
    val name: String,
    var rssi: Int
)

class MainActivity : AppCompatActivity() {

    private lateinit var btnScan: Button
    private lateinit var btnConnect: Button
    private lateinit var btnGetTemper : Button
    private lateinit var btnRead : Button
    private lateinit var btnOffAll : Button
    private lateinit var btnOnAll : Button
    private lateinit var tvInfo : TextView

    private lateinit var btnFW : Button
    private lateinit var btnBW : Button

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothLeScanner : BluetoothLeScanner

    private var isScanning = false

    private val mHandlerBleScanTimeout = Handler(Looper.getMainLooper())

    private val mBleScannedDevices = mutableListOf<BleDevice>()

    private var mBleGatt : BluetoothGatt? = null

    private var mSelectedDevice : BluetoothDevice? = null
    private var mCharacteristicObj : BluetoothGattCharacteristic? = null


    private lateinit var permissionLauncher : ActivityResultLauncher<Array<String>>

    private lateinit var etSendCmd: EditText
    private lateinit var btnSendCmd: Button

    private fun _updateDeviceList(name: String, address: String, rssi: Int) {

        Log.d("MainActivity", "name: $name, address: $address, rssi: $rssi")

        val existingDevice = mBleScannedDevices.find { it.address == address }
        if (existingDevice != null) {
            existingDevice.rssi = rssi
        } else {
            val newDevice = BleDevice(address, name, rssi)
            mBleScannedDevices.add(newDevice)
        }

        Log.d("MainActivity", "mBleScannedDevices: $mBleScannedDevices")

        // UI 업데이트는 메인 스레드에서 수행해야 함
        runOnUiThread {
            tvInfo.text = "found devices: ${mBleScannedDevices.size}"
        }


    }

    private val mScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device

            if(device == null) return

            val deviceAddress = device.address
            val rssi = result.rssi

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // android 12 or higher
                if(checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT))
                    return
                }


                _updateDeviceList(device?.name?: "no name", deviceAddress, rssi)

            }
            else {
                if(checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION))
                    return
                }
                _updateDeviceList(device?.name?: "no name", deviceAddress, rssi)
            }
        }

    }


    private fun connectToDevice(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // 권한 요청 코드 작성
                return
            }
        }
        mBleGatt = device.connectGatt(this, false, mGattCallback)
    }

//    private fun handleReceivedData(data: ByteArray) {
//        // 데이터 처리 로직 구현
//        // Log.d("MainActivity", "Received data: ${data.joinToString()}")
//        // ByteArray를 UTF-8 문자열로 변환
//        val receivedString = data.toString(Charsets.UTF_8)
//        Log.d("MainActivity", "Received data: $receivedString")
//        runOnUiThread {
//            tvInfo.append("\nReceived data: $receivedString")
//        }
//    }

    private fun appendStringTvLog(txt: String) {
        runOnUiThread {
            tvInfo.append("\n$txt")
        }
    }

    private val mGattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("MainActivity", "MTU changed to $mtu")
                gatt?.discoverServices()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "MTU changed to $mtu", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("MainActivity", "MTU change failed with status $status")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "MTU change failed with status $status", Toast.LENGTH_SHORT).show()
                }
            }
            // 🔸 실패했으면 어차피 기본 MTU로라도 써야 하니까, 여기서 fallback으로 서비스 탐색:
            gatt?.discoverServices()
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d("MainActivity", "onConnectionStateChange: STATE_CONNECTED")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // 권한 요청 코드 작성
                        return
                    }
                }


                // 🔹 여기서 바로 서비스 탐색하지 말고 MTU 먼저 요청
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val req = gatt?.requestMtu(185)   // 원하는 MTU 값 (예: 185)
                    Log.d("MainActivity", "requestMtu(185) called, result=$req")
                } else {
                    // 아주 구형 기기면 MTU negotiation 안 될 수 있으므로 바로 서비스 탐색
                    gatt?.discoverServices()
                }


                runOnUiThread {
                    btnGetTemper.isEnabled = true
                    btnConnect.isEnabled = true
                    btnConnect.text = "disconnect"
                    tvInfo.text = "connected"
                }


            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d("MainActivity", "onConnectionStateChange: STATE_DISCONNECTED")
                mBleGatt?.close()
                mBleGatt = null
                mCharacteristicObj = null
                runOnUiThread {
                    btnGetTemper.isEnabled = false
                    btnConnect.isEnabled = false
                    btnConnect.text = "connect"
                    tvInfo.text = "disconnected"
                }
            }
            else {
                Log.d("MainActivity", "onConnectionStateChange: $newState")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "onConnectionStateChange: $newState", Toast.LENGTH_SHORT).show()
//                    tvInfo.text = "onConnectionStateChange: $newState"
//                    btnConnect.isEnabled = true
//                    btnConnect.text = "connect"
                }
            }

        }

        @SuppressLint("MissingPermission")
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

            val serviceUuid = UUID.fromString(myBlueTooth.SERVICE_UUID)
            val characteristicUuid = UUID.fromString(myBlueTooth.CHARACTERISTIC_UUID)

            val service = gatt?.getService(serviceUuid)
            if (service != null) {
                val characteristic = service.getCharacteristic(characteristicUuid)
                if (characteristic != null) {
                    // 특성 객체를 성공적으로 얻음
                    Log.d("MainActivity", "Characteristic obtained: $characteristic")

                    // TODO: 특성 읽기, 쓰기 또는 알림 설정 등 추가 작업 수행
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(
                        UUID.fromString(myBlueTooth.CLIENT_CHARACTERISTIC_CONFIG_UUID) // Client Characteristic Configuration UUID
                    )

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // API 33 이상에서는 새로운 메서드 사용
                        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    }
                    else {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)

                    }

                    mCharacteristicObj = characteristic

                    runOnUiThread {
//                        btnConnect.text = "disconnect"
                        tvInfo.text = "Characteristic obtained: $characteristic"
                    }

                } else {
                    Log.w("MainActivity", "Characteristic not found.")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Characteristic not found.", Toast.LENGTH_SHORT).show()
                        tvInfo.text = "Characteristic not found."
                    }
                }
            } else {
                Log.w("MainActivity", "Service not found.")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Service not found.", Toast.LENGTH_SHORT).show()
                    tvInfo.text = "Service not found."
                }
            }
        }

        //알람을 받았을떼 , notify data
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (characteristic?.uuid == mCharacteristicObj?.uuid) {
                val data = characteristic?.value
                if (data == null) return
                // 수신된 데이터 처리
                Log.d("MainActivity", "notify : ${data.joinToString()}")
                val _msg = "notify : ${data.toString(Charsets.UTF_8)}"
                Log.d("MainActivity", _msg)
                appendStringTvLog(_msg)
            }
        }



        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic?.value
                if (data == null) return
                // 수신된 데이터 처리
                Log.d("MainActivity", "read : ${data.joinToString()}")
                val _msg = "read : ${data.toString(Charsets.UTF_8)}"
                Log.d("MainActivity", _msg)
                appendStringTvLog(_msg)
            }
        }


    }

    private fun showDeviceSelectionDialog() {
        if (mBleScannedDevices.isEmpty()) {
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show()
            return
        }

        // 기기 이름과 주소를 문자열 배열로 변환
        val deviceNames = mBleScannedDevices.map { device ->
            val name = device.name ?: "Unknown Device"
            "$name\n${device.address}\nRSSI: ${device.rssi}\n--------------------------"
        }.toTypedArray()

        // AlertDialog 생성
        AlertDialog.Builder(this)
            .setTitle("Select a Device")
            .setItems(deviceNames) { dialog, which ->
                val selectedDevice = mBleScannedDevices[which]
                // 선택된 기기에 대한 처리
                Toast.makeText(
                    this,
                    "Selected: ${selectedDevice.name ?: "Unknown Device"}",
                    Toast.LENGTH_SHORT
                ).show()

                // TODO: 선택된 기기와의 연결 등 추가 작업 수행
//                connectToDevice(bluetoothAdapter.getRemoteDevice(selectedDevice.address))

                mSelectedDevice = bluetoothAdapter.getRemoteDevice(selectedDevice.address)
                tvInfo.text = "selected: ${selectedDevice.name ?: "Unknown Device"}"
                btnConnect.isEnabled = true

            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun _sendData(command: String) {
        mCharacteristicObj?.let { characteristic ->
            mBleGatt?.let { gatt ->
                val writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                val commandBytes = command.toByteArray()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13 이상
                    val status = gatt.writeCharacteristic(characteristic, commandBytes, writeType)
                    if (status == BluetoothStatusCodes.SUCCESS) {
                        Log.d("MainActivity", "Command sent successfully: $command")
                    } else {
                        Log.e("MainActivity", "Failed to send command: $command, Status: $status")
                    }
                } else {
                    // Android 12 이하
                    characteristic.value = commandBytes
                    val success = gatt.writeCharacteristic(characteristic)
                    if (success) {
                        Log.d("MainActivity", "Command sent successfully: $command")
                    } else {
                        Log.e("MainActivity", "Failed to send command: $command")
                    }
                }
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            var allPermissionGranted = true

            permissions.entries.forEach { entry ->
                val permissionName = entry.key
                val isGranted = entry.value
                if (!isGranted) {
                    allPermissionGranted = false
                    Log.d("MainActivity", "permission denied: $permissionName")
                }
                else {
                    Log.d("MainActivity", "permission granted: $permissionName")
                }
            }

            if (allPermissionGranted) {
                Log.d("MainActivity", "all permission granted")
                Toast.makeText(this, "all permission granted", Toast.LENGTH_SHORT).show()

            }
            else {
                Log.d("MainActivity", "some permission denied")
                Toast.makeText(this, "some permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        btnScan = findViewById(R.id.btnScan)
        btnConnect = findViewById(R.id.btnConnect)
        btnGetTemper = findViewById(R.id.btnGetTemper)
        btnRead = findViewById(R.id.btnRead)

        btnFW = findViewById(R.id.btnFW)
        btnBW = findViewById(R.id.btnBW)

        btnOffAll = findViewById(R.id.btnOffAll)
        btnOnAll = findViewById(R.id.btnOnAll)

        tvInfo = findViewById(R.id.tvInfo)

        etSendCmd = findViewById(R.id.sendCmd)
        btnSendCmd = findViewById(R.id.btnSendCmd)


        btnScan.setOnClickListener { v  ->
            //TODO
            v as Button

            if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.d("MainActivity", "bluetooth not enabled")
                Toast.makeText(this, "bluetooth not enabled", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
            }
            else {

                val permissionToRequest = mutableListOf<String>()

                Log.d("MainActivity", "stop scan")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // android 12 or higher
                    if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        permissionToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
                    }
                    if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        permissionToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    if(checkSelfPermission((android.Manifest.permission.ACCESS_FINE_LOCATION)) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        permissionToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }

                if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionToRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                if(checkSelfPermission((android.Manifest.permission.ACCESS_FINE_LOCATION)) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }


                if (permissionToRequest.isNotEmpty()) {
                    permissionLauncher.launch(permissionToRequest.toTypedArray())
                    return@setOnClickListener
                } else {

                    if (isScanning) {
                        bluetoothLeScanner.stopScan(
                            mScanCallback
                        )
                        isScanning = false
                        v.text = "scan"

                    } else {

                        //디바이스 목록 클리어
                        mBleScannedDevices.clear()

                        bluetoothLeScanner.startScan(
                            mScanCallback
                        )
                        isScanning = true
                        v.text = "stop scan"

                        mHandlerBleScanTimeout.postDelayed({
                            bluetoothLeScanner.stopScan(
                                mScanCallback
                            )
                            isScanning = false
                            v.text = "scan"
                            Log.d("MainActivity", "scan timeout")

                            //dump list
                            Log.d("MainActivity", "mBleScannedDevices: $mBleScannedDevices")

                            showDeviceSelectionDialog()


                        }, 5000)

                    }

                }
            }
        }

        btnConnect.setOnClickListener { v ->
            v as Button
            if(mSelectedDevice == null) {
                Toast.makeText(this, "no device selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(mBleGatt == null) {
                v.text = "connecting..."
                v.isEnabled = false

                tvInfo.text = "connecting..."

                connectToDevice(mSelectedDevice!!)
            }
            else {
                //disconnect
                mBleGatt?.disconnect()
//                mBleGatt?.close()
//                mBleGatt = null
//                mCharacteristicObj = null

//                v.text = "connect"
//                tvInfo.text = "disconnected"
            }

        }

        btnGetTemper.setOnClickListener {

            if (mCharacteristicObj == null) {
                Toast.makeText(this, "No characteristic available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                _sendData("dht11")
            }
        }

        btnRead.setOnClickListener {
            if (mCharacteristicObj == null) {
                Toast.makeText(this, "No characteristic available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                // 특성 읽기
                val success = mBleGatt?.readCharacteristic(mCharacteristicObj)
                if (success == true) {
                    Log.d("MainActivity", "Characteristic read successfully")
                    Toast.makeText(this, "Characteristic read successfully", Toast.LENGTH_SHORT).show()
                }

            }

        }

        btnOffAll.setOnClickListener {
            _sendData("off -1")
        }
        btnOnAll.setOnClickListener {
            _sendData("on -1")
        }

        btnFW.setOnClickListener {
            _sendData("on 0\noff 1\non 2\noff 3")

        }
        btnBW.setOnClickListener {
            _sendData("on 1\noff 0\non 3\noff 2")

        }


        btnSendCmd.setOnClickListener {
            // 연결/Characteristic 체크
            if (mBleGatt == null || mCharacteristicObj == null) {
                Toast.makeText(this, "BLE가 연결되지 않았습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cmd = etSendCmd.text.toString().trim()
            if (cmd.isEmpty()) {
                Toast.makeText(this, "보낼 명령어를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 펌웨어가 '\n' 기준으로 명령어를 split 하므로 항상 개행 하나 붙여서 전송
            _sendData(cmd + "\n")

            // tvInfo 에 전송 내용 표시
            tvInfo.append("\n>> $cmd")

//            // (선택) 바로 read로 응답 확인도 시도
//            val success = mBleGatt?.readCharacteristic(mCharacteristicObj)
//            if (success == true) {
//                Log.d("MainActivity", "Read request sent after command")
//            } else {
//                Log.d("MainActivity", "Failed to send read request")
//            }
        }




    }
}


class myBlueTooth {
    companion object {
        const val SERVICE_UUID = "c6f8b088-2af8-4388-8364-ca2a907bdeb8"
        const val CHARACTERISTIC_UUID = "f6aa83ca-de53-46b4-bdea-28a7cb57942e"
        const val CLIENT_CHARACTERISTIC_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb"
        const val CHECK_CODE:UInt = 230815u
    }
}