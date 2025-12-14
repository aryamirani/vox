package com.edgeai.vox.stt

import com.edgeai.vox.audio.AudioProcessor
import com.edgeai.vox.config.VoxConfig
import com.edgeai.vox.snpe.SnpeModelHandle
import com.edgeai.vox.snpe.SnpeRuntime

/**
 * Whisper Tiny ASR running on Qualcomm SNPE.
 *
 * Production flow:
 * 1. Load whisper_tiny.dlc from [VoxConfig.whisperModelPath]
 * 2. Preprocess 16 kHz PCM → mel spectrogram (native)
 * 3. Run SNPE inference on Hexagon DSP / GPU
 * 4. Decode token IDs → text (native)
 */
class WhisperEngine(
    private val snpeRuntime: SnpeRuntime,
    private val config: VoxConfig
) : SpeechToTextEngine {

    private var modelHandle: SnpeModelHandle? = null

    override suspend fun initialize(): Result<Unit> = runCatching {
        snpeRuntime.initialize()
        modelHandle = snpeRuntime.loadModel(
            modelPath = config.whisperModelPath.absolutePath,
            runtime = config.whisperRuntime.name
        ).getOrThrow()
    }

    override suspend fun transcribe(pcmSamples: ShortArray): Result<String> = runCatching {
        val handle = modelHandle ?: error("WhisperEngine not initialized")
        val floatSamples = AudioProcessor.pcmToFloat32(pcmSamples)
        AudioProcessor.normalizeInPlace(floatSamples)

        snpeRuntime.runWhisperInference(
            handle = handle,
            audioSamples = floatSamples,
            sampleRateHz = config.sampleRateHz
        ).getOrThrow()
    }

    override fun release() {
        modelHandle?.let { snpeRuntime.unloadModel(it) }
        modelHandle = null
    }
}
