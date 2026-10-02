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
 * Valida:
 * 1. Criterio de aceptación principal: marcar "Poste sin luz", "esquina de la tienda", "Noche".
 * 2. Persistencia y lectura de datos locales sin perder información.
 * 3. Filtrado por horario (Mañana, Tarde, Noche).
 * 4. Compartir por texto y enlace.
 * 5. Carga de dato de ejemplo, exportación JSON y borrado.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RutaSeguraTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RiskRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
        val id = repository.insertPoint(
            category = "Poste sin luz",
            description = "esquina de la tienda",
            timeOfDay = "Noche",
            exactTime = "21:15"
        )
        assertTrue(id > 0)

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

    @Test
    fun `dato de ejemplo inicial precargado y borrado completo`() = runTest {
        // Verificar que seedDefaultIfEmpty carga el dato de prueba
        repository.seedDefaultIfEmpty()
        var points = repository.allPoints.first()
        assertEquals(1, points.size)
        assertEquals("Poste sin luz", points.first().category)
        assertEquals("esquina de la tienda", points.first().description)
        assertEquals("Noche", points.first().timeOfDay)

        // Verificar borrado
        repository.deleteAllPoints()
        points = repository.allPoints.first()
        assertEquals(0, points.size)
    }

    @Test
    fun `exportar a JSON genera contenido valido con campos`() = runTest {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        repository.insertPoint("Zanja", "frente al porton", "Tarde", "14:20")
        val viewModel = RutaViewModel(app, repository)
        val points = repository.allPoints.first()

        val json = viewModel.exportToJson(points)
        assertTrue(json.contains("categoria"))
        assertTrue(json.contains("Zanja"))
        assertTrue(json.contains("frente al porton"))
        assertTrue(json.contains("Tarde"))
    }
}
