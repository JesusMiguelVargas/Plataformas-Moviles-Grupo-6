package com.jesus.iot01.ui.scada

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jesus.iot01.R
import com.jesus.iot01.data.MqttManager
import com.jesus.iot01.data.SensoresRepository
import com.jesus.iot01.navigation.AppScreens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToInt

data class PlacedSensor(
    val id: String,
    val name: String,
    val value: String,
    val unit: String,
    val topic: String = "",  // campo topic agregado
    var offset: Offset
)

data class AvailableSensor(
    val id: String,
    val name: String,
    val unit: String,
    val topic: String  // campo topic agregado
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScadaEditorScreen(
    navController: NavController,
    viewModel: ScadaGenerationViewModel
) {
    val context = LocalContext.current
    val sensoresRepository = remember { SensoresRepository() }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val preloadedScada = viewModel.editingScadaId?.let { id ->
        viewModel.savedScadas.find { it.id == id }
    }

    val placedSensors = remember {
        mutableStateListOf<PlacedSensor>().apply {
            preloadedScada?.sensors?.forEach { s ->
                add(PlacedSensor(s.id, s.name, s.value, s.unit, "", Offset(s.offsetX, s.offsetY)))
            }
        }
    }

    val availableSensors = remember { mutableStateListOf<AvailableSensor>() }
    var isLoadingSensores by remember { mutableStateOf(true) }

    // Cargar sensores reales desde DynamoDB
    LaunchedEffect(Unit) {
        isLoadingSensores = true
        val result = sensoresRepository.getSensores()
        result.fold(
            onSuccess = { remoteSensores ->
                val disponibles = remoteSensores
                    .map { s -> AvailableSensor(s.sensorId, s.nombre, s.unidad, s.topico) }
                    .filter { available ->
                        placedSensors.none { placed -> placed.id == available.id }
                    }
                availableSensors.addAll(disponibles)
            },
            onFailure = { e ->
                android.util.Log.e("EDITOR", "❌ Error cargando sensores: ${e.message}")
            }
        )
        isLoadingSensores = false
    }

    //Referencia al MqttManager para desconectar al salir
    var mqttManagerRef by remember { mutableStateOf<MqttManager?>(null) }

    // MQTT en tiempo real con delay para no bloquear la UI
    LaunchedEffect(Unit) {
        delay(1500) // espera que la pantalla cargue completamente

        val caCert = context.resources.openRawResource(R.raw.aws_root_ca)
            .bufferedReader().use { it.readText() }
        val clientCert = context.resources.openRawResource(R.raw.aws_certificate)
            .bufferedReader().use { it.readText() }
        val privateKey = context.resources.openRawResource(R.raw.aws_private_key)
            .bufferedReader().use { it.readText() }

        val manager = MqttManager { topic, payload ->
            try {
                val json = JSONObject(payload)
                val newValue = when {
                    json.has("distancia")   -> String.format("%.1f", json.optDouble("distancia", 0.0))
                    json.has("estado")      -> json.optString("estado", "--")
                    json.has("valor")       -> json.optString("valor", "--")
                    json.has("temperatura") -> String.format("%.1f", json.optDouble("temperatura", 0.0))
                    json.has("humedad")     -> String.format("%.1f", json.optDouble("humedad", 0.0))
                    else -> payload
                }

                //  Actualizar sensor colocado que coincide con el tópico
                for (i in placedSensors.indices) {
                    if (placedSensors[i].topic == topic) {
                        placedSensors[i] = placedSensors[i].copy(value = newValue)
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("MQTT_EDITOR", "Error: ${e.message}")
            }
        }

        mqttManagerRef = manager

        withContext(Dispatchers.IO) {
            try {
                // Suscribirse a tópicos reales de los sensores
                val topicos = placedSensors.map { it.topic }
                    .plus(availableSensors.map { it.topic })
                    .filter { it.isNotBlank() }
                    .distinct()
                manager.connect(caCert, clientCert, privateKey, topicos)
                android.util.Log.d("MQTT_EDITOR", "✅ MQTT conectado en editor — tópicos: $topicos")
            } catch (e: Exception) {
                android.util.Log.e("MQTT_EDITOR", "❌ Error conectando: ${e.message}")
            }
        }
    }

    // Desconectar al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            mqttManagerRef?.disconnect()
            android.util.Log.d("MQTT_EDITOR", "MQTT desconectado del editor")
        }
    }

    var isPaletteExpanded by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf(preloadedScada?.title ?: "") }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val paletteWidth = screenWidth * 0.32f

    val animatedPaletteWidth by animateDpAsState(
        targetValue = if (isPaletteExpanded) paletteWidth else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "PaletteAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    end = if (isPaletteExpanded) paletteWidth else 0.dp,
                    top = 8.dp, start = 8.dp, bottom = 8.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E), RoundedCornerShape(12.dp))
            )

            val bitmap = viewModel.generatedBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Fondo SCADA",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            placedSensors.forEachIndexed { index, sensor ->
                var currentOffset by remember { mutableStateOf(sensor.offset) }
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(currentOffset.x.roundToInt(), currentOffset.y.roundToInt())
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                currentOffset += dragAmount
                                placedSensors[index] = sensor.copy(offset = currentOffset)
                            }
                        }
                ) {
                    SensorCard(
                        name = sensor.name,
                        value = sensor.value,
                        unit = sensor.unit
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showExitDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE53935), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Salir", tint = Color.White)
            }

            Button(
                onClick = { showSaveDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                enabled = !viewModel.isSaving
            ) {
                if (viewModel.isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Guardando...", color = Color.White, fontSize = 14.sp)
                } else {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Guardar",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (!isPaletteExpanded) {
            Button(
                onClick = { isPaletteExpanded = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(48.dp)
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1)),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Abrir sensores",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .width(animatedPaletteWidth)
                .fillMaxHeight()
                .align(Alignment.CenterEnd),
            color = Color(0xFF1E1E2E),
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
        ) {
            if (animatedPaletteWidth > 40.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isPaletteExpanded = false },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF5E35B1), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Cerrar panel",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Variables",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF3A3A5C))
                    Spacer(Modifier.height(8.dp))

                    if (isLoadingSensores) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = Color(0xFF5E35B1),
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Cargando...", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    } else if (availableSensors.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Todas las variables\nfueron colocadas",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        availableSensors.forEach { sensor ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2A2A3E)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            sensor.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        if (sensor.unit.isNotBlank()) {
                                            Text(
                                                sensor.unit,
                                                fontSize = 10.sp,
                                                color = Color(0xFF9E9EBF)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            // PlacedSensor ahora incluye el tópico real
                                            placedSensors.add(
                                                PlacedSensor(
                                                    id = sensor.id,
                                                    name = sensor.name,
                                                    value = "--",
                                                    unit = sensor.unit,
                                                    topic = sensor.topic,
                                                    offset = Offset(300f, 200f)
                                                )
                                            )
                                            // Suscribirse al tópico del nuevo sensor
                                            mqttManagerRef?.subscribeToTopic(sensor.topic)
                                            availableSensors.remove(sensor)
                                        },
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(Color(0xFF5E35B1), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.isSaving) showSaveDialog = false },
            containerColor = Color(0xFF1E1E2E),
            title = {
                Text("Guardar SCADA", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    Text(
                        "Asigna un nombre a esta vista:",
                        fontSize = 13.sp,
                        color = Color(0xFF9E9EBF)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = saveTitle,
                        onValueChange = { saveTitle = it },
                        label = { Text("Nombre del SCADA", color = Color(0xFF9E9EBF)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isSaving,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C4DFF),
                            unfocusedBorderColor = Color(0xFF3A3A5C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF7C4DFF)
                        )
                    )
                    if (viewModel.awsErrorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            viewModel.awsErrorMessage ?: "",
                            color = Color(0xFFEF5350),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bitmap = viewModel.generatedBitmap
                        if (bitmap != null && saveTitle.isNotBlank()) {
                            viewModel.saveScada(
                                title = saveTitle,
                                currentBitmap = bitmap,
                                placedSensors = placedSensors.toList(),
                                onSuccess = {
                                    showSaveDialog = false
                                    navController.navigate(AppScreens.ScadaList.route) {
                                        popUpTo(AppScreens.ScadaList.route) { inclusive = true }
                                    }
                                },
                                onError = { _ -> }
                            )
                        }
                    },
                    enabled = saveTitle.isNotBlank() && !viewModel.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    if (viewModel.isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveDialog = false },
                    enabled = !viewModel.isSaving
                ) {
                    Text("Cancelar", color = Color(0xFF9E9EBF))
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = Color(0xFF1E1E2E),
            title = {
                Text("¿Cerrar Editor?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Los cambios no guardados se perderán.", color = Color(0xFF9E9EBF))
            },
            confirmButton = {
                TextButton(onClick = {
                    navController.navigate(AppScreens.ScadaList.route) {
                        popUpTo(AppScreens.ScadaList.route) { inclusive = true }
                    }
                }) {
                    Text("Abandonar", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancelar", color = Color(0xFF9E9EBF))
                }
            }
        )
    }
}

@Composable
fun SensorCard(name: String, value: String, unit: String) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(64.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF607D8B),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE0E0E0))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A237E)
                )
                if (unit.isNotBlank()) {
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = unit,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5C6BC0),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}