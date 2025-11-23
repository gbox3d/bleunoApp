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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import scanersample.composeapp.generated.resources.Res
import scanersample.composeapp.generated.resources.compose_multiplatform

@Composable
@Preview
fun App(
    onScanClick: (() -> Unit)? = null,
) {
    MaterialTheme {
//        var showContent by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = {
//                showContent = !showContent
                onScanClick?.invoke()                  // 🔹 버튼 누를 때 스캔 시작 요청
                message = "BLE 스캔을 시작했습니다.\nLogcat을 확인하세요."

            }) {
                Text("Start Scan")
            }
            Text(
                text =
                    if (message.isEmpty())
                        "아직 버튼을 누르지 않았습니다."
                    else
                        message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
            )
//            AnimatedVisibility(showContent) {
//                val greeting = remember { Greeting().greet() }
//                Column(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                ) {
//                    Image(painterResource(Res.drawable.compose_multiplatform), null)
//                    Text("Compose: $greeting")
//                }
//            }
        }
    }
}