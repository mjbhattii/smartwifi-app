package com.smartwifi.logic

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkDecisionBrain @Inject constructor() {

    // Map of BSSID to Timestamp (when probation ends)
    private val probationList = mutableMapOf<String, Long>()
    private val PROBATION_DURATION_MS = 2 * 60 * 1000L // 2 minutes

    fun shouldEnterProbation(rssi: Int, hasInternet: Boolean): Boolean {
        // Scenario B: Strong Signal (>-60) but No Internet -> Zombie
        return rssi > -60 && !hasInternet
    }

    fun addToProbation(bssid: String) {
        probationList[bssid] = System.currentTimeMillis() + PROBATION_DURATION_MS
    }

    fun isUnderProbation(bssid: String): Boolean {
        val endTime = probationList[bssid] ?: return false
        if (System.currentTimeMillis() > endTime) {
            probationList.remove(bssid) // Expired
            return false
        }
        return true
    }

    fun shouldTriggerDataFallback(rssi: Int, hasInternet: Boolean): Boolean {
        // Scenario C: Weak signal AND no internet -> Fallback
        return !hasInternet && rssi < -80
    }

    fun getProbationList(): Map<String, Long> {
        return probationList.toMap()
    }
}
