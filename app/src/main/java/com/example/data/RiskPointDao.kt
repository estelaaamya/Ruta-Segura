package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para interactuar con la tabla de puntos de riesgo.
 * CUIDADO:
 * - Se retorna Flow<List<...>> para que la UI de Compose reaccione automáticamente
 *   en tiempo real cada vez que un usuario añade o elimina un reporte.
 * - Ordenamos por createdAt DESC para que los reportes más recientes aparezcan primero.
 */
@Dao
interface RiskPointDao {

    @Query("SELECT * FROM risk_points ORDER BY createdAt DESC")
    fun getAllPoints(): Flow<List<RiskPointEntity>>

    @Query("SELECT * FROM risk_points WHERE timeOfDay = :timeOfDay ORDER BY createdAt DESC")
    fun getPointsByTimeOfDay(timeOfDay: String): Flow<List<RiskPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: RiskPointEntity): Long

    @Delete
    suspend fun deletePoint(point: RiskPointEntity)

    @Query("DELETE FROM risk_points WHERE id = :id")
    suspend fun deletePointById(id: Long)

    @Query("SELECT COUNT(*) FROM risk_points")
    suspend fun countPoints(): Int

    @Query("DELETE FROM risk_points")
    suspend fun deleteAllPoints()
}
