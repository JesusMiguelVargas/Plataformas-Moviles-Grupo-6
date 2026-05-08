package com.jesus.iot01.ui.scada

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesus.iot01.data.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Modelo local para mostrar en UI
data class SavedScadaSensor(
    val id: String,
    val name: String,
    val value: String,
    val unit: String,
    val offsetX: Float,
    val offsetY: Float
)

data class SavedScada(
    val id: String,
    val title: String,
    val bitmap: Bitmap,
    val sensors: List<SavedScadaSensor>
)

class ScadaGenerationViewModel : ViewModel() {

    // --- ESTADO DE GENERACIÓN ---
    var isGenerating by mutableStateOf(false)
        private set

    var generatedBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // --- ESTADO AWS ---
    var isSaving by mutableStateOf(false)
        private set

    var isLoadingScadas by mutableStateOf(false)
        private set

    var awsErrorMessage by mutableStateOf<String?>(null)
        private set

    // --- ESTADO DEL EDITOR ACTIVO ---
    var editingScadaId by mutableStateOf<String?>(null)
        private set

    // LISTA LOCAL DE SCADAs (cache de lo que viene de AWS) ---
    val savedScadas = mutableStateListOf<SavedScada>()

    // REPOSITORIO AWS
    private val awsRepository = AwsScadaRepository()

