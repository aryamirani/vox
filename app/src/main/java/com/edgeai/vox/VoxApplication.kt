package com.edgeai.vox

import android.app.Application
import com.edgeai.vox.config.VoxConfig
import com.edgeai.vox.llm.Phi3Engine
import com.edgeai.vox.mqtt.MqttManager
import com.edgeai.vox.pipeline.VoicePipeline
import com.edgeai.vox.snpe.SnpeRuntime
import com.edgeai.vox.stt.WhisperEngine

class VoxApplication : Application() {

    lateinit var config: VoxConfig
        private set

    lateinit var snpeRuntime: SnpeRuntime
        private set

    lateinit var whisperEngine: WhisperEngine
        private set

    lateinit var phi3Engine: Phi3Engine
        private set

    lateinit var mqttManager: MqttManager
        private set

    lateinit var voicePipeline: VoicePipeline
        private set

    override fun onCreate() {
        super.onCreate()
        config = VoxConfig(this)
        snpeRuntime = SnpeRuntime(this)
        whisperEngine = WhisperEngine(snpeRuntime, config)
        phi3Engine = Phi3Engine(snpeRuntime, config)
        mqttManager = MqttManager()
        voicePipeline = VoicePipeline(
            whisperEngine = whisperEngine,
            phi3Engine = phi3Engine,
            mqttManager = mqttManager,
            config = config
        )
    }
}
