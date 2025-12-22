package com.smartwifi.logic

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class FastSpeedTestManager @Inject constructor() {

    private val _testState = MutableStateFlow<TestState>(TestState.Idle)
    val testState = _testState.asStateFlow()

    private val token = "YXNkZmFzZGxmbnNkYWZoYXNkZmhrYWxm"
    private val apiUrl = "https://api.fast.com/netflix/speedtest/v2?https=true&token=$token&urlCount=3"

    sealed class TestState {
        object Idle : TestState()
        object Preparing : TestState()
        data class Running(val speedMbps: Double, val progress: Float) : TestState() // progress 0.0 - 1.0
        data class Finished(val finalSpeedMbps: Double) : TestState()
        data class Error(val message: String) : TestState()
    }

    suspend fun startSpeedTest() {
        _testState.value = TestState.Preparing
        try {
            val targets = fetchTargets()
            if (targets.isEmpty()) {
                _testState.value = TestState.Error("No servers found")
                return
            }
            
            runDownloadTest(targets)
        } catch (e: Exception) {
            Log.e("FastSpeedTest", "Error", e)
            _testState.value = TestState.Error(e.message ?: "Unknown Error")
        }
    }

    private suspend fun fetchTargets(): List<String> = withContext(Dispatchers.IO) {
        val url = URL(apiUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000

        try {
            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonStr)
            val targetsArray = jsonObject.getJSONArray("targets")
            val list = mutableListOf<String>()
            for (i in 0 until targetsArray.length()) {
                list.add(targetsArray.getJSONObject(i).getString("url"))
            }
            list
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun runDownloadTest(urls: List<String>) = withContext(Dispatchers.IO) {
        // Parallel Download Logic Simplified
        // We will run downloads for ~10 seconds
        val startTime = System.currentTimeMillis()
        val endTime = startTime + 10_000 // 10s test
        
        var totalBytes = 0L
        val updateInterval = 200L
        var lastUpdate = startTime
        
        // Use the first valid URL for simplicity or round-robin
        // In a real robust implementation, we'd use multiple threads. 
        // Here we loop downloading chunks from the first URL to simulate load.
        val targetUrl = urls.first()
        
        try {
            val url = URL(targetUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val stream: InputStream = conn.inputStream
            val buffer = ByteArray(8192)
            
            while (System.currentTimeMillis() < endTime) {
                // Check if EOF, if so, reconnect (unlikely for Fast.com stream, but safety)
                val read = stream.read(buffer)
                if (read == -1) break 
                
                totalBytes += read
                
                val now = System.currentTimeMillis()
                if (now - lastUpdate > updateInterval) {
                    val durationSeconds = (now - startTime) / 1000.0
                    val bits = totalBytes * 8.0
                    val mbps = (bits / durationSeconds) / 1_000_000.0
                    val progress = (now - startTime) / 10_000f
                    
                    _testState.value = TestState.Running(mbps, progress.coerceIn(0f, 1f))
                    lastUpdate = now
                }
            }
            stream.close()
            conn.disconnect()
            
            // Final calc
            val totalDuration = (System.currentTimeMillis() - startTime) / 1000.0
            val finalMbps = ((totalBytes * 8.0) / totalDuration) / 1_000_000.0
            _testState.value = TestState.Finished(finalMbps)
            
        } catch (e: Exception) {
             _testState.value = TestState.Error("Download failed: ${e.message}")
        }
    }
    
    fun reset() {
        _testState.value = TestState.Idle
    }
}
