package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.RutaSeguraScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RutaViewModel

/**
 * Actividad principal de la aplicación RUTA SEGURA.
 * Inicializa el ViewModel con inyección de fábrica para asegurar la persistencia local de Room.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: RutaViewModel by viewModels {
        RutaViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Soporte de borde a borde moderno según estándares de Material Design 3
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RutaSeguraScreen(viewModel = viewModel)
                }
            }
        }
    }
}
