package com.edgeai.vox.audio

import kotlin.math.max
import kotlin.math.min

object AudioProcessor {

    /** Normalize int16 PCM to float32 in [-1, 1] for Whisper preprocessing. */
    fun pcmToFloat32(samples: ShortArray): FloatArray =
        FloatArray(samples.size) { samples[it] / 32768.0f }

    /** Simple peak normalization — applied before SNPE inference. */
    fun normalizeInPlace(samples: FloatArray) {
        var peak = 0f
        for (sample in samples) {
            peak = max(peak, abs(sample))
        }
        if (peak <= 1e-6f) return
        val gain = min(1f / peak, 4f)
        for (i in samples.indices) {
            samples[i] *= gain
        }
    }

    private fun abs(value: Float): Float = if (value < 0) -value else value
}
