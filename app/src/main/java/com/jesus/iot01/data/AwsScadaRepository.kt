package com.jesus.iot01.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class RemoteScada(
    val scadaId: String,
    val userId: String,
    val title: String,
    val imageUrl: String,
    val sensors: List<RemoteSensor>,
    val createdAt: String
)

data class RemoteSensor(
    val id: String,
    val name: String,
    val value: String,
    val unit: String,
    val offsetX: Float,
    val offsetY: Float
)

class AwsScadaRepository {

    companion object {
        private const val BASE_URL =
            "https://5cedy70arf.execute-api.us-east-1.amazonaws.com/prod"
        const val TEMP_USER_ID = "user_test_001"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Solo hace parseo doble si detecta el wrapper de statusCode
    private fun parseApiGatewayResponse(rawBody: String): JSONObject {
        val outer = JSONObject(rawBody)
        return if (outer.has("statusCode") && outer.has("body")) {
            val inner = outer.getString("body")
            JSONObject(inner)
        } else {
            outer
        }
    }

    //  GUARDAR SCADA
    suspend fun saveScada(
        scadaId: String,
        title: String,
        bitmap: Bitmap,
        sensors: List<RemoteSensor>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sensorsArray = JSONArray()
            sensors.forEach { s ->
                sensorsArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("value", s.value)
                    put("unit", s.unit)
                    put("offsetX", s.offsetX)
                    put("offsetY", s.offsetY)
                })
            }

            val metadataJson = JSONObject().apply {
                put("scadaId", scadaId)
                put("userId", TEMP_USER_ID)
                put("title", title)
                put("sensors", sensorsArray)
            }

            val metadataRequest = Request.Builder()
                .url("$BASE_URL/scadas")
                .post(metadataJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val metadataResponse = client.newCall(metadataRequest).execute()
            val metadataBody = metadataResponse.body?.string()
                ?: return@withContext Result.failure(Exception("Respuesta vacía de API Gateway"))

            android.util.Log.d("AWS_DEBUG", "Respuesta saveScada: $metadataBody")

            val parsedResponse = parseApiGatewayResponse(metadataBody)

            if (!parsedResponse.has("signedUrl")) {
                val errorMsg = parsedResponse.optString("error", "Sin signedUrl en respuesta")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val signedUrl = parsedResponse.getString("signedUrl")

            // Subir imagen a S3
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val imageBytes = stream.toByteArray()

            val uploadRequest = Request.Builder()
                .url(signedUrl)
                .put(imageBytes.toRequestBody("image/png".toMediaType()))
                .build()

            val uploadResponse = client.newCall(uploadRequest).execute()
            android.util.Log.d("AWS_DEBUG", "Upload S3 code: ${uploadResponse.code}")

            if (uploadResponse.isSuccessful) {
                Result.success(scadaId)
            } else {
                Result.failure(Exception("Error subiendo imagen a S3: ${uploadResponse.code}"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // CARGAR SCADAS DEL USUARIO
    suspend fun getScadas(): Result<List<RemoteScada>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/scadas?userId=$TEMP_USER_ID")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.success(emptyList())

            android.util.Log.d("AWS_DEBUG", "Respuesta getScadas: $body")

            val parsed = parseApiGatewayResponse(body)

            if (!parsed.has("scadas")) {
                return@withContext Result.success(emptyList())
            }

            val scadasArray = parsed.getJSONArray("scadas")

            if (scadasArray.length() == 0) {
                return@withContext Result.success(emptyList())
            }

            val scadas = mutableListOf<RemoteScada>()

            for (i in 0 until scadasArray.length()) {
                val item = scadasArray.getJSONObject(i)
                val sensorsArray = item.getJSONArray("sensors")
                val sensorsList = mutableListOf<RemoteSensor>()

                for (j in 0 until sensorsArray.length()) {
                    val s = sensorsArray.getJSONObject(j)
                    sensorsList.add(
                        RemoteSensor(
                            id = s.getString("id"),
                            name = s.getString("name"),
                            value = s.getString("value"),
                            unit = s.getString("unit"),
                            offsetX = s.getDouble("offsetX").toFloat(),
                            offsetY = s.getDouble("offsetY").toFloat()
                        )
                    )
                }

                scadas.add(
                    RemoteScada(
                        scadaId   = item.getString("scadaId"),
                        userId    = item.getString("userId"),
                        title     = item.getString("title"),
                        imageUrl  = item.getString("imageUrl"),
                        sensors   = sensorsList,
                        createdAt = item.getString("createdAt")
                    )
                )
            }

            Result.success(scadas)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // DESCARGAR IMAGEN DESDE S3
    suspend fun downloadImage(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(imageUrl).get().build()
            val response = client.newCall(request).execute()
            val bytes = response.body?.bytes() ?: return@withContext null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            android.util.Log.e("AWS_DEBUG", "Error descargando imagen: ${e.message}")
            null
        }
    }

    // ACTUALIZAR SCADA
    suspend fun updateScada(
        scadaId: String,
        title: String,
        bitmap: Bitmap,
        sensors: List<RemoteSensor>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sensorsArray = JSONArray()
            sensors.forEach { s ->
                sensorsArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("value", s.value)
                    put("unit", s.unit)
                    put("offsetX", s.offsetX)
                    put("offsetY", s.offsetY)
                })
            }

            val json = JSONObject().apply {
                put("scadaId", scadaId)
                put("userId", TEMP_USER_ID)
                put("title", title)
                put("sensors", sensorsArray)
            }

            val request = Request.Builder()
                .url("$BASE_URL/scadas/$scadaId")
                .put(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Respuesta vacía"))

            android.util.Log.d("AWS_DEBUG", "Respuesta updateScada: $body")

            val parsedResponse = parseApiGatewayResponse(body)

            if (!parsedResponse.has("signedUrl")) {
                val errorMsg = parsedResponse.optString("error", "Sin signedUrl en respuesta")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val signedUrl = parsedResponse.getString("signedUrl")

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val imageBytes = stream.toByteArray()

            val uploadRequest = Request.Builder()
                .url(signedUrl)
                .put(imageBytes.toRequestBody("image/png".toMediaType()))
                .build()

            val uploadResponse = client.newCall(uploadRequest).execute()
            android.util.Log.d("AWS_DEBUG", "Update S3 code: ${uploadResponse.code}")

            if (uploadResponse.isSuccessful) {
                Result.success(scadaId)
            } else {
                Result.failure(Exception("Error actualizando imagen: ${uploadResponse.code}"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ELIMINAR SCADA
    suspend fun deleteScada(scadaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("scadaId", scadaId)
                put("userId", TEMP_USER_ID)
            }

            val request = Request.Builder()
                .url("$BASE_URL/scadas/$scadaId")
                .delete(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error eliminando SCADA: ${response.code}"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}