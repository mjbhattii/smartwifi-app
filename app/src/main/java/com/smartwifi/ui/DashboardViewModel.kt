package com.smartwifi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartwifi.data.SmartWifiRepository
import com.smartwifi.data.model.AppUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: SmartWifiRepository
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = repository.uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppUiState()
        )

    fun setSensitivity(value: Int) {
        repository.setSensitivity(value)
    }

    fun setMinSignalDiff(value: Int) {
        repository.setMinSignalDiff(value)
    }

    fun setMobileDataThreshold(value: Int) {
        repository.setMobileDataThreshold(value)
    }

    fun setGeofencing(enabled: Boolean) {
        repository.setGeofencing(enabled)
    }

    fun toggleService(enabled: Boolean) {
        repository.updateServiceStatus(enabled)
    }

    fun toggleGamingMode() {
        // Toggle current state
        val current = uiState.value.isGamingMode
        repository.setGamingMode(!current)
    }

    fun toggleDataFallback() {
        val current = uiState.value.isDataFallback
        repository.setDataFallback(!current)
    }

    fun setHotspotSwitchingEnabled(enabled: Boolean) {
        repository.setHotspotSwitchingEnabled(enabled)
    }

    fun set5GhzPriorityEnabled(enabled: Boolean) {
        repository.set5GhzPriorityEnabled(enabled)
    }
}
