package com.edgeai.vox.snpe

internal object SnpeNative {

    init {
        System.loadLibrary("vox_snpe")
    }

    external fun init(nativeLibDir: String)
    external fun loadModel(modelPath: String, runtime: String): Long
    external fun unloadModel(handleId: Long)
    external fun runWhisper(handleId: Long, audioSamples: FloatArray, sampleRateHz: Int): String
    external fun runLlm(handleId: Long, prompt: String, maxNewTokens: Int): String
    external fun release()
}
