package com.edgeai.vox.llm

import com.edgeai.vox.config.VoxConfig
import com.edgeai.vox.snpe.SnpeModelHandle
import com.edgeai.vox.snpe.SnpeRuntime

/**
 * Microsoft Phi-3-mini-4k-instruct running on Qualcomm SNPE.
 *
 * Converts natural-language transcripts into JSON MQTT command batches.
 * Token generation uses greedy decoding with a 256-token cap for low latency.
 */
class Phi3Engine(
    private val snpeRuntime: SnpeRuntime,
    private val config: VoxConfig
) : LlmEngine {

    companion object {
        const val MAX_NEW_TOKENS = 256
    }

    private var modelHandle: SnpeModelHandle? = null

    override suspend fun initialize(): Result<Unit> = runCatching {
        snpeRuntime.initialize()
        modelHandle = snpeRuntime.loadModel(
            modelPath = config.phi3ModelPath.absolutePath,
            runtime = config.phi3Runtime.name
        ).getOrThrow()
    }

    override suspend fun generateCommand(transcript: String): Result<String> = runCatching {
        val handle = modelHandle ?: error("Phi3Engine not initialized")
        val prompt = PromptBuilder.buildPrompt(transcript)

        snpeRuntime.runLlmInference(
            handle = handle,
            prompt = prompt,
            maxNewTokens = MAX_NEW_TOKENS
        ).getOrThrow()
    }

    override fun release() {
        modelHandle?.let { snpeRuntime.unloadModel(it) }
        modelHandle = null
    }
}
