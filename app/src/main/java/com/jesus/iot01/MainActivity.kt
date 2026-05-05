package com.jesus.iot01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jesus.iot01.navigation.AppNavigation // Importa tu sistema de navegación
import com.jesus.iot01.ui.theme.Iot01Theme // Tu tema (verifica que el nombre sea exacto)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Iot01Theme {
                // AQUÍ ESTÁ LA MAGIA: Llamamos al navegador, no a una pantalla suelta
                AppNavigation()
            }
        }
    }
}
