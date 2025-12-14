package com.edgeai.vox.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.edgeai.vox.config.VoxConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class AudioRecorder(private val config: VoxConfig) {

    private var audioRecord: AudioRecord? = null

    val bufferSize: Int = AudioRecord.getMinBufferSize(
        config.sampleRateHz,
        config.audioChannelConfig,
        config.audioEncoding
    ).coerceAtLeast(config.sampleRateHz)

    fun startRecording(): AudioRecord {
        stopRecording()
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            config.sampleRateHz,
            config.audioChannelConfig,
            config.audioEncoding,
            bufferSize * 2
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("AudioRecord failed to initialize")
        }
        record.startRecording()
        audioRecord = record
        return record
    }

    suspend fun recordUntilSilenceOrTimeout(
        silenceThreshold: Int = 800,
        silenceDurationMs: Long = 1_200,
        maxDurationMs: Long = config.maxRecordingDurationMs
    ): ShortArray = withContext(Dispatchers.IO) {
        val record = startRecording()
        val pcmStream = ByteArrayOutputStream()
        val startTime = System.currentTimeMillis()
        var lastVoiceTime = startTime
        val buffer = ShortArray(bufferSize)

        try {
            while (isActive) {
                val read = record.read(buffer, 0, buffer.size)
                if (read <= 0) continue

                for (i in 0 until read) {
                    pcmStream.write(buffer[i].toInt() and 0xFF)
                    pcmStream.write((buffer[i].toInt() shr 8) and 0xFF)
                }

                val peak = buffer.take(read).maxOf { abs(it.toInt()) }
                val now = System.currentTimeMillis()
                if (peak > silenceThreshold) {
                    lastVoiceTime = now
                }
                if (now - startTime >= maxDurationMs) break
                if (now - lastVoiceTime >= silenceDurationMs && now - startTime > 500) break
            }
        } finally {
            stopRecording()
        }

        val bytes = pcmStream.toByteArray()
        ShortArray(bytes.size / 2) { i ->
            ((bytes[i * 2 + 1].toInt() shl 8) or (bytes[i * 2].toInt() and 0xFF)).toShort()
        }
    }

    fun stopRecording() {
        audioRecord?.let { record ->
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
            record.release()
        }
        audioRecord = null
    }
}
