package com.jesus.iot01.data

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiApiService {

    // FASE 1: Refinamiento técnico (GPT-4o)
    @POST("v1/chat/completions")
    suspend fun getRefinedPrompt(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): ChatResponse

    // FASE 2: Generación con GPT-IMAGE-1 (Images API)
    @POST("v1/images/generations")
    suspend fun generateImage(
        @Header("Authorization") token: String,
        @Body request: ImageRequest
    ): ImageResponse
}

// DATA PARA GPT-4o
data class ChatRequest(
    val model: String = "gpt-4o",
    val messages: List<ChatMessage>,
    val temperature: Double = 0.0
)
data class ChatMessage(val role: String, val content: String)
data class ChatResponse(val choices: List<ChatChoice>)
data class ChatChoice(val message: ChatMessage)

// DATA PARA GPT-IMAGE-1

data class ImageRequest(
    val model: String = "gpt-image-1",
    val prompt: String,
    val n: Int = 1,
    val size: String = "1536x1024",
    val quality: String = "high"
)

data class ImageResponse(val data: List<ImageData>)
data class ImageData(val b64_json: String?)
