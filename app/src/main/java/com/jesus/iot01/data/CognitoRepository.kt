package com.jesus.iot01.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CognitoUser(
    val userId: String,
    val email: String,
    val accessToken: String,
    val idToken: String,
    val refreshToken: String
)

class CognitoRepository {

    companion object {
        private const val REGION = "us-east-1"
        private const val CLIENT_ID = "78gac5mbj94qhvv5cc88s2b7rh"
        private const val COGNITO_URL =
            "https://cognito-idp.$REGION.amazonaws.com/"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Login con email y password
    suspend fun login(
        email: String,
        password: String
    ): Result<CognitoUser> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("AuthFlow", "USER_PASSWORD_AUTH")
                put("ClientId", CLIENT_ID)
                put("AuthParameters", JSONObject().apply {
                    put("USERNAME", email)
                    put("PASSWORD", password)
                })
            }

            val request = Request.Builder()
                .url(COGNITO_URL)
                .post(body.toString().toRequestBody("application/x-amz-json-1.1".toMediaType()))
                .addHeader("X-Amz-Target", "AWSCognitoIdentityProviderService.InitiateAuth")
                .addHeader("Content-Type", "application/x-amz-json-1.1")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            android.util.Log.d("COGNITO", "Login respuesta: $responseBody")

            if (!response.isSuccessful) {
                val error = JSONObject(responseBody)
                val message = error.optString("message", "Error de autenticación")
                return@withContext Result.failure(Exception(message))
            }

            val json = JSONObject(responseBody)
            val authResult = json.getJSONObject("AuthenticationResult")

            val accessToken = authResult.getString("AccessToken")
            val idToken = authResult.getString("IdToken")
            val refreshToken = authResult.getString("RefreshToken")

            //  Extraer userId del IdToken (es un JWT)
            val userId = extractUserIdFromToken(idToken)
            val email2 = extractEmailFromToken(idToken)

            Result.success(
                CognitoUser(
                    userId = userId,
                    email = email2,
                    accessToken = accessToken,
                    idToken = idToken,
                    refreshToken = refreshToken
                )
            )

        } catch (e: Exception) {
            android.util.Log.e("COGNITO", "Error login: ${e.message}")
            Result.failure(e)
        }
    }

    // Registro con email y password
    suspend fun register(
        email: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("ClientId", CLIENT_ID)
                put("Username", email)
                put("Password", password)
                put("UserAttributes", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("Name", "email")
                        put("Value", email)
                    })
                })
            }

            val request = Request.Builder()
                .url(COGNITO_URL)
                .post(body.toString().toRequestBody("application/x-amz-json-1.1".toMediaType()))
                .addHeader("X-Amz-Target", "AWSCognitoIdentityProviderService.SignUp")
                .addHeader("Content-Type", "application/x-amz-json-1.1")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            android.util.Log.d("COGNITO", "Register respuesta: $responseBody")

            if (!response.isSuccessful) {
                val error = JSONObject(responseBody)
                val message = error.optString("message", "Error de registro")
                return@withContext Result.failure(Exception(message))
            }

            Result.success("Registro exitoso. Revisa tu email para confirmar tu cuenta.")

        } catch (e: Exception) {
            android.util.Log.e("COGNITO", "Error register: ${e.message}")
            Result.failure(e)
        }
    }

    //  Confirmar cuenta con código de verificación
    suspend fun confirmAccount(
        email: String,
        code: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("ClientId", CLIENT_ID)
                put("Username", email)
                put("ConfirmationCode", code)
            }

            val request = Request.Builder()
                .url(COGNITO_URL)
                .post(body.toString().toRequestBody("application/x-amz-json-1.1".toMediaType()))
                .addHeader("X-Amz-Target", "AWSCognitoIdentityProviderService.ConfirmSignUp")
                .addHeader("Content-Type", "application/x-amz-json-1.1")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val error = JSONObject(responseBody)
                val message = error.optString("message", "Error de confirmación")
                return@withContext Result.failure(Exception(message))
            }

            Result.success("Cuenta confirmada. Ya puedes iniciar sesión.")

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //  Extraer userId (sub) del IdToken JWT
    private fun extractUserIdFromToken(idToken: String): String {
        return try {
            val payload = idToken.split(".")[1]
            val decoded = String(
                android.util.Base64.decode(
                    payload.padEnd((payload.length + 3) / 4 * 4, '='),
                    android.util.Base64.URL_SAFE
                )
            )
            JSONObject(decoded).getString("sub")
        } catch (e: Exception) {
            "unknown_user"
        }
    }

    // Extraer email del IdToken JWT
    private fun extractEmailFromToken(idToken: String): String {
        return try {
            val payload = idToken.split(".")[1]
            val decoded = String(
                android.util.Base64.decode(
                    payload.padEnd((payload.length + 3) / 4 * 4, '='),
                    android.util.Base64.URL_SAFE
                )
            )
            JSONObject(decoded).optString("email", "")
        } catch (e: Exception) {
            ""
        }
    }
}