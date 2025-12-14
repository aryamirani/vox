package com.edgeai.vox.mqtt

data class MqttCommand(
    val topic: String,
    val payload: String
)

data class MqttCommandBatch(
    val commands: List<MqttCommand>
)
