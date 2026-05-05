package com.jesus.iot01.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.jesus.iot01.navigation.AppScreens

// Modelo de datos ficticio para probar la UI
data class SensorVariable(
    val id: String,
    val name: String,
    val value: String,
    val unit: String,
    val topic: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariablesScreen(navController: NavController) {
    // ESTADO: Lista de variables (por ahora ficticia)
    var currentVariables by remember {
        mutableStateOf(
            listOf(
                SensorVariable("1", "Temperatura", "24.5", "°C", "taller/temp"),
                SensorVariable("2", "Humedad", "60", "%", "taller/hum"),
                SensorVariable("3", "Voltaje", "220", "V", "linea1/volt"),
                SensorVariable("4", "Presión", "1.2", "Bar", "linea1/pres")
            )
        )
    }

    // ESTADO: Controlar la visibilidad del Pop-up
    var showAddDialog by remember { mutableStateOf(false) }

    // ESTADO: Navegación inferior
    var selectedItem by remember { mutableIntStateOf(0) }
    val navItems = listOf("Variables", "SCADA")
    val navIcons = listOf(Icons.Default.List, Icons.Default.GridView) // Íconos típicos de lista y grilla

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Variables", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* Configuración general */ }) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                }
            )
        },
        // --- AQUÍ AÑADIMOS EL NAVIGATION BAR INFERIOR (Como Imagen 2) ---
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(navIcons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            // Navegación real
                            if (index == 1) { // Si hace clic en SCADA
                                navController.navigate(AppScreens.ScadaList.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2196F3), // Azul técnico
                            selectedTextColor = Color(0xFF2196F3)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Estado de Conexión (lo mantenemos porque es SaaS IoT)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("●", color = Color(0xFF4CAF50), modifier = Modifier.padding(end = 8.dp)) // Punto verde
                    Text("Gateway: Vargas/Taller - Online", color = Color(0xFF1976D2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 2. --- SECCIÓN DE AÑADIR VARIABLE (Compacto y arriba de las cards) ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tus Sensores", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                // Botón compacto (con ícono y texto pequeño)
                Button(
                    onClick = { showAddDialog = true }, // Abre el Pop-up
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir variable", fontSize = 12.sp)
                }
            }

            // 3. Grilla de sensores (mantenemos 2 variables por fila, como te gusta)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(currentVariables) { variable ->
                    SensorCard(
                        label = variable.name,
                        value = variable.value,
                        unit = variable.unit
                    )
                }
            }
        }
    }

    // --- POP-UP PARA AÑADIR NUEVA VARIABLE ( AlertDialog) ---
    if (showAddDialog) {
        // Contemplando MQTT
        var newVariableName by remember { mutableStateOf("") }
        var newVariableTopic by remember { mutableStateOf("") }
        var newVariableUnit by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nueva Variable de Sensor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Configura la variable para vincularla a un tópico MQTT.", fontSize = 14.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = newVariableName,
                        onValueChange = { newVariableName = it },
                        label = { Text("Nombre (ej: Temperatura Horno)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVariableTopic,
                        onValueChange = { newVariableTopic = it },
                        label = { Text("Tópico MQTT (ej: horno/sensor1/temp)") },
                        placeholder = { Text("Es el ID de tu equipo mecatrónico") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVariableUnit,
                        onValueChange = { newVariableUnit = it },
                        label = { Text("Unidad (ej: °C, Bar, V, %)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Por ahora solo cerramos el diálogo y limpiamos
                        // Lógica futura: Validar y guardar en base de datos SaaS
                        if (newVariableName.isNotBlank() && newVariableTopic.isNotBlank()) {
                            // Simulación: Añadir a la lista local
                            currentVariables = currentVariables + SensorVariable(
                                (currentVariables.size + 1).toString(),
                                newVariableName,
                                "--", // Valor inicial vacío hasta que llegue MQTT
                                newVariableUnit,
                                newVariableTopic
                            )
                        }
                        showAddDialog = false
                    }
                ) {
                    Text("Añadir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Mantenemos tu SensorCard favorita
@Composable
fun SensorCard(label: String, value: String, unit: String) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 14.sp, color = Color.Gray)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(unit, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
            }
        }
    }
}