package com.edgeai.vox.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgeai.vox.VoxApplication
import com.edgeai.vox.mqtt.MqttManager
import com.edgeai.vox.pipeline.VoicePipeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VoxApplication
    private val pipeline = app.voicePipeline
    private val mqttManager = app.mqttManager

    val defaultBrokerUri: String = app.config.defaultBrokerUri

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _llmOutput = MutableStateFlow("")
    val llmOutput: StateFlow<String> = _llmOutput.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    val pipelineStage: StateFlow<VoicePipeline.Stage> = pipeline.stage

    val lastError: StateFlow<String?> = pipeline.lastError

    val mqttConnected: StateFlow<Boolean> = mqttManager.connectionState
        .map { it == MqttManager.ConnectionState.CONNECTED }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val combinedLogs: StateFlow<List<String>> = combine(
        pipeline.logs,
        mqttManager.logs
    ) { pipelineLogs, mqttLogs ->
        (pipelineLogs + mqttLogs).sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun initializeEngines() {
        viewModelScope.launch {
            pipeline.initializeEngines()
        }
    }

    fun connectMqtt(brokerUri: String, clientId: String?) {
        viewModelScope.launch {
            mqttManager.connect(brokerUri, clientId)
        }
    }

    fun disconnectMqtt() {
        mqttManager.disconnect()
    }

    suspend fun runVoicePipeline() {
        if (_isProcessing.value) return
        _isProcessing.value = true
        try {
            pipeline.processVoiceCommand()
                .onSuccess { result ->
                    _transcript.value = result.transcript
                    _llmOutput.value = result.llmOutput
                }
        } finally {
            _isProcessing.value = false
        }
    }

    override fun onCleared() {
        pipeline.release()
        mqttManager.disconnect()
        super.onCleared()
    }
}
