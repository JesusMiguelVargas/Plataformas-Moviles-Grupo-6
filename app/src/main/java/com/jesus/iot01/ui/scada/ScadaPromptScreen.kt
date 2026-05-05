package com.jesus.iot01.ui.scada

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jesus.iot01.navigation.AppScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScadaPromptScreen(
    navController: NavController,
    viewModel: ScadaGenerationViewModel = viewModel() // Inyección del ViewModel corregido
) {
    var promptText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IA Builder SCADA", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Variables") },
                    selected = false,
                    onClick = { navController.navigate(AppScreens.Variables.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("SCADA") },
                    selected = true,
                    onClick = { /* Estamos aquí */ }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono de IA
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF673AB7)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Describe tu proceso",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "La IA diseñará el fondo industrial. Luego podrás arrastrar tus variables sobre los equipos.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo de texto para el Prompt
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = {
                    Text(
                        "Ej: Planta de destilación con 3 columnas de fraccionamiento y tuberías de vapor...",
                        fontSize = 14.sp
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF673AB7),
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color(0xFF673AB7),
                    cursorColor = Color(0xFF673AB7)
                ),
                enabled = !viewModel.isGenerating // Bloquea el campo mientras genera
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Botón de Generar conectado al ViewModel
            Button(
                onClick = {
                    // Llamamos a la función del ViewModel que creamos con Retrofit
                    viewModel.generateScada(promptText) {
                        // Este bloque se ejecuta solo cuando la imagen está lista
                        navController.navigate(AppScreens.ScadaEditor.route)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF673AB7),
                    disabledContainerColor = Color.Gray
                ),
                // Se deshabilita si el texto está vacío o si ya está generando
                enabled = promptText.isNotBlank() && !viewModel.isGenerating
            ) {
                if (viewModel.isGenerating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("La IA está diseñando...", fontSize = 16.sp)
                    }
                } else {
                    Text("Generar Interfaz SCADA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Mensaje de ayuda opcional
            if (viewModel.isGenerating) {
                Text(
                    text = "DALL-E 3 suele tardar entre 10 y 15 segundos",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}