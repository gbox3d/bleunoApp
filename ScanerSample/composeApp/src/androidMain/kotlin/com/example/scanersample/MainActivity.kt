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

class MainActivity : ComponentActivity() {

    private val TAG = "BLE_SCAN_MainActivity"
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


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                requestPermissions = ::requestBlePermissions,
                hasPermissions = ::hasBlePermissions,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}