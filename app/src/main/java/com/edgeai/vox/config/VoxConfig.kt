package com.edgeai.vox.config

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Central configuration for model paths, SNPE runtime options, and MQTT defaults.
 */
class VoxConfig(context: Context) {

    /** On-device location for .dlc files (see scripts/deploy_models.sh). */
    val deviceModelsRoot: File = File(
        Environment.getExternalStorageDirectory(),
        "vox/models"
    )

    val whisperModelPath: File = File(deviceModelsRoot, "whisper/whisper_tiny.dlc")
    val phi3ModelPath: File = File(deviceModelsRoot, "phi3/phi3_mini_4k_instruct.dlc")

    /** SNPE SDK location on device (extract SDK here for native linking during dev). */
    val snpeSdkRoot: File = File(deviceModelsRoot.parentFile, "snpe-sdk")

    val defaultBrokerUri: String = "tcp://192.168.1.100:1883"
    val defaultMqttClientIdPrefix: String = "vox-android"

    val sampleRateHz: Int = 16_000
    val audioChannelConfig: Int = android.media.AudioFormat.CHANNEL_IN_MONO
    val audioEncoding: Int = android.media.AudioFormat.ENCODING_PCM_16BIT
    val maxRecordingDurationMs: Long = 8_000

    val whisperRuntime: SnpeRuntimeProfile = SnpeRuntimeProfile.GPU
    val phi3Runtime: SnpeRuntimeProfile = SnpeRuntimeProfile.DSP

    enum class SnpeRuntimeProfile {
        CPU, GPU, DSP
    }
}
