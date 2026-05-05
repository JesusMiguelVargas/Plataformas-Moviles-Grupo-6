
package com.jesus.iot01.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jesus.iot01.navigation.AppScreens

@Composable
fun ConnectionCheckScreen(navController: NavController) {
    // Simulamos un estado de "Buscando señal"
    var isDataReceived by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isDataReceived) {
            // Indicador de carga circular para dar sensación de búsqueda
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                color = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Esperando señal de tu equipo...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Asegúrate de que tu Gateway esté encendido y enviando datos al tópico configurado.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            // BOTÓN TEMPORAL para simular que llegó un dato
            Button(
                onClick = { isDataReceived = true },
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text("Simular recepción de datos")
            }

        } else {
            // Cuando "detecta" el dato
            Text("¡Conexión Exitosa!", fontSize = 24.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            Text("Hemos recibido el primer paquete de datos.", fontSize = 16.sp)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate(AppScreens.Variables.route) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Ir al Tablero de Variables")
            }
        }
    }
}