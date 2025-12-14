package com.edgeai.vox.pipeline

import com.edgeai.vox.audio.AudioRecorder
import com.edgeai.vox.config.VoxConfig
import com.edgeai.vox.llm.LlmEngine
import com.edgeai.vox.llm.MqttCommandParser
import com.edgeai.vox.mqtt.MqttManager
import com.edgeai.vox.snpe.SnpeModelLoader
import com.edgeai.vox.stt.SpeechToTextEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * End-to-end zero-cloud pipeline:
 * Voice → Whisper (SNPE) → Phi-3 (SNPE) → MQTT publish
 */
class VoicePipeline(
    private val whisperEngine: SpeechToTextEngine,
    private val phi3Engine: LlmEngine,
    private val mqttManager: MqttManager,
    private val config: VoxConfig,
    private val audioRecorder: AudioRecorder = AudioRecorder(config),
    private val commandParser: MqttCommandParser = MqttCommandParser()
) {

    enum class Stage {
        IDLE,
        LISTENING,
        TRANSCRIBING,
        INTERPRETING,
        PUBLISHING,
        ERROR
    }

    data class PipelineResult(
        val transcript: String,
        val llmOutput: String,
        val commandsPublished: Int
    )

    private val _stage = MutableStateFlow(Stage.IDLE)
    val stage: StateFlow<Stage> = _stage.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val logLines = mutableListOf<String>()
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    suspend fun initializeEngines(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val validation = SnpeModelLoader.validate(
                sdkRoot = config.snpeSdkRoot,
                whisperPath = config.whisperModelPath,
                phi3Path = config.phi3ModelPath
            )
            if (!validation.isReady) {
                val missing = validation.missingComponents().joinToString(", ")
                appendLog("Missing on-device assets: $missing")
                appendLog("Pipeline will use demo stubs until models/SDK are installed.")
            }
            whisperEngine.initialize().getOrThrow()
            phi3Engine.initialize().getOrThrow()
            appendLog("STT and LLM engines initialized")
        }
    }

    suspend fun processVoiceCommand(): Result<PipelineResult> = withContext(Dispatchers.IO) {
        runCatching {
            _lastError.value = null

            _stage.value = Stage.LISTENING
            appendLog("Recording audio …")
            val pcm = audioRecorder.recordUntilSilenceOrTimeout()
            appendLog("Captured ${pcm.size} samples @ ${config.sampleRateHz} Hz")

            _stage.value = Stage.TRANSCRIBING
            appendLog("Running Whisper inference …")
            val transcript = whisperEngine.transcribe(pcm).getOrThrow()
            appendLog("Transcript: $transcript")

            _stage.value = Stage.INTERPRETING
            appendLog("Running Phi-3 inference …")
            val llmOutput = phi3Engine.generateCommand(transcript).getOrThrow()
            appendLog("LLM output: $llmOutput")

            _stage.value = Stage.PUBLISHING
            val batch = commandParser.parse(llmOutput).getOrThrow()
            appendLog("Parsed ${batch.commands.size} MQTT command(s)")

            val published = mqttManager.publishBatch(batch).getOrThrow()
            appendLog("Published $published command(s)")

            _stage.value = Stage.IDLE
            PipelineResult(
                transcript = transcript,
                llmOutput = llmOutput,
                commandsPublished = published
            )
        }.onFailure { error ->
            _stage.value = Stage.ERROR
            _lastError.value = error.message
            appendLog("Pipeline error: ${error.message}")
        }
    }

    fun release() {
        whisperEngine.release()
        phi3Engine.release()
        audioRecorder.stopRecording()
    }

    private fun appendLog(line: String) {
        synchronized(logLines) {
            logLines.add("[${System.currentTimeMillis() % 100_000}] $line")
            if (logLines.size > 200) logLines.removeAt(0)
            _logs.value = logLines.toList()
        }
    }
}
