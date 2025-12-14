package com.edgeai.vox.stt

/**
 * Local speech-to-text engine backed by OpenAI Whisper Tiny converted for SNPE.
 */
interface SpeechToTextEngine {
    suspend fun initialize(): Result<Unit>
    suspend fun transcribe(pcmSamples: ShortArray): Result<String>
    fun release()
}
