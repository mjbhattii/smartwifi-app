package com.smartwifi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartwifi.logic.FastSpeedTestManager

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(
    onBack: () -> Unit,
    viewModel: SpeedTestViewModel = hiltViewModel()
) {
    val state by viewModel.testManager.testState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Speed Test", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                     IconButton(onClick = onBack) {
                         Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, "Back")
                     }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Gauge
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
                SpeedGauge(
                    currentValue = when(val s = state) {
                        is FastSpeedTestManager.TestState.Running -> s.speedMbps.toFloat()
                        is FastSpeedTestManager.TestState.Finished -> s.finalSpeedMbps.toFloat()
                        else -> 0f
                    },
                    maxValue = 100f // Dynamic scaling
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val displayValue = when(val s = state) {
                        is FastSpeedTestManager.TestState.Running -> s.speedMbps
                        is FastSpeedTestManager.TestState.Finished -> s.finalSpeedMbps
                        else -> 0.0
                    }
                    Text(
                        text = "%.1f".format(displayValue),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Mbps", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Status Text
            Text(
                text = when(val s = state) {
                    is FastSpeedTestManager.TestState.Idle -> "Ready to Test"
                    is FastSpeedTestManager.TestState.Preparing -> "Connecting to Fast.com..."
                    is FastSpeedTestManager.TestState.Running -> "Testing... ${(s.progress * 100).toInt()}%"
                    is FastSpeedTestManager.TestState.Finished -> "Test Completed"
                    is FastSpeedTestManager.TestState.Error -> "Error: ${s.message}"
                },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            // Action Button
            Button(
                onClick = { 
                    viewModel.startTest()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = state !is FastSpeedTestManager.TestState.Running && state !is FastSpeedTestManager.TestState.Preparing
            ) {
                Text(if (state is FastSpeedTestManager.TestState.Finished) "Test Again" else "Start Speed Test")
            }
            
            // Powered by
            Spacer(modifier = Modifier.weight(1f))
            Text("Powered by FAST.com", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun SpeedGauge(currentValue: Float, maxValue: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val sweepAngle = 240f
        val startAngle = 150f
        
        drawArc(
            color = Color.LightGray.copy(alpha = 0.3f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Progress Arc
        val progress = (currentValue / maxValue).coerceIn(0f, 1f)
        drawArc(
            color = Color(0xFF00C853), // Green
            startAngle = startAngle,
            sweepAngle = sweepAngle * progress,
            useCenter = false,
            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
