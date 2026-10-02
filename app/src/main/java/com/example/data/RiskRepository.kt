package com.example.data

import kotlinx.coroutines.flow.Flow

/**
 * Repositorio que abstrae la capa de datos Room para el resto de la app.
 * CUIDADO:
 * - Toda interacción con la base de datos debe pasar por este repositorio
 *   para mantener separación limpia entre la UI y la persistencia SQLite.
 */
class RiskRepository(private val dao: RiskPointDao) {

    val allPoints: Flow<List<RiskPointEntity>> = dao.getAllPoints()

    suspend fun insertPoint(category: String, description: String, timeOfDay: String, exactTime: String): Long {
        val entity = RiskPointEntity(
            category = category,
            description = description.trim(),
            timeOfDay = timeOfDay,
            exactTime = exactTime
        )
        return dao.insertPoint(entity)
    }

    suspend fun deletePointById(id: Long) {
        dao.deletePointById(id)
    }

    suspend fun countPoints(): Int {
        return dao.countPoints()
    }
}
