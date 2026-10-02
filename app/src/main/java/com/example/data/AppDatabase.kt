package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos SQLite local utilizando Room.
 * CUIDADO:
 * - exportSchema = false evita advertencias de compilación si no se define una carpeta de esquemas.
 * - Usamos el patrón Singleton de doble verificación para garantizar que solo exista una instancia
 *   abierta de SQLite en toda la aplicación.
 */
@Database(
    entities = [RiskPointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun riskPointDao(): RiskPointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ruta_segura_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
