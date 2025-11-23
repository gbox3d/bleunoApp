package com.example.scanersample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import scanersample.composeapp.generated.resources.Res
import scanersample.composeapp.generated.resources.compose_multiplatform

@Composable
@Preview
fun App(
    requestPermissions: (() -> Unit)? = null,
    hasPermissions: (() -> Boolean)? = null,
) {


    MaterialTheme {

        var message by remember { mutableStateOf("") }
        val vm_ble: BleViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        val isScanning by vm_ble.isScanning.collectAsState()

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            message = if (isScanning) "스캔 중.. (누르면 중지)" else "스캔 준비"

            Button(onClick = {
                if (hasPermissions?.invoke() == true) {
                    vm_ble.toggleScan()
                } else {
                    requestPermissions?.invoke()
                }
            }) {
                Text(if (isScanning) "stop" else "start")
            }

            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
            )

        }
    }
}