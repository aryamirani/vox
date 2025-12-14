package com.edgeai.vox.snpe

import java.io.File

/**
 * Validates SNPE SDK and converted model artifacts before pipeline startup.
 */
object SnpeModelLoader {

    data class ValidationResult(
        val sdkPresent: Boolean,
        val whisperModelPresent: Boolean,
        val phi3ModelPresent: Boolean
    ) {
        val isReady: Boolean get() = sdkPresent && whisperModelPresent && phi3ModelPresent

        fun missingComponents(): List<String> = buildList {
            if (!sdkPresent) add("SNPE SDK (snpe-sdk/)")
            if (!whisperModelPresent) add("Whisper model (models/whisper/whisper_tiny.dlc)")
            if (!phi3ModelPresent) add("Phi-3 model (models/phi3/phi3_mini_4k_instruct.dlc)")
        }
    }

    fun validate(sdkRoot: File, whisperPath: File, phi3Path: File): ValidationResult {
        val sdkMarker = File(sdkRoot, "lib/aarch64-android/libSNPE.so")
        return ValidationResult(
            sdkPresent = sdkMarker.exists(),
            whisperModelPresent = whisperPath.exists(),
            phi3ModelPresent = phi3Path.exists()
        )
    }
}
