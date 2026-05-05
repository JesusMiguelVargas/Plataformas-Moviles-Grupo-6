package com.jesus.iot01.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Importamos nuestras pantallas
import com.jesus.iot01.ui.auth.LoginScreen
import com.jesus.iot01.ui.auth.RegisterScreen
import com.jesus.iot01.ui.onboarding.ConnectionCheckScreen
import com.jesus.iot01.ui.onboarding.CredentialsScreen
import com.jesus.iot01.ui.dashboard.VariablesScreen
import com.jesus.iot01.ui.scada.ScadaEditorScreen
import com.jesus.iot01.ui.scada.ScadaListScreen
import com.jesus.iot01.ui.scada.ScadaPromptScreen
import com.jesus.iot01.ui.scada.ScadaGenerationViewModel // Importante: Importar el ViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // --- INSTANCIA COMPARTIDA ---
    // Al crear el ViewModel aquí, actuará como un "cerebro compartido"
    // para todas las pantallas del SCADA.
    val scadaViewModel: ScadaGenerationViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = AppScreens.Login.route
    ) {
        composable(route = AppScreens.Login.route) {
            LoginScreen(navController)
        }

        composable(route = AppScreens.Register.route) {
            RegisterScreen(navController)
        }

        composable(route = AppScreens.Credentials.route) {
            CredentialsScreen(navController)
        }

        composable(route = AppScreens.ConnectionCheck.route) {
            ConnectionCheckScreen(navController)
        }

        composable(route = AppScreens.Variables.route) {
            VariablesScreen(navController)
        }

        // --- PANTALLAS DE SCADA (PASANDO EL VIEWMODEL) ---

        composable(route = AppScreens.ScadaPrompt.route) {
            // Pasamos scadaViewModel para que pueda INICIAR la generación
            ScadaPromptScreen(navController, scadaViewModel)
        }

        composable(route = AppScreens.ScadaList.route) {
            ScadaListScreen(navController)
        }

        composable(route = AppScreens.ScadaEditor.route) {
            // Pasamos el MISMO scadaViewModel para que pueda LEER la imagen generada
            ScadaEditorScreen(navController, scadaViewModel)
        }
    }
}