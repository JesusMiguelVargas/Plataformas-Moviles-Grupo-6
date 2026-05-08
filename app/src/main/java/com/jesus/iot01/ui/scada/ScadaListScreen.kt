package com.jesus.iot01.ui.scada

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jesus.iot01.navigation.AppScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScadaListScreen(
    navController: NavController,
    viewModel: ScadaGenerationViewModel
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadScadasFromAws()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Vistas SCADA", fontWeight = FontWeight.Bold) },
                actions = {
                    //  Botón recargar
                    IconButton(
                        onClick = { viewModel.loadScadasFromAws() },
                        enabled = !viewModel.isLoadingScadas
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Recargar",
                            tint = Color(0xFF673AB7)
                        )
                    }
                    //  Botón salir
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
                    onClick = { }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    viewModel.resetForNewScada()
                    navController.navigate(AppScreens.ScadaPrompt.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Crear Nueva Vista con IA")
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vistas Guardadas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (viewModel.savedScadas.isNotEmpty()) {
                    Text(
                        "${viewModel.savedScadas.size} vista(s)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                viewModel.isLoadingScadas -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF673AB7))
                            Spacer(Modifier.height(16.dp))
                            Text("Cargando tus vistas SCADA...", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
                viewModel.awsErrorMessage != null && viewModel.savedScadas.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚠️", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No se pudieron cargar las vistas",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                viewModel.awsErrorMessage ?: "",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { viewModel.loadScadasFromAws() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                viewModel.savedScadas.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📋", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Aún no tienes vistas guardadas", color = Color.Gray, fontSize = 16.sp)
                            Text("Crea tu primera vista con IA", color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(viewModel.savedScadas) { scada ->
                            ScadaCard(
                                title = scada.title,
                                thumbnail = {
                                    Image(
                                        bitmap = scada.bitmap.asImageBitmap(),
                                        contentDescription = "Miniatura SCADA",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                },
                                onClick = {
                                    viewModel.loadScadaForEditing(scada.id)
                                    navController.navigate(AppScreens.ScadaEditor.route)
                                }
                            )
                        }
                    }
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
}

@Composable
fun ScadaCard(
    title: String,
    thumbnail: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                thumbnail()
            }
            Text(
                text = title,
                modifier = Modifier.padding(8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}