package com.jesus.iot01.ui.scada

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesus.iot01.data.ImageRequest
import com.jesus.iot01.data.OpenAiApiService
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ScadaGenerationViewModel : ViewModel() {

    var isGenerating by mutableStateOf(false)
        private set

    var generatedImageUrl by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // 1. Configuramos un cliente de red con "paciencia" (Timeouts)
    // DALL-E 3 puede tardar hasta 30 segundos en generar una imagen compleja.
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // 2. Integramos el cliente en Retrofit
    private val api = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenAiApiService::class.java)

    fun generateScada(userPrompt: String, onImageReady: () -> Unit) {
        if (userPrompt.isBlank()) return

        // Tu API Key con el prefijo Bearer corregido
        val apiKey = ""

        // Refinamos el prompt con ingeniería para que DALL-E sea preciso
        val finalPrompt = """
            Professional industrial SCADA HMI background. 
            Concept: $userPrompt. 
            Style: 2D vector, flat design, isometric, dark theme. 
            IMPORTANT: Absolute NO text, NO labels, NO numbers, NO legends. 
            High resolution, professional UI asset.
        """.trimIndent()

        viewModelScope.launch {
            isGenerating = true
            errorMessage = null

            try {
                Log.d("OPENAI_DEBUG", "Iniciando generación de imagen...")

                val response = api.generateImage(apiKey, ImageRequest(prompt = finalPrompt))
                val url = response.data.firstOrNull()?.url

                if (url != null) {
                    Log.d("OPENAI_DEBUG", "Imagen recibida: $url")
                    generatedImageUrl = url
                    onImageReady() // Dispara la navegación al Editor
                } else {
                    errorMessage = "OpenAI no devolvió una URL válida."
                    Log.e("OPENAI_DEBUG", "Respuesta vacía de la API")
                }
            } catch (e: Exception) {
                // Captura timeouts, errores de red o de API
                Log.e("OPENAI_DEBUG", "Error en la petición: ${e.message}")
                errorMessage = "Error: ${e.localizedMessage}"
            } finally {
                isGenerating = false
            }
        }
    }
}