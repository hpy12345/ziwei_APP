package com.ziweistar.app.engine.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 跨模型受控文学渲染器（Controlled LLM Literary Renderer）
 *
 * 使用 OpenAI 兼容的 /chat/completions 协议，可对接：
 *  OpenAI / DeepSeek / Moonshot / 通义 / 本地 llama.cpp server 等。
 *
 * 采样参数严格锁定：temperature = 0.0, top_p = 1.0, seed = YYYYMMDD。
 */
class LlmRenderer(
    private val endpoint: String,
    private val apiKey: String,
    private val model: String,
    private val timeoutSeconds: Long = 20,
) {
    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    /** 是否已正确配置（未配置则永远走本地兜底） */
    val isConfigured: Boolean
        get() = endpoint.isNotBlank() && model.isNotBlank()

    /**
     * 调用 chat/completions，返回文本内容。
     * 任何失败（网络/超时/格式）都抛异常，由上层降级到本地古籍模板库。
     */
    fun complete(systemPrompt: String, userPrompt: String, seed: Int): String {
        require(isConfigured) { "LLM 未配置" }

        val body = buildJsonObject {
            put("model", model)
            put("temperature", 0.0)
            put("top_p", 1.0)
            put("seed", seed)
            put("stream", false)
            put("max_tokens", 1600)
            putJsonObject("response_format") { put("type", "json_object") }
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", "system")
                            put("content", systemPrompt)
                        },
                    )
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put("content", userPrompt)
                        },
                    )
                },
            )
        }

        val url = endpoint.trimEnd('/').let {
            if (it.endsWith("/chat/completions")) it else "$it/chat/completions"
        }

        val req = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .apply { if (apiKey.isNotBlank()) addHeader("Authorization", "Bearer $apiKey") }
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

        client.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code}: ${raw.take(300)}")
            }
            val obj = json.parseToJsonElement(raw).jsonObject
            val content = obj["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject?.get("content")
                ?.jsonPrimitive?.content
                ?: throw IllegalStateException("响应缺少 choices[0].message.content")
            return stripCodeFence(content).trim()
        }
    }

    /**
     * 解析出 JSON 对象文本（用于校验器）。若无法解析则抛异常。
     */
    fun completeJson(systemPrompt: String, userPrompt: String, seed: Int): String {
        val text = complete(systemPrompt, userPrompt, seed)
        // 校验可解析性
        json.parseToJsonElement(text).jsonObject
        return text
    }

    /** 剥离可能的 ```json ... ``` 包裹 */
    private fun stripCodeFence(s: String): String {
        val t = s.trim()
        if (!t.startsWith("```")) return t
        val body = t.removePrefix("```")
            .removePrefix("json")
            .removePrefix("JSON")
            .trimStart('\n', '\r')
        val end = body.lastIndexOf("```")
        return if (end >= 0) body.substring(0, end) else body
    }
}
