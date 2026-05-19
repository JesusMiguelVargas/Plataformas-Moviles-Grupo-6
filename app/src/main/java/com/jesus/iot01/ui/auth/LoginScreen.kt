package com.jesus.iot01.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.jesus.iot01.R
import com.jesus.iot01.data.CognitoRepository
import com.jesus.iot01.data.UserSession
import com.jesus.iot01.navigation.AppScreens
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val cognitoRepository = remember { CognitoRepository() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.2f))

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Zyntec Logo",
            modifier = Modifier
                .size(300.dp)
                .padding(bottom = 8.dp)
        )

        Text(
            text = "IoT Control",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )

        Spacer(modifier = Modifier.weight(0.5f))

        // Campo email
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Email, contentDescription = "Email Icon")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            isError = errorMessage != null,
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                errorBorderColor = Color(0xFFE53935)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo password
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Contraseña") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock Icon")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = errorMessage != null,
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                errorBorderColor = Color(0xFFE53935)
            )
        )

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

        Spacer(modifier = Modifier.height(30.dp))

        // Botón con Cognito
        Button(
            onClick = {
                when {
                    email.isBlank() || password.isBlank() -> {
                        errorMessage = "Por favor completa todos los campos."
                    }
                    else -> {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val result = cognitoRepository.login(email, password)
                            result.fold(
                                onSuccess = { user ->
                                    //  Guardar sesión real
                                    UserSession.setUser(user)
                                    android.util.Log.d("COGNITO", "Login OK — userId: ${user.userId}")
                                    navController.navigate(AppScreens.Variables.route) {
                                        popUpTo(AppScreens.Login.route) { inclusive = true }
                                    }
                                },
                                onFailure = { e ->
                                    errorMessage = when {
                                        e.message?.contains("NotAuthorizedException") == true ||
                                                e.message?.contains("Incorrect username or password") == true ->
                                            "Email o contraseña incorrectos."
                                        e.message?.contains("UserNotConfirmedException") == true ->
                                            "Debes confirmar tu cuenta. Revisa tu email."
                                        e.message?.contains("UserNotFoundException") == true ->
                                            "No existe una cuenta con ese email."
                                        else -> e.message ?: "Error de conexión."
                                    }
                                    isLoading = false
                                }
                            )
                        }
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
                Text(text = "Entrar", fontWeight = FontWeight.Bold)
            }
        }

        TextButton(
            onClick = { navController.navigate(AppScreens.Register.route) },
            enabled = !isLoading
        ) {
            Text(text = "¿No tienes cuenta? Regístrate")
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    val fakeNavController = rememberNavController()
    LoginScreen(navController = fakeNavController)
}