    //  RETROFIT OPENAI
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenAiApiService::class.java)

    //  CARGAR UN SCADA GUARDADO PARA EDITAR
    fun loadScadaForEditing(scadaId: String) {
        val scada = savedScadas.find { it.id == scadaId } ?: return
        generatedBitmap = scada.bitmap
        editingScadaId = scadaId
    }

    // UARDAR O ACTUALIZAR EN AWS
    fun saveScada(
        title: String,
        currentBitmap: Bitmap,
        placedSensors: List<PlacedSensor>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            isSaving = true
            awsErrorMessage = null

            try {
                // Convertir PlacedSensor → RemoteSensor
                val remoteSensors = placedSensors.map { s ->
                    RemoteSensor(
                        id = s.id,
                        name = s.name,
                        value = s.value,
                        unit = s.unit,
                        offsetX = s.offset.x,
                        offsetY = s.offset.y
                    )
                }

                val isEditing = editingScadaId != null
                val scadaId = editingScadaId ?: System.currentTimeMillis().toString()

                val result = if (isEditing) {
                    // Actualizar existente en AWS
                    Log.d("AWS_DEBUG", "Actualizando SCADA $scadaId en AWS...")
                    awsRepository.updateScada(scadaId, title, currentBitmap, remoteSensors)
                } else {
                    // Crear nuevo en AWS
                    Log.d("AWS_DEBUG", "Guardando nuevo SCADA $scadaId en AWS...")
                    awsRepository.saveScada(scadaId, title, currentBitmap, remoteSensors)
                }

                result.fold(
                    onSuccess = {
                        Log.d("AWS_DEBUG", "✅ SCADA guardado en AWS correctamente")

                        // Actualizar cache local
                        val localSensors = placedSensors.map { s ->
                            SavedScadaSensor(s.id, s.name, s.value, s.unit, s.offset.x, s.offset.y)
                        }

                        val existingIndex = savedScadas.indexOfFirst { it.id == scadaId }
                        if (existingIndex >= 0) {
                            savedScadas[existingIndex] = savedScadas[existingIndex].copy(
                                title = title,
                                bitmap = currentBitmap,
                                sensors = localSensors
                            )
                        } else {
                            savedScadas.add(
                                SavedScada(
                                    id = scadaId,
                                    title = title,
                                    bitmap = currentBitmap,
                                    sensors = localSensors
                                )
                            )
                            editingScadaId = scadaId
                        }

                        onSuccess()
                    },
                    onFailure = { e ->
                        Log.e("AWS_DEBUG", "❌ Error guardando en AWS: ${e.message}")
                        awsErrorMessage = "Error al guardar: ${e.message}"
                        onError(e.message ?: "Error desconocido")
                    }
                )

            } catch (e: Exception) {
                Log.e("AWS_DEBUG", "❌ Excepción: ${e.message}")
                awsErrorMessage = e.message
                onError(e.message ?: "Error desconocido")
            } finally {
                isSaving = false
            }
        }
    }

    // CARGAR SCADAs DESDE AWS
    fun loadScadasFromAws() {
        viewModelScope.launch {
            isLoadingScadas = true
            awsErrorMessage = null

            try {
                Log.d("AWS_DEBUG", "Cargando SCADAs desde AWS...")
                val result = awsRepository.getScadas()

                result.fold(
                    onSuccess = { remoteScadas ->
                        Log.d("AWS_DEBUG", "✅ ${remoteScadas.size} SCADAs recibidos")
                        savedScadas.clear()

                        remoteScadas.forEach { remote ->
                            // Descargar imagen desde S3
                            val bitmap = awsRepository.downloadImage(remote.imageUrl)

                            if (bitmap != null) {
                                val localSensors = remote.sensors.map { s ->
                                    SavedScadaSensor(s.id, s.name, s.value, s.unit, s.offsetX, s.offsetY)
                                }
                                savedScadas.add(
                                    SavedScada(
                                        id = remote.scadaId,
                                        title = remote.title,
                                        bitmap = bitmap,
                                        sensors = localSensors
                                    )
                                )
                            }
                        }
                    },
                    onFailure = { e ->
                        Log.e("AWS_DEBUG", "❌ Error cargando SCADAs: ${e.message}")
                        awsErrorMessage = "Error al cargar: ${e.message}"
                    }
                )

            } catch (e: Exception) {
                Log.e("AWS_DEBUG", "❌ Excepción cargando: ${e.message}")
                awsErrorMessage = e.message
            } finally {
                isLoadingScadas = false
            }
        }
    }

    // --- ELIMINAR SCADA DE AWS ---
    fun deleteScada(scadaId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val result = awsRepository.deleteScada(scadaId)
                result.fold(
                    onSuccess = {
                        savedScadas.removeAll { it.id == scadaId }
                        Log.d("AWS_DEBUG", "✅ SCADA eliminado")
                        onSuccess()
                    },
                    onFailure = { e ->
                        Log.e("AWS_DEBUG", "❌ Error eliminando: ${e.message}")
                        awsErrorMessage = e.message
                    }
                )
            } catch (e: Exception) {
                awsErrorMessage = e.message
            }
        }
    }

    //  RESET PARA NUEVO SCADA
    fun resetForNewScada() {
        generatedBitmap = null
        errorMessage = null
        editingScadaId = null
    }

    // GENERAR SCADA CON IA
    fun generateScada(userPrompt: String, onImageReady: () -> Unit) {
        if (userPrompt.isBlank()) return

        val apiKey = ""

        viewModelScope.launch {
            isGenerating = true
            errorMessage = null
            generatedBitmap = null
            editingScadaId = null

            try {
                Log.d("OPENAI_DEBUG", "Fase 1: Mejorando el prompt del usuario...")

                val systemInstructions = """
                You are an Expert UI/UX Designer specialized in Modern Industrial Dashboards.
                Convert the user's idea into a CONCISE technical prompt in ENGLISH for an AI image generator.
                STRICT RULES:
                - NO lists, NO bullet points, NO Spanish, NO introductory text.
                - CRITICAL STYLE: Clean, modern, minimalist isometric digital twin.
                - COLOR PALETTE: Light mode, soft pastel colors, clean white or very light gray background.
                - NO dark mode, NO complex P&ID engineering symbols, NO text, NO numbers, NO UI elements, NO borders.
                - COMPOSITION: Leave ample negative space so the user can overlay data cards later.
                - OUTPUT ONLY THE ENGLISH PROMPT.
                """.trimIndent()

                val chatResponse = api.getRefinedPrompt(
                    apiKey,
                    ChatRequest(
                        messages = listOf(
                            ChatMessage("system", systemInstructions),
                            ChatMessage("user", "Idea: $userPrompt")
                        )
                    )
                )

                val refinedPrompt = chatResponse.choices.firstOrNull()?.message?.content?.trim()

                if (!refinedPrompt.isNullOrEmpty()) {
                    Log.d("OPENAI_DEBUG", "Prompt Refinado: $refinedPrompt")
                    Log.d("OPENAI_DEBUG", "Fase 2: Enviando a gpt-image-1...")

                    val imageResponse = api.generateImage(
                        apiKey,
                        ImageRequest(prompt = refinedPrompt)
                    )

                    val b64 = imageResponse.data.firstOrNull()?.b64_json

                    if (!b64.isNullOrBlank()) {
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                        if (bitmap != null) {
                            Log.d("OPENAI_DEBUG", "¡Imagen generada con éxito!")
                            generatedBitmap = bitmap
                            onImageReady()
                        } else {
                            errorMessage = "Error: No se pudo decodificar la imagen."
                        }
                    } else {
                        errorMessage = "Error: El servidor no devolvió imagen en b64_json."
                    }
                } else {
                    errorMessage = "Error: Falló la traducción del prompt en la Fase 1."
                }

            } catch (e: Exception) {
                Log.e("OPENAI_DEBUG", "Error Crítico: ${e.localizedMessage}")
                errorMessage = "Hubo un problema de red o tu API Key agotó su saldo."
            } finally {
                isGenerating = false
            }
        }
    }
}