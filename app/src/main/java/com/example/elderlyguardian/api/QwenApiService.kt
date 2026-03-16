package com.example.elderlyguardian.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 阿里云千问(Qwen) API 服务
 * 用于语音对话和智能问答
 */
class QwenApiService {
    companion object {
        private const val TAG = "QwenApiService"

        // OpenClaw 代理地址（需要根据您的实际配置修改）
        private const val BASE_URL = "http://localhost:3000/v1/chat/completions"

        // 或者使用阿里云官方API
        private const val ALIYUN_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"

        // 默认使用通义千问模型
        private const val DEFAULT_MODEL = "qwen-turbo"
    }

    /**
     * 发送对话请求
     * @param message 用户输入的消息
     * @param apiKey API密钥
     * @param useOpenClaw 是否使用OpenClaw代理
     * @return AI回复内容
     */
    suspend fun sendMessage(
        message: String,
        apiKey: String,
        useOpenClaw: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = if (useOpenClaw) {
                // 使用OpenClaw代理
                URL(BASE_URL)
            } else {
                // 直接使用阿里云API
                URL(ALIYUN_API_URL)
            }

            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
            }

            // 构建请求体
            val requestBody = if (useOpenClaw) {
                // OpenClaw格式
                JSONObject().apply {
                    put("model", DEFAULT_MODEL)
                    put("messages", listOf(
                        mapOf("role" to "system", "content" to "你是一个贴心的老年人助手，请用简单易懂的语言回答问题。"),
                        mapOf("role" to "user", "content" to message)
                    ))
                    put("temperature", 0.7)
                    put("max_tokens", 500)
                }.toString()
            } else {
                // 阿里云官方API格式
                JSONObject().apply {
                    put("model", DEFAULT_MODEL)
                    put("input", JSONObject().apply {
                        put("messages", listOf(
                            mapOf("role" to "system", "content" to "你是一个贴心的老年人助手，请用简单易懂的语言回答问题。"),
                            mapOf("role" to "user", "content" to message)
                        ))
                    })
                    put("parameters", JSONObject().apply {
                        put("temperature", 0.7)
                        put("max_tokens", 500)
                    })
                }.toString()
            }

            // 发送请求
            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray())
            }

            // 读取响应
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Response: $response")

                // 解析响应
                val jsonResponse = JSONObject(response)
                if (useOpenClaw) {
                    // OpenClaw格式
                    jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else {
                    // 阿里云格式
                    jsonResponse.getJSONObject("output")
                        .getString("text")
                }
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "Error: $error")
                "抱歉，我暂时无法回答，请稍后再试。"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            "抱歉，网络出现问题，请检查网络连接。"
        }
    }

    /**
     * 测试API连接
     */
    suspend fun testConnection(apiKey: String, useOpenClaw: Boolean = true): Boolean {
        return try {
            val response = sendMessage("你好", apiKey, useOpenClaw)
            response.isNotBlank() && !response.contains("抱歉")
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 使用示例：
 *
 * // 在ViewModel或Activity中使用
 * val qwenService = QwenApiService()
 *
 * // 发送消息
 * lifecycleScope.launch {
 *     val response = qwenService.sendMessage(
 *         message = "今天天气怎么样？",
 *         apiKey = "your-api-key-here",
 *         useOpenClaw = true
 *     )
 *     // 处理回复
 *     textToSpeech.speak(response, ...)
 * }
 *
 * // 测试连接
 * lifecycleScope.launch {
 *     val isConnected = qwenService.testConnection("your-api-key")
 *     if (isConnected) {
 *         // API连接正常
 *     }
 * }
 */