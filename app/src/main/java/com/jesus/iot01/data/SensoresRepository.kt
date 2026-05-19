package com.jesus.iot01.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SensorItem(
    val sensorId: String,
    val userId: String,
    val nombre: String,
    val topico: String,
    val unidad: String,
    val createdAt: String
)

class SensoresRepository {

    companion object {
        private const val BASE_URL =
            "https://5cedy70arf.execute-api.us-east-1.amazonaws.com/prod"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getSensores(): Result<List<SensorItem>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/sensores?userId=${UserSession.userId}") // userId real
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.success(emptyList())

            android.util.Log.d("SENSORES", "Respuesta: $body")

            val json = JSONObject(body)
            if (!json.has("sensores")) return@withContext Result.success(emptyList())

            val array = json.getJSONArray("sensores")
            val sensores = mutableListOf<SensorItem>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                sensores.add(
                    SensorItem(
                        sensorId  = item.getString("sensorId"),
                        userId    = item.getString("userId"),
                        nombre    = item.getString("nombre"),
                        topico    = item.getString("topico"),
                        unidad    = item.optString("unidad", ""),
                        createdAt = item.getString("createdAt")
                    )
                )
            }

            Result.success(sensores)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSensor(
        nombre: String,
        topico: String,
        unidad: String
    ): Result<SensorItem> = withContext(Dispatchers.IO) {
        try {
            val sensorId = System.currentTimeMillis().toString()

            val json = JSONObject().apply {
                put("sensorId", sensorId)
                put("userId", UserSession.userId) //  userId real
                put("nombre", nombre)
                put("topico", topico)
                put("unidad", unidad)
            }

            val request = Request.Builder()
                .url("$BASE_URL/sensores")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            android.util.Log.d("SENSORES", "Save respuesta: $responseBody")

            if (response.isSuccessful) {
                Result.success(
                    SensorItem(
                        sensorId  = sensorId,
                        userId    = UserSession.userId,
                        nombre    = nombre,
                        topico    = topico,
                        unidad    = unidad,
                        createdAt = System.currentTimeMillis().toString()
                    )
                )
            } else {
                Result.failure(Exception("Error guardando sensor: ${response.code}"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSensor(sensorId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("sensorId", sensorId)
                put("userId", UserSession.userId) //  userId real
            }

            val request = Request.Builder()
                .url("$BASE_URL/sensores/$sensorId")
                .delete(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Error eliminando: ${response.code}"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}