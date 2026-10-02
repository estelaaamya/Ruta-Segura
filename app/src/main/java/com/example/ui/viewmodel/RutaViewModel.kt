package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FilterTime
import com.example.data.RiskCategory
import com.example.data.RiskPointEntity
import com.example.data.RiskRepository
import com.example.data.TimeOfDay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
 */
class RutaViewModel(
    application: Application,
    private val repository: RiskRepository
) : AndroidViewModel(application) {

    // Filtro horario actualmente seleccionado (Todas, Mañana, Tarde, Noche)
    private val _selectedFilter = MutableStateFlow(FilterTime.TODAS)
    val selectedFilter: StateFlow<FilterTime> = _selectedFilter.asStateFlow()

    // Lista completa de puntos leída de Room
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

    // Mensajes para el Snackbar (retroalimentación instantánea)
    private val _userFeedback = MutableSharedFlow<String>()
    val userFeedback: SharedFlow<String> = _userFeedback.asSharedFlow()

    fun onFilterChanged(newFilter: FilterTime) {
        _selectedFilter.value = newFilter
    }

    fun openForm() {
        // Al abrir el formulario, sugerimos la franja horaria según el momento del día actual
        val defaultTime = determineCurrentTimeOfDay()
        _selectedTimeOfDay.value = defaultTime
        _formErrorMessage.value = null
        _isFormVisible.value = true
    }

    fun closeForm() {
        _isFormVisible.value = false
        _formErrorMessage.value = null
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onDescriptionChanged(newDescription: String) {
        // Limitamos la descripción para que sea concisa y no sature la tarjeta
        if (newDescription.length <= 150) {
            _descriptionInput.value = newDescription
            if (_formErrorMessage.value != null && newDescription.isNotBlank()) {
                _formErrorMessage.value = null
            }
        }
    }

    fun onTimeOfDaySelected(time: String) {
        _selectedTimeOfDay.value = time
    }

    /**
     * Guarda el nuevo punto en la base de datos Room.
     * CUIDADO:
     * - Validación estricta: evitar guardar cadenas en blanco o con solo espacios.
     * - Limpiar los campos solo después de confirmar inserción exitosa.
     */
    fun saveRiskPoint() {
        val description = _descriptionInput.value.trim()
        if (description.isBlank()) {
            _formErrorMessage.value = "Por favor escribe una descripción corta de la ubicación"
            return
        }

        val currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            try {
                repository.insertPoint(
                    category = _selectedCategory.value,
                    description = description,
                    timeOfDay = _selectedTimeOfDay.value,
                    exactTime = currentTimeString
                )
                // Restablecer el formulario
                _descriptionInput.value = ""
                _formErrorMessage.value = null
                _isFormVisible.value = false
                _userFeedback.emit("¡Punto de riesgo reportado con éxito!")
            } catch (e: Exception) {
                _formErrorMessage.value = "Error al guardar el punto: ${e.localizedMessage}"
            }
        }
    }

    fun deletePoint(id: Long) {
        viewModelScope.launch {
            try {
                repository.deletePointById(id)
                _userFeedback.emit("Reporte eliminado")
            } catch (e: Exception) {
                _userFeedback.emit("No se pudo eliminar el reporte")
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

    /**
     * Determina la hora del día aproximada actual para sugerir en el formulario.
     */
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
