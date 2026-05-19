package com.jesus.iot01.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jesus.iot01.data.CognitoRepository
import com.jesus.iot01.navigation.AppScreens
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showConfirmField by remember { mutableStateOf(false) }

    val cognitoRepository = remember { CognitoRepository() }
    val scope = rememberCoroutineScope()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Crear Cuenta",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(32.dp))

            //  Campos de registro — se ocultan después del registro
            if (!showConfirmField) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Nombre Completo") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    isError = errorMessage != null
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !isLoading,
                    isError = errorMessage != null
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !isLoading,
                    isError = errorMessage != null
                )

            } else {
                // Campo de confirmación — aparece después del registro
                Text(
                    text = "Revisa tu email: $email",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmCode,
                    onValueChange = { confirmCode = it; errorMessage = null },
                    label = { Text("Código de verificación") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !isLoading,
                    isError = errorMessage != null
                )
            }

            // Mensaje de error
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFE53935),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }

            // Mensaje de éxito
            if (successMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = successMessage ?: "",
                    color = Color(0xFF43A047),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (!showConfirmField) {
                        // PASO 1 — Registrar en Cognito
                        when {
                            name.isBlank() || email.isBlank() || password.isBlank() -> {
                                errorMessage = "Por favor completa todos los campos."
                            }
                            password.length < 8 -> {
                                errorMessage = "La contraseña debe tener al menos 8 caracteres."
                            }
                            else -> {
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    val result = cognitoRepository.register(email, password)
                                    result.fold(
                                        onSuccess = { message ->
                                            successMessage = message
                                            showConfirmField = true
                                            isLoading = false
                                        },
                                        onFailure = { e ->
                                            errorMessage = when {
                                                e.message?.contains("UsernameExistsException") == true ->
                                                    "Ya existe una cuenta con ese email."
                                                e.message?.contains("InvalidPasswordException") == true ->
                                                    "La contraseña debe tener mayúsculas, números y mínimo 8 caracteres."
                                                else -> e.message ?: "Error al registrar."
                                            }
                                            isLoading = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // PASO 2 — Confirmar código
                        if (confirmCode.isBlank()) {
                            errorMessage = "Ingresa el código de verificación."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val result = cognitoRepository.confirmAccount(email, confirmCode)
                            result.fold(
                                onSuccess = { message ->
                                    successMessage = message
                                    isLoading = false
                                    // Ir al login después de confirmar
                                    navController.navigate(AppScreens.Login.route) {
                                        popUpTo(AppScreens.Register.route) { inclusive = true }
                                    }
                                },
                                onFailure = { e ->
                                    errorMessage = when {
                                        e.message?.contains("CodeMismatchException") == true ->
                                            "Código incorrecto. Intenta de nuevo."
                                        e.message?.contains("ExpiredCodeException") == true ->
                                            "El código expiró. Regístrate de nuevo."
                                        else -> e.message ?: "Error al confirmar."
                                    }
                                    isLoading = false
                                }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 40.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (showConfirmField) "Confirmar cuenta" else "Registrarse",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TextButton(
                onClick = { navController.popBackStack() },
                enabled = !isLoading
            ) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }
        }
    }
}