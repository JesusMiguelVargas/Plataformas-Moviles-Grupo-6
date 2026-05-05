package com.jesus.iot01.ui.scada

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.jesus.iot01.navigation.AppScreens
import kotlin.math.roundToInt

// --- MODELOS DE DATOS ---
data class PlacedSensor(
    val id: String,
    val name: String,
    val value: String,
    val unit: String,
    val offset: Offset
)

data class AvailableSensor(
    val id: String,
    val name: String,
    val unit: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScadaEditorScreen(
    navController: NavController,
    viewModel: ScadaGenerationViewModel // Recibe el ViewModel compartido desde AppNavigation
) {
    val context = LocalContext.current

    // --- 1. FORZAR ORIENTACIÓN HORIZONTAL ---
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            // Al salir de esta pantalla, devolvemos el control de orientación al sistema
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // --- 2. ESTADOS DEL EDITOR ---
    val placedSensors = remember { mutableStateListOf<PlacedSensor>() }
    val availableSensors = remember {
        mutableStateListOf(
            AvailableSensor("1", "Voltaje Batería", "V"),
            AvailableSensor("2", "Estado Motor", ""),
            AvailableSensor("3", "Temperatura Central", "°C"),
            AvailableSensor("4", "Presión Sistema", "Bar")
        )
    }

    var isPaletteExpanded by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val paletteWidth = screenWidth / 3.5f // Ajustamos el ancho del panel lateral
    val animatedPaletteWidth by animateDpAsState(targetValue = if (isPaletteExpanded) paletteWidth else 0.dp)

    // --- 3. UI PRINCIPAL ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)) // Fondo ultra oscuro para resaltar el SCADA
    ) {
        // --- LIENZO SCADA ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // --- FONDO GENERADO POR IA (DALL-E 3) ---
            if (viewModel.generatedImageUrl != null) {
                AsyncImage(
                    model = viewModel.generatedImageUrl,
                    contentDescription = "Fondo Industrial Generado",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds // Estira la imagen para cubrir el lienzo horizontal
                )
            } else {
                // Estado de espera por si la URL tarda en propagarse
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF673AB7))
                    Spacer(Modifier.height(8.dp))
                    Text("Cargando lienzo...", color = Color.Gray, fontSize = 12.sp)
                }
            }

            // --- CAPA DE SENSORES ARRASTRABLES ---
            placedSensors.forEachIndexed { index, sensor ->
                var currentOffset by remember { mutableStateOf(sensor.offset) }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(currentOffset.x.roundToInt(), currentOffset.y.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                currentOffset += dragAmount
                                placedSensors[index] = sensor.copy(offset = currentOffset)
                            }
                        }
                        .background(
                            color = Color(0xDD2196F3), // Azul semi-transparente
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sensor.name, fontSize = 8.sp, color = Color.White.copy(alpha = 0.8f))
                        Text("${sensor.value} ${sensor.unit}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // --- BOTONES FLOTANTES DE CONTROL ---

        // Salir (Esquina Superior Izquierda)
        IconButton(
            onClick = { showExitDialog = true },
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .background(Color(0xFFE53935), CircleShape)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(20.dp))
        }

        // Guardar (Esquina Superior Derecha)
        Button(
            onClick = { navController.navigate(AppScreens.ScadaList.route) },
            modifier = Modifier
                .padding(16.dp)
                .height(40.dp)
                .align(Alignment.TopEnd),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.buttonElevation(4.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Guardar Vista", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // --- MANIJA DEL PANEL LATERAL ---
        if (!isPaletteExpanded) {
            Surface(
                modifier = Modifier
                    .width(40.dp)
                    .height(90.dp)
                    .align(Alignment.CenterEnd)
                    .clickable { isPaletteExpanded = true },
                color = Color(0xFF673AB7),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                shadowElevation = 4.dp
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Abrir Sensores", tint = Color.White, modifier = Modifier.padding(8.dp))
            }
        }

        // --- PANEL DE SENSORES (DRAWER) ---
        Surface(
            modifier = Modifier
                .width(animatedPaletteWidth)
                .fillMaxHeight()
                .align(Alignment.CenterEnd),
            color = Color.White,
            tonalElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
        ) {
            if (isPaletteExpanded) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sensores", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF333333))
                        IconButton(onClick = { isPaletteExpanded = false }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Cerrar", tint = Color.Gray)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                    // Lista de sensores disponibles
                    availableSensors.forEach { sensor ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sensor.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Unidad: ${sensor.unit}", fontSize = 10.sp, color = Color.Gray)
                                }
                                IconButton(
                                    onClick = {
                                        // Añadimos el sensor al centro del lienzo
                                        placedSensors.add(
                                            PlacedSensor(sensor.id, sensor.name, "--", sensor.unit, Offset(400f, 250f))
                                        )
                                        availableSensors.remove(sensor)
                                    },
                                    modifier = Modifier.size(32.dp).background(Color(0xFF673AB7), CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGO DE CONFIRMACIÓN AL SALIR ---
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Cerrar Editor?", fontWeight = FontWeight.Bold) },
            text = { Text("Si sales ahora no se guardará la posición de tus sensores.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        navController.navigate(AppScreens.ScadaList.route)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Abandonar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continuar editando")
                }
            }
        )
    }
}
// HOLA MUNDO