package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.FilterTime
import com.example.data.RiskCategory
import com.example.data.RiskPointEntity
import com.example.data.RiskRepository
import com.example.data.TimeOfDay
import com.example.data.gemini.AnalisisRutaResponse
import com.example.data.gemini.GeminiClient
import com.example.data.gemini.GeminiResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel principal para Ruta Segura.
 * CUIDADO:
 * - Heredar de AndroidViewModel nos da acceso seguro a applicationContext
 *   sin generar memory leaks de Activities.
 * - Combinamos la lista completa de Room con el filtro seleccionado usando StateFlow
 *   para garantizar reactividad pura y prevenir inconsistencias en recomposiciones.
 * - En el bloque init se asegura la existencia de un dato de ejemplo ("Poste sin luz",
 *   "esquina de la tienda", "Noche") para pruebas inmediatas si la base está vacía.
 */
class RutaViewModel(
    application: Application,
    private val repository: RiskRepository
) : AndroidViewModel(application) {

    init {
        // Carga automática del dato de ejemplo al iniciar si la memoria está vacía
        viewModelScope.launch {
            repository.seedDefaultIfEmpty()
        }
    }

    // Filtro horario actualmente seleccionado (Todas, Mañana, Tarde, Noche)
    private val _selectedFilter = MutableStateFlow(FilterTime.TODAS)
    val selectedFilter: StateFlow<FilterTime> = _selectedFilter.asStateFlow()

    // Lista completa de puntos leída de Room (Persistencia SQLite en el teléfono)
    val allPoints: StateFlow<List<RiskPointEntity>> = repository.allPoints
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Lista filtrada en memoria según la hora del día seleccionada
    val filteredPoints: StateFlow<List<RiskPointEntity>> = combine(allPoints, _selectedFilter) { list, filter ->
        when (filter) {
            FilterTime.TODAS -> list
            FilterTime.MANANA -> list.filter { it.timeOfDay.equals("Mañana", ignoreCase = true) }
            FilterTime.TARDE -> list.filter { it.timeOfDay.equals("Tarde", ignoreCase = true) }
            FilterTime.NOCHE -> list.filter { it.timeOfDay.equals("Noche", ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Estados del formulario para marcar nuevo punto
    private val _isFormVisible = MutableStateFlow(false)
    val isFormVisible: StateFlow<Boolean> = _isFormVisible.asStateFlow()

    private val _selectedCategory = MutableStateFlow(RiskCategory.POSTE_SIN_LUZ.displayName)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _descriptionInput = MutableStateFlow("")
    val descriptionInput: StateFlow<String> = _descriptionInput.asStateFlow()

    private val _selectedTimeOfDay = MutableStateFlow(TimeOfDay.NOCHE.displayName)
    val selectedTimeOfDay: StateFlow<String> = _selectedTimeOfDay.asStateFlow()

    private val _formErrorMessage = MutableStateFlow<String?>(null)
    val formErrorMessage: StateFlow<String?> = _formErrorMessage.asStateFlow()

    // Guardia contra pulsaciones múltiples (doble toque)
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // Mensajes para el Snackbar (retroalimentación instantánea)
    private val _userFeedback = MutableSharedFlow<String>()
    val userFeedback: SharedFlow<String> = _userFeedback.asSharedFlow()

    // Estado del análisis de IA con Gemini
    private val _geminiAnalysisState = MutableStateFlow<GeminiResult>(GeminiResult.Idle)
    val geminiAnalysisState: StateFlow<GeminiResult> = _geminiAnalysisState.asStateFlow()

    private val _isAiPanelExpanded = MutableStateFlow(false)
    val isAiPanelExpanded: StateFlow<Boolean> = _isAiPanelExpanded.asStateFlow()

    fun toggleAiPanel() {
        _isAiPanelExpanded.value = !_isAiPanelExpanded.value
    }

    fun requestAiRouteAnalysis() {
        viewModelScope.launch {
            _geminiAnalysisState.value = GeminiResult.Loading
            _isAiPanelExpanded.value = true
            val currentPoints = repository.allPoints.first()
            val result = GeminiClient.analyzeRoutes(
                apiKey = BuildConfig.GEMINI_API_KEY,
                points = currentPoints
            )
            _geminiAnalysisState.value = result
            when (result) {
                is GeminiResult.Success -> {
                    _userFeedback.emit("¡Ruta analizada con éxito por la IA!")
                }
                is GeminiResult.Error -> {
                    _userFeedback.emit(result.userFriendlyMessage)
                }
                else -> Unit
            }
        }
    }

    fun loadMockAiAnalysis() {
        _isAiPanelExpanded.value = true
        _geminiAnalysisState.value = GeminiResult.Success(
            data = GeminiClient.SAMPLE_MOCK_ANALISIS,
            isMock = true
        )
        viewModelScope.launch {
            _userFeedback.emit("Análisis de prueba cargado (sin gastar llamadas de API).")
        }
    }

    fun clearAiAnalysis() {
        _geminiAnalysisState.value = GeminiResult.Idle
    }

    fun onFilterChanged(newFilter: FilterTime) {
        _selectedFilter.value = newFilter
    }

    fun openForm() {
        val defaultTime = determineCurrentTimeOfDay()
        _selectedTimeOfDay.value = defaultTime
        _formErrorMessage.value = null
        _isSaving.value = false
        _isFormVisible.value = true
    }

    fun closeForm() {
        if (!_isSaving.value) {
            _isFormVisible.value = false
            _formErrorMessage.value = null
        }
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onDescriptionChanged(newDescription: String) {
        // Acepta pegado masivo de más de 500 caracteres recortando automáticamente a 150
        _descriptionInput.value = newDescription.take(150)
        if (_formErrorMessage.value != null && newDescription.trim().length >= 3) {
            _formErrorMessage.value = null
        }
    }

    fun onTimeOfDaySelected(time: String) {
        _selectedTimeOfDay.value = time
    }

    /**
     * OPERACIÓN: GUARDAR CON VALIDACIONES DEFENSIVAS
     */
    fun saveRiskPoint() {
        // Evita doble toque o concurrencia
        if (_isSaving.value) return

        // Sanitización: colapsa saltos de línea repetidos y limpia bordes
        val cleanedDescription = _descriptionInput.value
            .replace(Regex("\\n{2,}"), "\n")
            .trim()

        if (cleanedDescription.isBlank()) {
            _formErrorMessage.value = "Por favor escribí la ubicación del peligro para avisar a tus compañeros."
            return
        }

        if (cleanedDescription.length < 3) {
            _formErrorMessage.value = "Por favor escribí al menos 3 letras para describir la ubicación."
            return
        }

        // Obtención de hora tolerante a fallos
        val currentTimeString = try {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        } catch (e: Exception) {
            "Hora no registrada"
        }

        _isSaving.value = true

        viewModelScope.launch {
            try {
                repository.insertPoint(
                    category = _selectedCategory.value,
                    description = cleanedDescription,
                    timeOfDay = _selectedTimeOfDay.value,
                    exactTime = currentTimeString
                )
                _descriptionInput.value = ""
                _formErrorMessage.value = null
                _isFormVisible.value = false
                _userFeedback.emit("¡Aviso guardado! Ya se muestra en la lista para tus compañeros.")
            } catch (e: Exception) {
                _formErrorMessage.value = "No se pudo guardar el aviso. Por favor intentá de nuevo."
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * OPERACIÓN: BORRAR INDIVIDUAL
     */
    fun deletePoint(id: Long) {
        viewModelScope.launch {
            try {
                repository.deletePointById(id)
                _userFeedback.emit("Aviso eliminado de la lista.")
            } catch (e: Exception) {
                _userFeedback.emit("No se pudo quitar el aviso. Intentá de nuevo.")
            }
        }
    }

    /**
     * OPERACIÓN: BORRAR TODO
     */
    fun clearAllPoints() {
        viewModelScope.launch {
            try {
                repository.deleteAllPoints()
                _userFeedback.emit("Se borraron todos los avisos de la lista.")
            } catch (e: Exception) {
                _userFeedback.emit("No pudimos borrar los avisos. Intentá de nuevo.")
            }
        }
    }

    /**
     * Restablece el dato de ejemplo para pruebas rápidas.
     */
    fun resetToSampleData() {
        viewModelScope.launch {
            try {
                repository.deleteAllPoints()
                repository.seedDefaultIfEmpty()
                _userFeedback.emit("Aviso de ejemplo cargado para que pruebes la app.")
            } catch (e: Exception) {
                _userFeedback.emit("No se pudo cargar el ejemplo.")
            }
        }
    }

    /**
     * OPERACIÓN: EXPORTAR A ARCHIVO / JSON
     * Genera la representación en formato JSON de todos los reportes almacenados.
     */
    fun exportToJson(points: List<RiskPointEntity> = allPoints.value): String {
        val jsonArray = JSONArray()
        for (point in points) {
            val obj = JSONObject().apply {
                put("id", point.id)
                put("categoria", point.category)
                put("descripcion", point.description)
                put("horario", point.timeOfDay)
                put("hora_registro", point.exactTime)
                put("timestamp", point.createdAt)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    /**
     * Guarda el archivo JSON en cacheDir y abre el diálogo del sistema operativo
     * para que el usuario pueda guardarlo en Google Drive, enviarlo por correo, WhatsApp o Descargas.
     */
    fun exportBackupFile(context: Context) {
        viewModelScope.launch {
            try {
                val currentPoints = repository.allPoints.first()
                val jsonString = exportToJson(currentPoints)
                val fileName = "respaldo_ruta_segura_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
                val file = File(context.cacheDir, fileName)
                file.writeText(jsonString, StandardCharsets.UTF_8)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Respaldo Ruta Segura")
                    putExtra(Intent.EXTRA_TEXT, "Archivo JSON con el respaldo de puntos de riesgo de Ruta Segura.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooser = Intent.createChooser(shareIntent, "Guardar o enviar archivo de respaldo").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                _userFeedback.emit("Archivo de respaldo preparado para compartir o guardar.")
            } catch (e: java.io.IOException) {
                _userFeedback.emit("Tu celular no tiene suficiente espacio libre para guardar el archivo.")
            } catch (e: Exception) {
                _userFeedback.emit("No se pudo crear el archivo de respaldo. Por favor intentá de nuevo.")
            }
        }
    }

    /**
     * Construye el texto completo formateado para compartir por WhatsApp, SMS u otras aplicaciones.
     */
    fun generateShareText(point: RiskPointEntity): String {
        return buildString {
            append("🚨 ALERTA - RUTA SEGURA\n")
            append("Punto de riesgo: ").append(point.category).append("\n")
            append("📍 Ubicación: ").append(point.description).append("\n")
            append("⏰ Horario reportado: ").append(point.timeOfDay)
            if (point.exactTime.isNotBlank()) {
                append(" (").append(point.exactTime).append(")")
            }
            append("\n")
            append("⚠️ Atención estudiantes que van caminando al instituto. ¡No pasen solos por aquí!\n")
            append("🔗 Enlace: ").append(generateShareLink(point))
        }
    }

    /**
     * Genera un enlace compartible representativo del reporte.
     */
    fun generateShareLink(point: RiskPointEntity): String {
        val encodedCat = try {
            URLEncoder.encode(point.category, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            "alerta"
        }
        val encodedDesc = try {
            URLEncoder.encode(point.description, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            "ubicacion"
        }
        return "https://rutasegura.app/punto?id=${point.id}&cat=$encodedCat&desc=$encodedDesc&h=${point.timeOfDay}"
    }

    private fun determineCurrentTimeOfDay(): String {
        val hour = SimpleDateFormat("H", Locale.getDefault()).format(Date()).toIntOrNull() ?: 20
        return when (hour) {
            in 6..11 -> TimeOfDay.MANANA.displayName
            in 12..18 -> TimeOfDay.TARDE.displayName
            else -> TimeOfDay.NOCHE.displayName
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val repository = RiskRepository(db.riskPointDao())
                    return RutaViewModel(application, repository) as T
                }
            }
    }
}
