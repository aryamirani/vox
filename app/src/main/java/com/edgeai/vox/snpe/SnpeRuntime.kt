package com.edgeai.vox.snpe

/**
 * Opaque handle returned when a .dlc model is loaded into the SNPE runtime.
 */
data class SnpeModelHandle(val id: Long, val modelPath: String)

/**
 * Kotlin façade over the Qualcomm SNPE native runtime.
 *
 * JNI implementation lives in [com.edgeai.vox.snpe.SnpeNative].
 * Link against libraries placed in the project-root `snpe-sdk/` directory.
 */
class SnpeRuntime(private val context: android.content.Context) {

    private var initialized = false

    fun initialize(): Result<Unit> = runCatching {
        if (initialized) return@runCatching
        SnpeNative.init(context.applicationInfo.nativeLibraryDir)
        initialized = true
    }

    fun loadModel(modelPath: String, runtime: String): Result<SnpeModelHandle> = runCatching {
        check(initialized) { "SnpeRuntime not initialized" }
        val handleId = SnpeNative.loadModel(modelPath, runtime)
        SnpeModelHandle(handleId, modelPath)
    }

    fun unloadModel(handle: SnpeModelHandle) {
        if (!initialized) return
        SnpeNative.unloadModel(handle.id)
    }

    fun runWhisperInference(
        handle: SnpeModelHandle,
        audioSamples: FloatArray,
        sampleRateHz: Int
    ): Result<String> = runCatching {
        SnpeNative.runWhisper(handle.id, audioSamples, sampleRateHz)
    }

    fun runLlmInference(
        handle: SnpeModelHandle,
        prompt: String,
        maxNewTokens: Int
    ): Result<String> = runCatching {
        SnpeNative.runLlm(handle.id, prompt, maxNewTokens)
    }

    fun release() {
        if (!initialized) return
        SnpeNative.release()
        initialized = false
    }
}
