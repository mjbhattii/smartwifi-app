package com.smartwifi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartwifi.logic.FastSpeedTestManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    val testManager: FastSpeedTestManager
) : ViewModel() {

    fun startTest() {
        viewModelScope.launch {
            testManager.startSpeedTest()
        }
    }
    
    fun reset() {
        testManager.reset()
    }
}
