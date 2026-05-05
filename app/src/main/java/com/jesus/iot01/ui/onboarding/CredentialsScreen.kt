package com.jesus.iot01.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jesus.iot01.navigation.AppScreens

@Composable
fun CredentialsScreen(navController: NavController) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Configura tu Gateway", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Copia estos datos en tu ESP32 / Arduino", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(30.dp))

            // Caja de credenciales (Diseño tipo código)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                CredentialItem("BROKER", "mqtt.iot-saas.com")
                CredentialItem("PORT", "1883")
                CredentialItem("CLIENT_ID", "user_jm_01")
                CredentialItem("TOPIC", "vargas/data/sensors")
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { navController.navigate(AppScreens.ConnectionCheck.route) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Ya configuré mi equipo")
            }
        }
    }
}

@Composable
fun CredentialItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.DarkGray)
        Text(value, fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF2E7D32))
    }
}