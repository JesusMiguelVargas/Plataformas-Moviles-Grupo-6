package com.jesus.iot01.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jesus.iot01.R
import com.jesus.iot01.data.MqttManager
import com.jesus.iot01.data.SensoresRepository
import com.jesus.iot01.navigation.AppScreens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

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

    val context = LocalContext.current
    val sensoresRepository = remember { SensoresRepository() }
    val scope = rememberCoroutineScope()

    // Lista dinámica — se carga desde DynamoDB
    var currentVariables by remember { mutableStateOf<List<SensorVariable>>(emptyList()) }
    var isLoadingSensores by remember { mutableStateOf(true) }

    // Referencia al MqttManager para suscripción dinámica
    var mqttManager by remember { mutableStateOf<MqttManager?>(null) }

    var mqttStatus by remember { mutableStateOf("Conectando...") }
    var mqttStatusColor by remember { mutableStateOf(Color(0xFFFF9800)) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // PASO 1: Cargar sensores desde DynamoDB
    LaunchedEffect(Unit) {
        isLoadingSensores = true
        val result = sensoresRepository.getSensores()
        result.fold(
            onSuccess = { remoteSensores ->
                currentVariables = remoteSensores.map { s ->
                    SensorVariable(s.sensorId, s.nombre, "--", s.unidad, s.topico)
                }
                android.util.Log.d("SENSORES", "✅ ${remoteSensores.size} sensores cargados")
            },
            onFailure = { e ->
                android.util.Log.e("SENSORES", "❌ Error cargando: ${e.message}")
            }
        )
        isLoadingSensores = false
    }

    // PASO 2: Conectar MQTT cuando los sensores estén cargados
    DisposableEffect(currentVariables.size) {
        if (currentVariables.isEmpty()) return@DisposableEffect onDispose {}

        val caCert = context.resources.openRawResource(R.raw.aws_root_ca)
            .bufferedReader().use { it.readText() }
        val clientCert = context.resources.openRawResource(R.raw.aws_certificate)
            .bufferedReader().use { it.readText() }
        val privateKey = context.resources.openRawResource(R.raw.aws_private_key)
            .bufferedReader().use { it.readText() }

        val manager = MqttManager { topic, payload ->
            try {
                val json = JSONObject(payload)

                currentVariables = currentVariables.map { variable ->
                    if (variable.topic == topic) {
                        //  Parseo genérico — funciona para cualquier sensor
                        val newValue = when {
                            json.has("distancia") -> {
                                String.format("%.1f", json.optDouble("distancia", 0.0))
                            }
                            json.has("estado") -> {
                                json.optString("estado", "--")
                            }
                            json.has("valor") -> {
                                json.optString("valor", "--")
                            }
                            json.has("temperatura") -> {
                                String.format("%.1f", json.optDouble("temperatura", 0.0))
                            }
                            json.has("humedad") -> {
                                String.format("%.1f", json.optDouble("humedad", 0.0))
                            }
                            else -> {
                                // Si el payload es texto simple
                                payload
                            }
                        }
                        variable.copy(value = newValue)
                    } else {
                        variable
                    }
                }

                mqttStatus = "Online"
                mqttStatusColor = Color(0xFF4CAF50)

            } catch (e: Exception) {
                // Si el payload no es JSON intenta usarlo directo
                currentVariables = currentVariables.map { variable ->
                    if (variable.topic == topic) variable.copy(value = payload)
                    else variable
                }
            }
        }

        mqttManager = manager

        // Conectar con todos los tópicos actuales
        val topicos = currentVariables.map { it.topic }

        Thread {
            try {
                manager.connect(caCert, clientCert, privateKey, topicos)
                mqttStatus = "Online"
                mqttStatusColor = Color(0xFF4CAF50)
            } catch (e: Exception) {
                mqttStatus = "Error de conexión"
                mqttStatusColor = Color(0xFFE53935)
            }
        }.start()

        onDispose {
            manager.disconnect()
            mqttManager = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Variables", fontWeight = FontWeight.Bold) },
                actions = {
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
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
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
            // Estado conexión MQTT
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("●", color = mqttStatusColor, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        "Gateway: Vargas/Taller - $mqttStatus",
                        color = Color(0xFF1976D2),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
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

            // Estado cargando
            if (isLoadingSensores) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF2196F3))
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando sensores...", color = Color.Gray)
                    }
                }

                //  Estado vacío
            } else if (currentVariables.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📡", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No tienes sensores aún", color = Color.Gray, fontSize = 16.sp)
                        Text("Toca + Añadir variable para agregar uno", color = Color.LightGray, fontSize = 13.sp)
                    }
                }

                //  Lista de sensores
            } else {
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
                            unit = variable.unit,
                            onDelete = {
                                //  Eliminar sensor de DynamoDB y desuscribir
                                scope.launch {
                                    val result = sensoresRepository.deleteSensor(variable.id)
                                    result.fold(
                                        onSuccess = {
                                            mqttManager?.unsubscribeFromTopic(variable.topic)
                                            currentVariables = currentVariables.filter { it.id != variable.id }
                                        },
                                        onFailure = { e ->
                                            android.util.Log.e("SENSORES", "Error eliminando: ${e.message}")
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo confirmar salida
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
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    //  Diálogo añadir sensor — ahora guarda en DynamoDB
    if (showAddDialog) {
        var newVariableName by remember { mutableStateOf("") }
        var newVariableTopic by remember { mutableStateOf("") }
        var newVariableUnit by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }
        var saveError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddDialog = false },
            title = { Text("Nueva Variable de Sensor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Escribe el tópico MQTT exacto que configuraste en tu ESP32.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = newVariableName,
                        onValueChange = { newVariableName = it; saveError = null },
                        label = { Text("Nombre") },
                        placeholder = { Text("Ej: Distancia") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )
                    OutlinedTextField(
                        value = newVariableTopic,
                        onValueChange = { newVariableTopic = it; saveError = null },
                        label = { Text("Tópico MQTT") },
                        placeholder = { Text("Ej: taller/distancia") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )
                    OutlinedTextField(
                        value = newVariableUnit,
                        onValueChange = { newVariableUnit = it },
                        label = { Text("Unidad (opcional)") },
                        placeholder = { Text("Ej: cm, °C, V") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )
                    if (saveError != null) {
                        Text(saveError ?: "", color = Color(0xFFE53935), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newVariableName.isBlank() || newVariableTopic.isBlank()) {
                            saveError = "Nombre y tópico son obligatorios"
                            return@Button
                        }
                        isSaving = true
                        scope.launch {
                            val result = sensoresRepository.saveSensor(
                                nombre = newVariableName,
                                topico = newVariableTopic,
                                unidad = newVariableUnit
                            )
                            result.fold(
                                onSuccess = { remoteSensor ->
                                    // Agregar a la lista local
                                    val newVar = SensorVariable(
                                        id = remoteSensor.sensorId,
                                        name = remoteSensor.nombre,
                                        value = "--",
                                        unit = remoteSensor.unidad,
                                        topic = remoteSensor.topico
                                    )
                                    currentVariables = currentVariables + newVar

                                    // Suscribirse al nuevo tópico sin reconectar
                                    mqttManager?.subscribeToTopic(remoteSensor.topico)

                                    showAddDialog = false
                                },
                                onFailure = { e ->
                                    saveError = "Error al guardar: ${e.message}"
                                    isSaving = false
                                }
                            )
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Agregar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// SensorCard con botón eliminar
@Composable
fun SensorCard(
    label: String,
    value: String,
    unit: String,
    onDelete: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontSize = 14.sp, color = Color.Gray)
                // Botón eliminar sensor
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    unit,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                )
            }
        }
    }
}