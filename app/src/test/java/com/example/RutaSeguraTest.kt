package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.FilterTime
import com.example.data.RiskCategory
import com.example.data.RiskPointEntity
import com.example.data.RiskRepository
import com.example.ui.viewmodel.RutaViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pruebas unitarias y de integración para RUTA SEGURA.
 * Valida el criterio de aceptación principal:
 * "marco un punto con categoría 'poste sin luz', descripción 'esquina de la tienda'
 * y hora de la noche, y lo veo en la lista con su categoría y su hora".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RutaSeguraTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RiskRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Usamos base de datos en memoria para pruebas rápidas y aisladas
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RiskRepository(database.riskPointDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `criterio de aceptacion - insertar punto poste sin luz de noche y verificar en lista`() = runTest {
        // 1. Guardar punto con los datos exactos del criterio de aceptación
        val id = repository.insertPoint(
            category = "Poste sin luz",
            description = "esquina de la tienda",
            timeOfDay = "Noche",
            exactTime = "21:15"
        )
        assertTrue(id > 0)

        // 2. Obtener lista desde el repositorio (Room Flow)
        val points = repository.allPoints.first()
        assertEquals(1, points.size)

        val report = points.first()
        assertEquals("Poste sin luz", report.category)
        assertEquals("esquina de la tienda", report.description)
        assertEquals("Noche", report.timeOfDay)
    }

    @Test
    fun `filtrado por hora del dia - verificar manana, tarde y noche`() = runTest {
        repository.insertPoint("Poste sin luz", "esquina de la tienda", "Noche", "21:15")
        repository.insertPoint("Perro suelto", "parque central", "Mañana", "08:30")
        repository.insertPoint("Zanja", "callejon 3", "Tarde", "15:00")

        val all = repository.allPoints.first()
        assertEquals(3, all.size)

        val morningList = all.filter { it.timeOfDay.equals("Mañana", ignoreCase = true) }
        val eveningList = all.filter { it.timeOfDay.equals("Tarde", ignoreCase = true) }
        val nightList = all.filter { it.timeOfDay.equals("Noche", ignoreCase = true) }

        assertEquals(1, morningList.size)
        assertEquals("Perro suelto", morningList.first().category)

        assertEquals(1, eveningList.size)
        assertEquals("Zanja", eveningList.first().category)

        assertEquals(1, nightList.size)
        assertEquals("Poste sin luz", nightList.first().category)
    }

    @Test
    fun `compartir reporte genera texto con datos completos`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RutaViewModel(app, repository)

        val point = RiskPointEntity(
            id = 42,
            category = "Poste sin luz",
            description = "esquina de la tienda",
            timeOfDay = "Noche",
            exactTime = "22:00"
        )

        val shareText = viewModel.generateShareText(point)
        assertTrue(shareText.contains("Poste sin luz"))
        assertTrue(shareText.contains("esquina de la tienda"))
        assertTrue(shareText.contains("Noche"))
        assertTrue(shareText.contains("https://rutasegura.app"))

        val shareLink = viewModel.generateShareLink(point)
        assertTrue(shareLink.contains("https://rutasegura.app/punto"))
        assertTrue(shareLink.contains("id=42"))
    }
}
