package com.smartwifi.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.drawscope.Stroke
import com.smartwifi.data.model.ConnectionSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSettingsClick: () -> Unit,
    onSpeedTestClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Animation for Radar
    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadarScale"
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Smart WiFi", 
                    modifier = Modifier.padding(16.dp), 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text(text = "Speed Test") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onSpeedTestClick()
                    },
                    icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                 NavigationDrawerItem(
                    label = { Text(text = "Settings") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onSettingsClick()
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Simple Top Bar for Menu
                CenterAlignedTopAppBar(
                    title = { Text("Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // --- Radar Visual (Principal Focus) ---
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(280.dp)
                ) {
                    // Outer Ripple
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = getRadarColor(uiState.internetStatus).copy(alpha = 0.2f),
                            radius = size.minDimension / 2 * radarScale,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                    
                    // Main Circle
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                            .border(8.dp, getRadarColor(uiState.internetStatus), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                             
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Dynamic Icon Logic
                            val connectionSource = uiState.connectionSource
                            when (connectionSource) {
                                ConnectionSource.MOBILE_DATA -> {
                                    Icon(
                                        imageVector = getMobileSignalIcon(uiState.signalStrength),
                                        contentDescription = "Mobile Data",
                                        modifier = Modifier.size(64.dp),
                                        tint = getRadarColor(uiState.internetStatus)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = getWifiSignalIcon(uiState.signalStrength),
                                        contentDescription = "WiFi",
                                        modifier = Modifier.size(64.dp),
                                        tint = getRadarColor(uiState.internetStatus)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = if (connectionSource == ConnectionSource.MOBILE_DATA) uiState.currentSsid else uiState.currentSsid.replace("\"", ""),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = uiState.internetStatus, 
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (uiState.internetStatus == "Connected") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // --- Triangle Status Bar (Restored) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusIcon(
                        icon = Icons.Rounded.Router,
                        label = "Router",
                        isActive = uiState.connectionSource == ConnectionSource.WIFI_ROUTER
                    )
                    StatusIcon(
                        icon = Icons.Rounded.WifiTethering,
                        label = "Hotspot",
                        isActive = uiState.connectionSource == ConnectionSource.WIFI_HOTSPOT
                    )
                    StatusIcon(
                        icon = Icons.Rounded.SignalCellularAlt,
                        label = "Mobile",
                        isActive = uiState.connectionSource == ConnectionSource.MOBILE_DATA
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
        
                // --- Network Speed (Link Speed / Usage) ---
                Card(
                   modifier = Modifier.fillMaxWidth(),
                   colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             Text("Link Speed", style = MaterialTheme.typography.labelMedium)
                             Text(
                                 text = "${uiState.linkSpeed} Mbps", 
                                 style = MaterialTheme.typography.titleLarge,
                                 fontWeight = FontWeight.Bold,
                                 color = MaterialTheme.colorScheme.primary
                             )
                         }
                         
                         Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.2f)))
        
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             Text("Current Usage", style = MaterialTheme.typography.labelMedium)
                             Text(
                                 text = uiState.currentUsage, 
                                 style = MaterialTheme.typography.titleLarge,
                                 fontWeight = FontWeight.Bold,
                                 color = MaterialTheme.colorScheme.secondary
                             )
                         }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // --- Quick Action Cards (Restored) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Gamepad,
                        label = "Gaming Mode",
                        description = "Pause scanning",
                        isActive = uiState.isGamingMode,
                        onClick = { viewModel.toggleGamingMode() }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.SignalCellular4Bar,
                        label = "Data Fallback",
                        description = "Mobile backup",
                        isActive = uiState.isDataFallback,
                        onClick = { viewModel.toggleDataFallback() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedButton(onClick = onSettingsClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Advanced Settings")
                }
    }
    // --- Switch Confirmation Dialog Removed (Auto-Switch enabled) ---
    // if (uiState.pendingSwitchNetwork != null) { ... }
        }
    }
}

@Composable
fun StatusIcon(icon: ImageVector, label: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier, 
    icon: ImageVector, 
    label: String, 
    description: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
             Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isActive) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusCard(
    title: String, 
    value: String, 
    icon: ImageVector, 
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}



fun getRadarColor(status: String): Color {
    return when (status) {
        "Connected" -> Color(0xFF4CAF50) // Green
        "No Internet" -> Color(0xFFF44336) // Red
        else -> Color(0xFFFFC107) // Yellow
    }
}

fun getWifiSignalIcon(rssi: Int): ImageVector {
    val level = android.net.wifi.WifiManager.calculateSignalLevel(rssi, 5)
    return when (level) {
        4 -> Icons.Default.SignalWifi4Bar
        3 -> Icons.Default.SignalWifi4Bar // Fallback: 3Bar missing
        2 -> Icons.Default.SignalWifi0Bar // Fallback: 2Bar missing
        1 -> Icons.Default.SignalWifi0Bar // Fallback: 1Bar missing
        else -> Icons.Default.SignalWifi0Bar
    }
}

fun getMobileSignalIcon(dbm: Int): ImageVector {
    // Approximate mapping with available icons
    return when {
         dbm > -90 -> Icons.Default.SignalCellular4Bar
         dbm > -105 -> Icons.Default.SignalCellular4Bar // Fallback
         else -> Icons.Default.SignalCellular0Bar // Fallback
    }
}
