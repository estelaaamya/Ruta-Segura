package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Categorías oficiales solicitadas para la aplicación Ruta Segura.
 * CUIDADO: Mantener los nombres exactos para cumplir los criterios de aceptación.
 */
enum class RiskCategory(
    val displayName: String,
    val descriptionText: String,
    val icon: ImageVector,
    val color: Color
) {
    POSTE_SIN_LUZ(
        displayName = "Poste sin luz",
        descriptionText = "Zonas oscuras o luminarias apagadas",
        icon = Icons.Default.Lightbulb,
        color = Color(0xFFF59E0B) // Ámbar brillante
    ),
    PERRO_SUELTO(
        displayName = "Perro suelto",
        descriptionText = "Animales agresivos o sin correa",
        icon = Icons.Default.Pets,
        color = Color(0xFFEF4444) // Rojo alerta
    ),
    ZANJA(
        displayName = "Zanja",
        descriptionText = "Obras abiertas o baches peligrosos",
        icon = Icons.Default.Dangerous,
        color = Color(0xFF8B5CF6) // Púrpura alerta
    ),
    TRAMO_SOLITARIO(
        displayName = "Tramo solitario",
        descriptionText = "Callejón o calle con poco paso de gente",
        icon = Icons.Default.Warning,
        color = Color(0xFF0EA5E9) // Azul advertencia
    );

    companion object {
        fun fromDisplayName(name: String): RiskCategory {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
                ?: POSTE_SIN_LUZ
        }
    }
}

/**
 * Franjas horarias del día requeridas para reportes y filtros.
 */
enum class TimeOfDay(
    val displayName: String,
    val hint: String,
    val icon: ImageVector
) {
    MANANA(
        displayName = "Mañana",
        hint = "06:00 - 12:00",
        icon = Icons.Default.WbSunny
    ),
    TARDE(
        displayName = "Tarde",
        hint = "12:00 - 19:00",
        icon = Icons.Default.WbTwilight
    ),
    NOCHE(
        displayName = "Noche",
        hint = "19:00 - 06:00",
        icon = Icons.Default.Nightlight
    );

    companion object {
        fun fromDisplayName(name: String): TimeOfDay {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
                ?: NOCHE
        }
    }
}

/**
 * Filtro de la lista: incluye la opción de ver "Todas" o filtrar por turno específico.
 */
enum class FilterTime(val label: String) {
    TODAS("Todas"),
    MANANA("Mañana"),
    TARDE("Tarde"),
    NOCHE("Noche")
}
