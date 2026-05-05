package com.jesus.iot01.ui.scada

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jesus.iot01.navigation.AppScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScadaListScreen(navController: NavController) {
    // Lista de SCADAs ya creados (simulación)
    val myScadas = listOf("Tanque Principal", "Control de Motores", "Línea de Envasado")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mis Vistas SCADA", fontWeight = FontWeight.Bold) })
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
                    onClick = { /* Ya estamos aquí */ }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            // BOTÓN PARA CREAR NUEVO (Lleva a la página del Prompt)
            Button(
                onClick = { navController.navigate(AppScreens.ScadaPrompt.route) },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)) // Morado IA
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Crear Nueva Vista con IA")
            }

            Spacer(Modifier.height(24.dp))

            Text("Vistas Guardadas", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(16.dp))

            // Grilla de vistas SCADA existentes
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(myScadas) { name ->
                    ScadaCard(name) {
                        navController.navigate(AppScreens.ScadaView.route)
                    }
                }
            }
        }
    }
}

@Composable
fun ScadaCard(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Miniatura simulada
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ViewQuilt, contentDescription = null, tint = Color.LightGray)
            }
            // Título
            Text(
                text = title,
                modifier = Modifier.padding(8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}