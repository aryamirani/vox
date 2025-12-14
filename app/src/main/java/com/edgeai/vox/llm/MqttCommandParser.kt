package com.edgeai.vox.llm

import com.edgeai.vox.mqtt.MqttCommandBatch
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class MqttCommandParser {

    private val gson = Gson()

    fun parse(llmOutput: String): Result<MqttCommandBatch> = runCatching {
        val json = extractJsonObject(llmOutput)
        gson.fromJson(json, MqttCommandBatch::class.java)
            ?: throw JsonSyntaxException("Empty LLM response")
    }

    /**
     * Phi-3 occasionally wraps JSON in markdown fences — strip those before parsing.
     */
    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()
        val fenceStart = trimmed.indexOf("```")
        if (fenceStart >= 0) {
            val afterFence = trimmed.substring(fenceStart + 3)
                .removePrefix("json")
                .trimStart()
            val fenceEnd = afterFence.indexOf("```")
            if (fenceEnd >= 0) return afterFence.substring(0, fenceEnd).trim()
        }
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        require(start >= 0 && end > start) { "No JSON object found in LLM output" }
        return trimmed.substring(start, end + 1)
    }
}
