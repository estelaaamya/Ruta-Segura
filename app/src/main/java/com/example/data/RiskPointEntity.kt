package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de persistencia local SQLite/Room para almacenar cada punto de riesgo.
 * CUIDADO:
 * - autoGenerate = true permite que cada reporte tenga un ID único sin colisiones.
 * - Guardamos category y timeOfDay como Strings legibles para facilitar compatibilidad
 *   y evitar errores de migración si se agregan más categorías después.
 */
@Entity(tableName = "risk_points")
data class RiskPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,       // Ej: "Poste sin luz", "Perro suelto", "Zanja", "Tramo solitario"
    val description: String,    // Ej: "esquina de la tienda"
    val timeOfDay: String,      // Ej: "Mañana", "Tarde", "Noche"
    val exactTime: String = "", // Ej: "20:30" u hora registrada
    val createdAt: Long = System.currentTimeMillis()
)
