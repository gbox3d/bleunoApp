package com.example.scanersample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import com.juul.kable.Scanner
class MainActivity : ComponentActivity() {

    private val TAG = "BLE_SCAN"

    // 스캔 코루틴 Job (다시 눌렀을 때 중복 방지용)
    private var scanJob: Job? = null
    private var isScanning = false   // 🔹 UI가 인식해야 하는 상태값

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                onScanClick = ::startBleScan,
            )
        }
    }

    private val blePermissions: Array<String>
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            }
        }


    private fun hasBlePermissions(): Boolean {
        val result = blePermissions.all { perm ->
            val granted = ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "perm=$perm -> granted=$granted")
            granted
        }
        Log.d(TAG, "hasBlePermissions() = $result")
        return result
    }


    private fun requestBlePermissions() {
        ActivityCompat.requestPermissions(
            this,
            blePermissions,
            1001,
        )
    }

    private fun startBleScan() {
        if (!hasBlePermissions()) {
            Log.d(TAG, "BLE 권한이 없어 요청합니다.")
            requestBlePermissions()
            return
        }

        // 이미 스캔 중이면 다시 시작하지 않음 (필요하면 cancel 후 재시작 로직으로 바꿔도 됨)
        if (scanJob?.isActive == true) {
            Log.d(TAG, "이미 스캔 중입니다.")
            return
        }

        Log.d(TAG, "BLE 스캔 시작")

        scanJob = lifecycleScope.launch {
            // 0.40 버전 Scanner DSL 사용
            val scanner = Scanner {
                // 필요하면 나중에 필터 추가
                // filters {
                //     match {
                //         name = Filter.Name.Prefix("ESP")
                //     }
                // }
            }

            // Flow 수집 시작 = 스캔 시작
            scanner.advertisements.collect { adv ->
                val name = adv.name ?: "(이름 없음)"
                val addr = adv.address
                val rssi = adv.rssi
                Log.d(TAG, "📡 발견: name=$name, addr=$addr, rssi=$rssi")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isCan
        scanJob?.cancel()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}