package com.smartwifi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartwifi.logic.FastSpeedTestManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: SpeedTestViewModel = hiltViewModel()
) {
    val testState by viewModel.testManager.testState.collectAsState()
    val metricData by viewModel.testManager.metricData.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Speed Test", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Powered by Fast.com", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                     IconButton(onClick = onBackClick) {
                         Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                     }
                },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        bottomBar = {
            Box(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(24.dp)
                     .height(56.dp)
                     .clip(MaterialTheme.shapes.extraLarge)
                     .background(
                          if (testState is FastSpeedTestManager.TestState.Running || testState is FastSpeedTestManager.TestState.Preparing) 
                              MaterialTheme.colorScheme.surfaceVariant 
                          else MaterialTheme.colorScheme.primary
                     )
                     .clickable(enabled = testState !is FastSpeedTestManager.TestState.Running && testState !is FastSpeedTestManager.TestState.Preparing) {
                         viewModel.startTest()
                     },
                 contentAlignment = Alignment.Center
            ) {
                if (testState is FastSpeedTestManager.TestState.Running) {
                     val state = testState as FastSpeedTestManager.TestState.Running
                     Box(
                         modifier = Modifier
                             .fillMaxHeight()
                             .fillMaxWidth(state.progress)
                             .align(Alignment.CenterStart)
                             .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) // Revert to primary tint
                     )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when(testState) {
                         is FastSpeedTestManager.TestState.Running -> {
                             if ((testState as FastSpeedTestManager.TestState.Running).phase == FastSpeedTestManager.TestPhase.DOWNLOAD) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward
                         }
                         is FastSpeedTestManager.TestState.Preparing -> null // No icon for connecting
                         else -> Icons.Rounded.PlayArrow
                    }
                    val text = when(testState) {
                        is FastSpeedTestManager.TestState.Preparing -> "Connecting..."
                        is FastSpeedTestManager.TestState.Running -> {
                            if ((testState as FastSpeedTestManager.TestState.Running).phase == FastSpeedTestManager.TestPhase.DOWNLOAD) "Downloading..." else "Uploading..."
                        }
                        is FastSpeedTestManager.TestState.Finished -> "Test Again"
                        else -> "Start Speed Test"
                    }
                    
                    val contentColor = if (testState is FastSpeedTestManager.TestState.Running || testState is FastSpeedTestManager.TestState.Preparing) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else MaterialTheme.colorScheme.onPrimary
                    
                    if (icon != null) {
                        Icon(
                            imageVector = icon, 
                            contentDescription = null, 
                            tint = contentColor
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    ) { padding ->
        val isFinished = testState is FastSpeedTestManager.TestState.Finished
        
        // Sequential Animation State
        // We only expand the content AFTER the gauge has fully faded out to prevent overlap/clutter.
        var contentExpanded by remember { mutableStateOf(false) }
        
        LaunchedEffect(isFinished) {
            if (isFinished) {
                // Immediate transition for simultaneous animation
                contentExpanded = true
            } else {
                contentExpanded = false
            }
        }
        
        // Font Scale Animation (Triggered by contentExpanded)
        val fontScale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (contentExpanded) 1.6f else 1f, 
            animationSpec = tween(1000)
        )
        
        // Explicit Height Animation
        // Target: 140.dp (base) + 12.dp (gap) + 220.dp (gauge) = 372.dp
        val cardsHeight by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (contentExpanded) 372.dp else 140.dp,
            animationSpec = tween(1000)
        )

        // Root Column with Scroll
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()), // Scrollable ROOT
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // Allow content to stack from top
        ) {
             // Static Top Spacer
             Spacer(modifier = Modifier.height(32.dp)) 


                    // Result Cards Row
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardsHeight), // Animated Explicit Height
                        horizontalArrangement = Arrangement.spacedBy(8.dp) 
                    ) {
                        // Result Card Content
                         // Download Box
                         Card(
                             modifier = Modifier.weight(1f).fillMaxHeight(),
                             colors = CardDefaults.cardColors(
                                 containerColor = if (testState is FastSpeedTestManager.TestState.Running && (testState as FastSpeedTestManager.TestState.Running).phase == FastSpeedTestManager.TestPhase.DOWNLOAD) 
                                     MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                             )
                         ) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth().fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Download", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val dlVal = when {
                                    testState is FastSpeedTestManager.TestState.Finished -> (testState as FastSpeedTestManager.TestState.Finished).downloadSpeed
                                    testState is FastSpeedTestManager.TestState.Running && (testState as FastSpeedTestManager.TestState.Running).phase == FastSpeedTestManager.TestPhase.UPLOAD -> metricData.downloadSpeed ?: 0.0
                                    testState is FastSpeedTestManager.TestState.Running && (testState as FastSpeedTestManager.TestState.Running).phase == FastSpeedTestManager.TestPhase.DOWNLOAD -> (testState as FastSpeedTestManager.TestState.Running).speedMbps
                                    else -> null
                                }
                                
                                val dlText = if (dlVal != null) "%.0f".format(dlVal) else "-"
                                
                                // Scaled Text
                                Text(
                                    text = dlText, 
                                    fontSize = (32 * fontScale).sp, 
                                    lineHeight = (40 * fontScale).sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Mbps", style = MaterialTheme.typography.labelSmall)
                                
                                if (metricData.downloadJitter != null || metricData.jitter != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.GraphicEq, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${metricData.downloadJitter ?: metricData.jitter} ms", 
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Upload Box
                        Card(
                             modifier = Modifier.weight(1f).fillMaxHeight(),
                             colors = CardDefaults.cardColors(
                                 containerColor = if (testState is FastSpeedTestManager.TestState.Running && (testState as FastSpeedTestManager.TestState.Running).phase == FastSpeedTestManager.TestPhase.UPLOAD) 
                                     MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                             )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth().fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Upload", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val ulVal = when {
                                    testState is FastSpeedTestManager.TestState.Finished -> (testState as FastSpeedTestManager.TestState.Finished).uploadSpeed
                                    testState is FastSpeedTestManager.TestState.Running && (testState as FastSpeedTestManager.TestState.Running).phase == FastSpeedTestManager.TestPhase.UPLOAD -> (testState as FastSpeedTestManager.TestState.Running).speedMbps
                                    else -> null
                                }
                                
                                val ulText = if (ulVal != null) "%.0f".format(ulVal) else "-"
                                 // Scaled Text
                                Text(
                                    text = ulText, 
                                    fontSize = (32 * fontScale).sp, 
                                    lineHeight = (40 * fontScale).sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Mbps", style = MaterialTheme.typography.labelSmall)
        
                                if (metricData.uploadJitter != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.GraphicEq, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${metricData.uploadJitter} ms", 
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } // End Result Row
    
                    AnimatedVisibility(
                        visible = !isFinished && !contentExpanded, // Strictly ensure invisible if expanded
                        enter = fadeIn(animationSpec = tween(1000)) + expandVertically(animationSpec = tween(1000)),
                        exit = fadeOut(animationSpec = tween(1200)) + shrinkVertically(animationSpec = tween(1200)) 
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                             // Gap between Cards and Gauge (Increased)
                             Spacer(modifier = Modifier.height(24.dp))
                        
                             val currentSpeed = when(testState) {
                                is FastSpeedTestManager.TestState.Running -> (testState as FastSpeedTestManager.TestState.Running).speedMbps
                                is FastSpeedTestManager.TestState.Finished -> (testState as FastSpeedTestManager.TestState.Finished).downloadSpeed 
                                else -> 0.0
                             }
                             
                             val isUpload = testState is FastSpeedTestManager.TestState.Running && 
                                           (testState as FastSpeedTestManager.TestState.Running).phase == FastSpeedTestManager.TestPhase.UPLOAD
                                           
                             val gaugeColor = if (isUpload) Color(0xFFFF9800) else Color(0xFF4CAF50)
                             
                             // Gauge Size 220dp (Reverted to original)
                             Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                                 com.smartwifi.ui.components.SpeedometerGauge(
                                     currentValue = currentSpeed,
                                     maxValue = 100.0,
                                     gaugeColor = gaugeColor,
                                     modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)
                                 )
                                 
                                 Column(
                                     horizontalAlignment = Alignment.CenterHorizontally,
                                     modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                                 ) {
                                     Text(
                                        text = "%.0f".format(currentSpeed),
                                        fontSize = 40.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                     )
                                     Text(
                                         text = "Mbps", 
                                         style = MaterialTheme.typography.titleMedium, 
                                         color = MaterialTheme.colorScheme.onSurfaceVariant
                                     )
                                 }
                             }
                             
                             // Bottom spacing below gauge (Tightened to 4dp)
                             Spacer(modifier = Modifier.height(4.dp))
                        }
                    } // End AnimatedVisibility

                // 4. Detailed Metrics Grid
                Spacer(modifier = Modifier.height(8.dp))
    
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Client & Server
                    // Row 1: Client & Server
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Client Info
                        MetricCell(
                            modifier = Modifier.weight(1f), 
                            label = "Client", 
                            icon = Icons.Rounded.Person
                        ) {
                            val location = metricData.userLocation
                            val ip = metricData.clientIp
                            val isp = metricData.clientIsp
                            
                            val isClientLoaded = !location.isNullOrEmpty() && location != "Unknown"
                            
                            // Stacked Top to Bottom
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (isClientLoaded) {
                                    Text(location!!, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) 
                                    if (!ip.isNullOrEmpty() && ip != "Unknown") {
                                        Text(ip, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                    if (!isp.isNullOrEmpty()) {
                                        Text(isp, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    // 3 Shimmer Placeholders
                                    ShimmerLine(width = 100.dp)
                                    ShimmerLine(width = 120.dp)
                                    ShimmerLine(width = 80.dp)
                                }
                            }
                        }
                        
                        // Server Info
                        MetricCell(
                            modifier = Modifier.weight(1f), 
                            label = "Server", 
                            icon = Icons.Rounded.Dns
                        ) {
                             val serverStr = metricData.serverName
                             val isServerLoaded = !serverStr.isNullOrEmpty() && serverStr != "Unknown Server"
                             
                             Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                 if (isServerLoaded) {
                                     // Split and render each server line (avoids duplication)
                                     serverStr!!.split("\n").distinct().forEach { serverLine ->
                                         Text(serverLine, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                     }
                                 } else {
                                     // 3 Shimmer Placeholders
                                     ShimmerLine(width = 90.dp)
                                     ShimmerLine(width = 110.dp)
                                     ShimmerLine(width = 70.dp)
                                 }
                             }
                        }
                    }
    
                    // Row 2: Internal IP & Packet Loss
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         MetricCell(
                            modifier = Modifier.weight(1f),
                            label = "Internal IP",
                            icon = Icons.Rounded.Router
                        ) {
                            Text(metricData.internalIp ?: "-", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        
                         MetricCell(
                             modifier = Modifier.weight(1f), 
                             label = "Packet Loss", 
                             icon = Icons.Rounded.Warning
                         ) {
                             Text(if (metricData.packetLoss != null) "%.1f%%".format(metricData.packetLoss) else "-", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                         }
                    }
                    
                    // Row 3: Pings (Idle Only)
                     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         MetricCell(
                             modifier = Modifier.weight(1f), 
                             label = "Idle Ping", 
                             icon = Icons.Rounded.HourglassEmpty
                         ) {
                             Text("${metricData.idlePing ?: "-"} ms", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                         }
                    }
                }
                

                
                // Spacer to ensure content doesn't get hidden behind bottom bar
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

// Shimmer Animation Component
@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 14.dp,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.extraSmall
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), // Primary Color
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
    )

    Spacer(
        modifier = modifier
            .size(width, height)
            .background(brush, shape)
    )
}

@Composable
fun MetricCell(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), MaterialTheme.shapes.small)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
             content()
             Spacer(modifier = Modifier.height(4.dp))
             Text(
                 text = label, 
                 style = MaterialTheme.typography.labelSmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                 maxLines = 1
             )
        }
    }
}
