package com.edgeai.vox.llm

/**
 * On-device LLM for interpreting voice commands and emitting structured MQTT payloads.
 */
interface LlmEngine {
    suspend fun initialize(): Result<Unit>
    suspend fun generateCommand(transcript: String): Result<String>
    fun release()
}
