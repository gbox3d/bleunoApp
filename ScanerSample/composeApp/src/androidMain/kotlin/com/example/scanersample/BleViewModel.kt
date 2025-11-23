package com.example.scanersample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleViewModel : ViewModel() {
    private val bleManager = BleScanManager(viewModelScope)

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    fun toggleScan() {
        if (_isScanning.value) {
            stopScan()
        } else {
            startScan()
        }
    }

    private fun startScan() {
        bleManager.startScan()
        _isScanning.value = true
    }

    private fun stopScan() {
        bleManager.stopScan()
        _isScanning.value = false
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.dispose()
    }
}