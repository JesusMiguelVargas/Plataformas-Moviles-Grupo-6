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
import androidx.compose.material.icons.filled.Logout
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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jesus.iot01.navigation.AppScreens

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

    var showAddDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Variables", fontWeight = FontWeight.Bold) },
                actions = {
                    //  Botón salir — reemplaza el engranaje
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = Color(0xFFE53935)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Variables") },
                    label = { Text("Variables") },
                    selected = currentRoute == AppScreens.Variables.route,
                    onClick = {
                        if (currentRoute != AppScreens.Variables.route) {
                            navController.navigate(AppScreens.Variables.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2196F3),
                        selectedTextColor = Color(0xFF2196F3)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.GridView, contentDescription = "SCADA") },
                    label = { Text("SCADA") },
                    selected = currentRoute == AppScreens.ScadaList.route || currentRoute == AppScreens.ScadaPrompt.route,
                    onClick = {
                        if (currentRoute != AppScreens.ScadaList.route) {
                            navController.navigate(AppScreens.ScadaList.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2196F3),
                        selectedTextColor = Color(0xFF2196F3)
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("●", color = Color(0xFF4CAF50), modifier = Modifier.padding(end = 8.dp))
                    Text(
                        "Gateway: Vargas/Taller - Online",
                        color = Color(0xFF1976D2),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tus Sensores", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir variable", fontSize = 12.sp)
                }
            }

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

    //  Diálogo confirmar salida
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro que deseas salir?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        navController.navigate(AppScreens.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Salir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo añadir variable
    if (showAddDialog) {
        var newVariableName by remember { mutableStateOf("") }
        var newVariableTopic by remember { mutableStateOf("") }
        var newVariableUnit by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nueva Variable de Sensor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Vincula esta variable a un tópico MQTT.", fontSize = 14.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = newVariableName,
                        onValueChange = { newVariableName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVariableTopic,
                        onValueChange = { newVariableTopic = it },
                        label = { Text("Tópico MQTT") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVariableUnit,
                        onValueChange = { newVariableUnit = it },
                        label = { Text("Unidad") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newVariableName.isNotBlank() && newVariableTopic.isNotBlank()) {
                            currentVariables = currentVariables + SensorVariable(
                                (currentVariables.size + 1).toString(),
                                newVariableName,
                                "--",
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