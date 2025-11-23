package com.example.scanersample

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import com.juul.kable.Scanner

class BleScanManager(private val scope: CoroutineScope) {

    private val TAG = "BLE_SCAN"

    private var scanJob: Job? = null

    val isScanning: Boolean
        get() = scanJob?.isActive == true

    fun startScan() {
        if (isScanning) {
            Log.d(TAG, "이미 스캔 중입니다.")
            return
        }

        Log.d(TAG, "BLE 스캔 시작")

        scanJob = scope.launch {
            val scanner = Scanner { /* 필요하면 filters { ... } */ }

            scanner.advertisements.collect { adv ->
                val name = adv.name ?: "(이름 없음)"
                val addr = adv.address
                val rssi = adv.rssi
                Log.d(TAG, "📡 발견: name=$name, addr=$addr, rssi=$rssi")
            }
        }
    }

    fun stopScan() {
        if (!isScanning) return
        Log.d(TAG, "BLE 스캔 중단")
        scanJob?.cancel()
        scanJob = null
    }

    fun dispose() {
        stopScan()
    }


}