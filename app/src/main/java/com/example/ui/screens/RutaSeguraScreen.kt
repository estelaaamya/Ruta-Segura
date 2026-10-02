package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FilterTime
import com.example.data.RiskCategory
import com.example.data.RiskPointEntity
import com.example.data.TimeOfDay
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SafetyBluePrimary
import com.example.ui.viewmodel.RutaViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Pantalla principal de RUTA SEGURA.
 * Integra las 3 funciones exigidas:
 * 1. Marcar punto (Categoría, Ubicación escrita, Hora del día)
 * 2. Ver lista de puntos filtrada por hora del día (Mañana, Tarde, Noche)
 * 3. Compartir reporte por enlace o por texto (copiar al portapapeles o menú nativo de compartir)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutaSeguraScreen(
    viewModel: RutaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados observados reactivamente desde el ViewModel
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val allPoints by viewModel.allPoints.collectAsStateWithLifecycle()
    val filteredPoints by viewModel.filteredPoints.collectAsStateWithLifecycle()
    val isFormVisible by viewModel.isFormVisible.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val descriptionInput by viewModel.descriptionInput.collectAsStateWithLifecycle()
    val selectedTimeOfDay by viewModel.selectedTimeOfDay.collectAsStateWithLifecycle()
    val formErrorMessage by viewModel.formErrorMessage.collectAsStateWithLifecycle()

    // Estado para el modal de compartir un punto específico
    var pointToShare by remember { mutableStateOf<RiskPointEntity?>(null) }
    var pointToDelete by remember { mutableStateOf<RiskPointEntity?>(null) }

    // Manejo de mensajes instantáneos en pantalla mediante Snackbar
    LaunchedEffect(Unit) {
        viewModel.userFeedback.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "RUTA SEGURA",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "Puntos de cuidado camino al instituto",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openForm() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Marcar punto", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_point_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ==========================================
            // SECCIÓN: Filtro horario (Mañana, Tarde, Noche)
            // ==========================================
            FilterSection(
                selectedFilter = selectedFilter,
                allPoints = allPoints,
                onFilterSelected = { viewModel.onFilterChanged(it) }
            )

            // ==========================================
            // SECCIÓN: Lista de puntos reportados
            // ==========================================
            if (filteredPoints.isEmpty()) {
                EmptyStateCard(
                    selectedFilter = selectedFilter,
                    onOpenForm = { viewModel.openForm() }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("reports_list")
                ) {
                    items(
                        items = filteredPoints,
                        key = { it.id }
                    ) { point ->
                        RiskPointCard(
                            point = point,
                            onShareClick = { pointToShare = point },
                            onDeleteClick = { pointToDelete = point },
                            onDirectShareApp = {
                                shareReportViaIntent(context, viewModel.generateShareText(point))
                            },
                            onCopyText = {
                                copyToClipboard(context, "Reporte Ruta Segura", viewModel.generateShareText(point))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("¡Texto del reporte copiado al portapapeles!")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ========================================================
    // MODAL BOTTOM SHEET: Formulario para marcar nuevo punto
    // ========================================================
    if (isFormVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { viewModel.closeForm() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("new_point_bottom_sheet")
        ) {
            // Manejador del botón atrás físico en Android para cerrar el formulario
            BackHandler {
                viewModel.closeForm()
            }

            NewRiskPointFormContent(
                selectedCategory = selectedCategory,
                descriptionInput = descriptionInput,
                selectedTimeOfDay = selectedTimeOfDay,
                errorMessage = formErrorMessage,
                onCategoryChange = { viewModel.onCategorySelected(it) },
                onDescriptionChange = { viewModel.onDescriptionChanged(it) },
                onTimeOfDayChange = { viewModel.onTimeOfDaySelected(it) },
                onSave = { viewModel.saveRiskPoint() },
                onCancel = { viewModel.closeForm() }
            )
        }
    }

    // ========================================================
    // DIÁLOGO DE COMPARTIR: Enlace o Texto
    // ========================================================
    pointToShare?.let { point ->
        SharePointDialog(
            point = point,
            onDismiss = { pointToShare = null },
            onShareViaSystem = {
                shareReportViaIntent(context, viewModel.generateShareText(point))
                pointToShare = null
            },
            onCopyText = {
                copyToClipboard(context, "Reporte Ruta Segura", viewModel.generateShareText(point))
                pointToShare = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("¡Texto copiado al portapapeles!")
                }
            },
            onCopyLink = {
                val link = viewModel.generateShareLink(point)
                copyToClipboard(context, "Enlace Ruta Segura", link)
                pointToShare = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("¡Enlace copiado al portapapeles!")
                }
            }
        )
    }

    // ========================================================
    // DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN
    // ========================================================
    pointToDelete?.let { point ->
        AlertDialog(
            onDismissRequest = { pointToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar este reporte?", fontWeight = FontWeight.Bold) },
            text = { Text("Se quitará el punto \"${point.category}\" en \"${point.description}\" de la lista de advertencias.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePoint(point.id)
                        pointToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pointToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Barra superior de filtros por hora del día con contador en tiempo real.
 */
@Composable
private fun FilterSection(
    selectedFilter: FilterTime,
    allPoints: List<RiskPointEntity>,
    onFilterSelected: (FilterTime) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Filtrar por horario de tránsito:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(FilterTime.entries.toTypedArray()) { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        FilterTime.TODAS -> allPoints.size
                        FilterTime.MANANA -> allPoints.count { it.timeOfDay.equals("Mañana", ignoreCase = true) }
                        FilterTime.TARDE -> allPoints.count { it.timeOfDay.equals("Tarde", ignoreCase = true) }
                        FilterTime.NOCHE -> allPoints.count { it.timeOfDay.equals("Noche", ignoreCase = true) }
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = filter.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta individual para mostrar un punto reportado.
 * Muestra categoría, descripción y hora, con botones directos para compartir o copiar.
 */
@Composable
private fun RiskPointCard(
    point: RiskPointEntity,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDirectShareApp: () -> Unit,
    onCopyText: () -> Unit
) {
    val categoryMeta = RiskCategory.fromDisplayName(point.category)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_card_${point.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Fila superior: Categoría con badge colorido + Hora del día
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge de categoría
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = categoryMeta.color.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, categoryMeta.color.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = categoryMeta.icon,
                            contentDescription = null,
                            tint = categoryMeta.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = point.category,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = categoryMeta.color
                            )
                        )
                    }
                }

                // Badge de Hora del día (Mañana, Tarde, Noche)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val timeIcon = when (point.timeOfDay.lowercase()) {
                            "mañana" -> Icons.Default.Schedule
                            "tarde" -> Icons.Default.Schedule
                            else -> Icons.Default.Schedule
                        }
                        Icon(
                            imageVector = timeIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = point.timeOfDay,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Descripción de la ubicación
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Ubicación:",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = point.description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            if (point.exactTime.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Hora de registro: ${point.exactTime}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.padding(start = 28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fila de acciones: Botones para compartir por app y copiar texto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Botón Menú Compartir celular
                    OutlinedButton(
                        onClick = onDirectShareApp,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("share_button_${point.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Compartir", style = MaterialTheme.typography.labelMedium)
                    }

                    // Botón Copiar texto directo
                    OutlinedButton(
                        onClick = onCopyText,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("copy_text_button_${point.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Copiar", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Menú de opciones adicionales / Eliminar
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar reporte",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * Contenido del formulario para marcar un punto nuevo.
 * Incluye selección de categoría, descripción escrita y selección de hora del día.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewRiskPointFormContent(
    selectedCategory: String,
    descriptionInput: String,
    selectedTimeOfDay: String,
    errorMessage: String?,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTimeOfDayChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Marcar punto de riesgo",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Alerta a otros estudiantes sobre lugares donde no conviene pasar solo.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // ------------------------------------------
        // 1. CATEGORÍA (Poste sin luz, perro suelto, zanja, tramo solitario)
        // ------------------------------------------
        Text(
            text = "1. Selecciona la categoría:",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RiskCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category.displayName
                val chipColor by animateColorAsState(
                    targetValue = if (isSelected) category.color else MaterialTheme.colorScheme.surfaceVariant,
                    label = "chipColor"
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) category.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) category.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .clickable { onCategoryChange(category.displayName) }
                        .testTag("category_${category.name.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = if (isSelected) category.color else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) category.color else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ------------------------------------------
        // 2. DESCRIPCIÓN ESCRITA DE LA UBICACIÓN
        // ------------------------------------------
        Text(
            text = "2. Descripción corta de la ubicación:",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = descriptionInput,
            onValueChange = onDescriptionChange,
            placeholder = { Text("Ej: esquina de la tienda, frente a la parada...") },
            isError = errorMessage != null,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(text = "Sé claro y específico para tus compañeros")
                    }
                    Text(text = "${descriptionInput.length}/150")
                }
            },
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("location_description_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ------------------------------------------
        // 3. HORA DEL DÍA (Mañana, Tarde, Noche)
        // ------------------------------------------
        Text(
            text = "3. Hora del día (cuándo es más riesgoso pasar):",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeOfDay.entries.forEach { timeSlot ->
                val isSelected = selectedTimeOfDay.equals(timeSlot.displayName, ignoreCase = true)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTimeOfDayChange(timeSlot.displayName) }
                        .testTag("time_${timeSlot.name.lowercase()}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = timeSlot.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeSlot.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = timeSlot.hint,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botones de acción del formulario
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("save_report_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar reporte", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Diálogo modal para compartir un reporte por enlace o por texto.
 */
@Composable
private fun SharePointDialog(
    point: RiskPointEntity,
    onDismiss: () -> Unit,
    onShareViaSystem: () -> Unit,
    onCopyText: () -> Unit,
    onCopyLink: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir reporte", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Avisa a tus compañeros para que tomen precauciones o busquen otra calle:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠️ ${point.category}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "📍 ${point.description}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "⏰ Horario: ${point.timeOfDay}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Opción 1: Menú compartir del celular
                Button(
                    onClick = onShareViaSystem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dialog_share_system")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartir por WhatsApp / Apps")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Opción 2: Copiar texto completo
                OutlinedButton(
                    onClick = onCopyText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dialog_copy_text")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar texto del reporte")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Opción 3: Copiar enlace directo
                OutlinedButton(
                    onClick = onCopyLink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dialog_copy_link")
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar enlace al reporte")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

/**
 * Vista de estado vacío cuando no hay puntos registrados en el filtro seleccionado.
 */
@Composable
private fun EmptyStateCard(
    selectedFilter: FilterTime,
    onOpenForm: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (selectedFilter == FilterTime.TODAS) {
                    "No hay puntos reportados aún"
                } else {
                    "No hay reportes para el turno de la ${selectedFilter.label.lowercase()}"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sé el primero en marcar un poste sin luz, perro suelto, zanja o tramo solitario para cuidar a tus compañeros.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onOpenForm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("empty_state_add_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Marcar primer punto")
            }
        }
    }
}

/**
 * Función utilitaria para invocar el menú nativo de compartir de Android.
 * CUIDADO:
 * - FLAG_ACTIVITY_NEW_TASK es obligatorio cuando el context no proviene directamente de una Activity
 *   para evitar un crash tipo AndroidRuntimeException.
 * - Intent.createChooser proporciona el diálogo oficial del sistema operativo con las apps instaladas.
 */
private fun shareReportViaIntent(context: Context, textToShare: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, textToShare)
        type = "text/plain"
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooserIntent = Intent.createChooser(sendIntent, "Compartir reporte de Ruta Segura").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se encontró ninguna aplicación para compartir", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Función utilitaria para copiar texto o enlace al portapapeles de Android.
 * CUIDADO:
 * - Usar ClipData.newPlainText asegura compatibilidad con cualquier versión de Android desde SDK 24 a 36.
 */
private fun copyToClipboard(context: Context, label: String, textToCopy: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, textToCopy)
    clipboard.setPrimaryClip(clip)
}
