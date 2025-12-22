package com.smartwifi.logic

import android.content.Context
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class MobileNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager
    
    // Cache for latest signal strength (dBm)
    private var cachedSignalDbm: Int = -100
    
    // Cache for latest network type override (e.g., 4G+)
    private var cachedDisplayOverride: Int = 0

    init {
        registerListeners()
    }

    private fun registerListeners() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // API 31+
                telephonyManager.registerTelephonyCallback(
                    context.mainExecutor,
                    object : android.telephony.TelephonyCallback(), 
                             android.telephony.TelephonyCallback.SignalStrengthsListener,
                             android.telephony.TelephonyCallback.DisplayInfoListener {
                        
                        override fun onSignalStrengthsChanged(signalStrength: android.telephony.SignalStrength) {
                            cachedSignalDbm = getDbmFromSignalStrength(signalStrength)
                        }

                        override fun onDisplayInfoChanged(telephonyDisplayInfo: android.telephony.TelephonyDisplayInfo) {
                            cachedDisplayOverride = telephonyDisplayInfo.overrideNetworkType
                        }
                    }
                )
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // API 30 (Android 11)
                @Suppress("DEPRECATION")
                telephonyManager.listen(object : android.telephony.PhoneStateListener() {
                    override fun onSignalStrengthsChanged(signalStrength: android.telephony.SignalStrength) {
                        super.onSignalStrengthsChanged(signalStrength)
                        cachedSignalDbm = getDbmFromSignalStrength(signalStrength)
                    }
                    
                    override fun onDisplayInfoChanged(telephonyDisplayInfo: android.telephony.TelephonyDisplayInfo) {
                        super.onDisplayInfoChanged(telephonyDisplayInfo)
                        cachedDisplayOverride = telephonyDisplayInfo.overrideNetworkType
                    }
                }, android.telephony.PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or android.telephony.PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED)
            } else {
                // API < 30: Legacy
                @Suppress("DEPRECATION")
                telephonyManager.listen(object : android.telephony.PhoneStateListener() {
                     override fun onSignalStrengthsChanged(signalStrength: android.telephony.SignalStrength) {
                        super.onSignalStrengthsChanged(signalStrength)
                        cachedSignalDbm = getDbmFromSignalStrength(signalStrength)
                    }
                }, android.telephony.PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
            }
        } catch (e: SecurityException) {
            Log.e("MobileNetworkMonitor", "Permission failed for listener", e)
        } catch (e: Exception) {
            Log.e("MobileNetworkMonitor", "Error registering listener", e)
        }
    }

    private fun getDbmFromSignalStrength(signalStrength: android.telephony.SignalStrength): Int {
        // Simple logic: Try to get LTE, then NR (5G), then others.
        // In a real app we'd check the active data network first.
        // For brevity, we try to grab the first valid readable level.
        
        // Reflection or string parsing is sometimes needed for old APIs, 
        // but let's stick to standard CellSignalStrength if possible (API 29+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
             signalStrength.cellSignalStrengths.forEach { 
                 val dbm = it.dbm
                 if (dbm < 0 && dbm > -140) return dbm // valid range
             }
        }
        // Fallback or legacy handling could go here. 
        // If we can't extract, map level 0-4 to dBm approximation
        val level = signalStrength.level // 0..4
        return when (level) {
            4 -> -65
            3 -> -85
            2 -> -100
            1 -> -115
            else -> -120
        }
    }

    fun getCarrierName(): String {
        return try {
            // ... (Existing Dual SIM logic logic matches here or needs to be retained) ...
            // 1. Try to get Active Data Subscription ID
            val activeSubId = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.telephony.SubscriptionManager.getDefaultDataSubscriptionId()
            } else {
                -1
            }

            // 2. If valid ID, try to find matching SubscriptionInfo
            if (activeSubId != -1) {
                val activeInfo = subscriptionManager.activeSubscriptionInfoList?.find { it.subscriptionId == activeSubId }
                if (activeInfo != null) {
                    var carrier = activeInfo.carrierName?.toString()
                    if (carrier.isNullOrEmpty()) carrier = activeInfo.displayName?.toString()
                    if (!carrier.isNullOrEmpty()) return carrier
                }
            }

            val name = telephonyManager.simOperatorName
            if (name.isNullOrEmpty()) {
                telephonyManager.networkOperatorName ?: "Mobile Data"
            } else {
                name
            }
        } catch (e: Exception) {
            "Mobile Network"
        }
    }

    fun getNetworkType(): String {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                 val networkType = telephonyManager.dataNetworkType
                 
                 // Check for 4.5G / 5G+ overrides first
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                     when (cachedDisplayOverride) {
                         android.telephony.TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO,
                         android.telephony.TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> return "4.5G"
                         android.telephony.TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
                         android.telephony.TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE -> return "5G" // 5G NSA
                     }
                 }

                 when (networkType) {
                     TelephonyManager.NETWORK_TYPE_NR -> "5G"
                     TelephonyManager.NETWORK_TYPE_LTE -> "4G" 
                     TelephonyManager.NETWORK_TYPE_HSPAP, 
                     TelephonyManager.NETWORK_TYPE_HSPA,
                     TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                     TelephonyManager.NETWORK_TYPE_EDGE,
                     TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                     else -> "Mobile Data"
                 }
            } else {
                "Mobile Data"
            }
        } catch (e: SecurityException) {
            "Mobile"
        } catch (e: Exception) {
            "Mobile"
        }
    }
    
    fun getSignalStrength(): Int {
        return cachedSignalDbm
    }
}
