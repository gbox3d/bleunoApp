package com.example.ex01scaner

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.juul.kable.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {


    private val permissions = arrayOf(
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    private fun ensurePermissions() {
        if (permissions.any {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }
        ) {
            requestPermissions(permissions, 1001)
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

        ensurePermissions()

        lifecycleScope.launch {
            val scanner = Scanner()

            scanner.advertisements.collect { adv ->
                Log.d("BLE", "디바이스 발견: ${adv.name ?: "이름 없음"} / ${adv.address}")
            }
        }
    }
}