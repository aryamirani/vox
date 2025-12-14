package com.edgeai.vox.mqtt

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.Charset
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MqttManager {

    private var client: MqttClient? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val logLines = mutableListOf<String>()
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED
    }

    suspend fun connect(brokerUri: String, clientId: String? = null): Result<Unit> = runCatching {
        val normalizedUri = normalizeBrokerUri(brokerUri)
        val id = clientId?.takeIf { it.isNotBlank() } ?: generateClientId()

        suspendCoroutine { cont ->
            _connectionState.value = ConnectionState.CONNECTING
            appendLog("Connecting to $normalizedUri as $id …")

            Thread {
                try {
                    disconnectInternal()
                    val mqttClient = MqttClient(normalizedUri, id, MemoryPersistence())
                    val options = MqttConnectOptions().apply {
                        isAutomaticReconnect = true
                        isCleanSession = true
                        connectionTimeout = 10
                        keepAliveInterval = 60
                    }
                    mqttClient.setCallback(object : MqttCallback {
                        override fun connectionLost(cause: Throwable?) {
                            appendLog("Connection lost: ${cause?.message ?: "unknown"}")
                            _connectionState.value = ConnectionState.DISCONNECTED
                        }

                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            val payload = message?.payload?.toString(Charset.defaultCharset()) ?: ""
                            appendLog("<- $topic : $payload")
                        }

                        override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                    })
                    mqttClient.connect(options)
                    client = mqttClient
                    mqttClient.subscribe("home/#", 1)
                    appendLog("Subscribed to home/#")
                    _connectionState.value = ConnectionState.CONNECTED
                    cont.resume(Unit)
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    appendLog("Connect failed: ${e.message}")
                    cont.resumeWithException(e)
                }
            }.start()
        }
    }

    fun publish(topic: String, payload: String, qos: Int = 1, retained: Boolean = false): Result<Unit> =
        runCatching {
            val mqttClient = client
            if (mqttClient == null || !mqttClient.isConnected) {
                error("MQTT client not connected")
            }
            val message = MqttMessage(payload.toByteArray(Charset.defaultCharset())).apply {
                this.qos = qos
                isRetained = retained
            }
            mqttClient.publish(topic, message)
            appendLog("-> $topic : $payload")
        }

    fun publishBatch(batch: MqttCommandBatch): Result<Int> = runCatching {
        var published = 0
        for (command in batch.commands) {
            publish(command.topic, command.payload).getOrThrow()
            published++
        }
        published
    }

    fun disconnect() {
        disconnectInternal()
        _connectionState.value = ConnectionState.DISCONNECTED
        appendLog("Disconnected")
    }

    private fun disconnectInternal() {
        try {
            client?.disconnectForcibly(500)
            client?.close()
        } catch (_: Exception) {
        } finally {
            client = null
        }
    }

    private fun normalizeBrokerUri(uri: String): String {
        val trimmed = uri.trim()
        return when {
            trimmed.startsWith("tcp://", ignoreCase = true) -> trimmed
            trimmed.startsWith("mqtt://", ignoreCase = true) ->
                "tcp://${trimmed.removePrefix("mqtt://")}"
            trimmed.startsWith("ssl://", ignoreCase = true) -> trimmed
            else -> "tcp://$trimmed"
        }
    }

    private fun generateClientId(): String =
        "vox-" + UUID.randomUUID().toString().replace("-", "").take(18)

    private fun appendLog(line: String) {
        synchronized(logLines) {
            logLines.add("[${System.currentTimeMillis() % 100_000}] $line")
            if (logLines.size > 200) logLines.removeAt(0)
            _logs.value = logLines.toList()
        }
    }
}
