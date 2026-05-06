package com.jesus.iot01.data // <--- IMPORTANTE: Sin el ".scada"

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiApiService {
    @POST("v1/images/generations")
    suspend fun generateImage(
        @Header("Authorization") token: String,
        @Body request: ImageRequest
    ): ImageResponse
}

data class ImageRequest(
    val model: String = "dall-e-3",
    val prompt: String,
    val n: Int = 1,
    val size: String = "1792x1024" // <--- CAMBIAR AQUÍ PARA HORIZONTAL
)

data class ImageResponse(val data: List<ImageData>)
data class ImageData(val url: String) // Actualización