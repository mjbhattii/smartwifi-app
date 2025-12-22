package com.smartwifi.logic

import android.content.Context
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SignalMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _currentSignalLevel = MutableStateFlow(0)
    val currentSignalLevel = _currentSignalLevel.asStateFlow()

    fun getRssi(): Int {
        val info = wifiManager.connectionInfo
        return info.rssi
    }

    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    fun getFrequency(): Int {
        val info = wifiManager.connectionInfo
        return info.frequency
    }

    fun getLinkSpeed(): Int {
        val info = wifiManager.connectionInfo
        return info.linkSpeed
    }

    fun getTotalRxBytes(): Long {
        return android.net.TrafficStats.getTotalRxBytes()
    }

    fun getTotalTxBytes(): Long {
        return android.net.TrafficStats.getTotalTxBytes()
    }

    // Logic for "Significant Difference" (20% better)
    fun isSignificantlyBetter(newRssi: Int, currentRssi: Int, threshold: Int = 10): Boolean {
        return newRssi > (currentRssi + threshold)
    }
}
