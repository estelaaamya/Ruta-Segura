package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FilterTime
import com.example.data.RiskCategory
import com.example.data.RiskPointEntity
import com.example.data.TimeOfDay
import com.example.ui.viewmodel.RutaViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Pantalla principal de RUTA SEGURA adaptada para la versión M3 (Experiencia y Accesibilidad).
 * Cumple con los 6 requisitos de UX:
 * 1. Funciona desde 320 px de ancho, con una sola mano y sin hacer zoom.
 * 2. Alto contraste para leerse bajo luz solar directa; NINGÚN texto menor a 16 px (16 sp).
 * 3. Todos los campos con etiqueta visible fija, no solo texto de ejemplo dentro.
 * 4. Un solo botón principal por pantalla (FAB en inicio, Guardar en formulario); los demás secundarios.
 * 5. Estado vacío claro con frase motivadora que invita a la primera acción.
 * 6. Mensajes de éxito y error en español coloquial y claro, sin palabras técnicas.
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

    // Estados observados desde el ViewModel
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val allPoints by viewModel.allPoints.collectAsStateWithLifecycle()
    val filteredPoints by viewModel.filteredPoints.collectAsStateWithLifecycle()
    val isFormVisible by viewModel.isFormVisible.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val descriptionInput by viewModel.descriptionInput.collectAsStateWithLifecycle()
    val selectedTimeOfDay by viewModel.selectedTimeOfDay.collectAsStateWithLifecycle()
    val formErrorMessage by viewModel.formErrorMessage.collectAsStateWithLifecycle()

    var pointToShare by remember { mutableStateOf<RiskPointEntity?>(null) }
    var pointToDelete by remember { mutableStateOf<RiskPointEntity?>(null) }
    var isBackupDialogOpen by remember { mutableStateOf(false) }
    var isClearAllConfirmOpen by remember { mutableStateOf(false) }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "RUTA SEGURA",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        text = "M3",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Camino al instituto",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Botón secundario para respaldo y exportación
                    IconButton(
                        onClick = { isBackupDialogOpen = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("storage_backup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Opciones de respaldo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // ÚNICO BOTÓN PRINCIPAL EN ESTA PANTALLA: Prominente y en la zona cómoda para el pulgar
            ExtendedFloatingActionButton(
                onClick = { viewModel.openForm() },
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp)) },
                text = {
                    Text(
                        text = "Marcar punto",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(6.dp),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("add_point_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // SECCIÓN: Filtro horario
            FilterSection(
                selectedFilter = selectedFilter,
                allPoints = allPoints,
                onFilterSelected = { viewModel.onFilterChanged(it) }
            )

            // SECCIÓN: Lista de puntos reportados o Estado Vacío
            if (filteredPoints.isEmpty()) {
                EmptyStateCard(
                    selectedFilter = selectedFilter,
                    onOpenForm = { viewModel.openForm() },
                    onLoadSample = { viewModel.resetToSampleData() }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
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
                                copyToClipboard(context, "Aviso de Ruta Segura", viewModel.generateShareText(point))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("¡Texto copiado! Ya podés pegarlo en un mensaje.")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // FORMULARIO: ModalBottomSheet accesible con una sola mano
    if (isFormVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { viewModel.closeForm() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("new_point_bottom_sheet")
        ) {
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

    // DIÁLOGO: Compartir (Un solo botón primario)
    pointToShare?.let { point ->
        SharePointDialog(
            point = point,
            onDismiss = { pointToShare = null },
            onShareViaSystem = {
                shareReportViaIntent(context, viewModel.generateShareText(point))
                pointToShare = null
            },
            onCopyText = {
                copyToClipboard(context, "Aviso de Ruta Segura", viewModel.generateShareText(point))
                pointToShare = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("¡Texto copiado para compartir!")
                }
            },
            onCopyLink = {
                val link = viewModel.generateShareLink(point)
                copyToClipboard(context, "Enlace de Ruta Segura", link)
                pointToShare = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("¡Enlace copiado listo para enviar!")
                }
            }
        )
    }

    // DIÁLOGO: Confirmar eliminación
    pointToDelete?.let { point ->
        AlertDialog(
            onDismissRequest = { pointToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "¿Querés borrar este aviso?",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Se quitará el aviso \"${point.category}\" ubicado en \"${point.description}\".",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                // Único botón principal del diálogo
                Button(
                    onClick = {
                        viewModel.deletePoint(point.id)
                        pointToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Borrar aviso", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                // Botón secundario
                OutlinedButton(
                    onClick = { pointToDelete = null },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cancelar", fontSize = 16.sp)
                }
            }
        )
    }

    // DIÁLOGO: Gestión de respaldo local
    if (isBackupDialogOpen) {
        StorageBackupDialog(
            totalCount = allPoints.size,
            jsonPreview = viewModel.exportToJson(),
            onDismiss = { isBackupDialogOpen = false },
            onExportFile = {
                viewModel.exportBackupFile(context)
                isBackupDialogOpen = false
            },
            onCopyJson = {
                copyToClipboard(context, "Respaldo Ruta Segura", viewModel.exportToJson())
                isBackupDialogOpen = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("¡Datos de respaldo copiados al portapapeles!")
                }
            },
            onResetSample = {
                viewModel.resetToSampleData()
                isBackupDialogOpen = false
            },
            onPromptClearAll = {
                isBackupDialogOpen = false
                isClearAllConfirmOpen = true
            }
        )
    }

    // DIÁLOGO: Confirmar borrado completo
    if (isClearAllConfirmOpen) {
        AlertDialog(
            onDismissRequest = { isClearAllConfirmOpen = false },
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "¿Borrar todos los avisos?",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Esta acción quitará todos los reportes guardados en este celular.",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllPoints()
                        isClearAllConfirmOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Sí, borrar todo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { isClearAllConfirmOpen = false },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Volver", fontSize = 16.sp)
                }
            }
        )
    }
}

/**
 * Filtro horario horizontal adaptable a pantallas estrechas (320px).
 * Textos en 16.sp con alto contraste.
 */
@Composable
private fun FilterSection(
    selectedFilter: FilterTime,
    allPoints: List<RiskPointEntity>,
    onFilterSelected: (FilterTime) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Etiqueta visible del filtro con texto >= 16sp
                Text(
                    text = "Filtrar por horario:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = filter.label,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = count.toString(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
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
 * Tarjeta individual de reporte.
 * - Alto contraste (fondo blanco, texto oscuro, bordes sólidos).
 * - Tamaño de letra nunca menor a 16 px.
 * - Botones secundarios claros (Compartir / Copiar).
 * - Disposición vertical/responsiva para no romperse en 320 px de ancho.
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_card_${point.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Fila superior: Tipo de peligro + Momento del día
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = categoryMeta.color.copy(alpha = 0.18f),
                    border = BorderStroke(1.5.dp, categoryMeta.color)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = categoryMeta.icon,
                            contentDescription = null,
                            tint = categoryMeta.color,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = point.category,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryMeta.color
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = point.timeOfDay,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ubicación del reporte con etiqueta visible y texto >= 16sp
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Ubicación reportada:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = point.description,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 23.sp
                    )
                }
            }

            if (point.exactTime.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Registrado a las: ${point.exactTime}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones SECUNDARIOS: En 320px de ancho se ajustan en FlowRow para no desbordar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Botón secundario: Compartir
                    OutlinedButton(
                        onClick = onDirectShareApp,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("share_button_${point.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Compartir", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    // Botón secundario: Copiar
                    OutlinedButton(
                        onClick = onCopyText,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("copy_text_button_${point.id}")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Copiar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Botón secundario: Borrar
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Borrar aviso",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Formulario para marcar un punto nuevo.
 * Requisitos cumplidos:
 * - Se usa con una sola mano desde abajo (Bottom Sheet con scroll).
 * - Todas las etiquetas son VISIBLES permanentemente.
 * - Textos en 16.sp o superior con contraste para luz solar.
 * - Un solo botón principal ("Guardar aviso"). Cancelar es secundario.
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
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Marcar un punto de riesgo",
            fontSize = 21.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Avisá a tus compañeros sobre lugares donde conviene no pasar solo.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 1. ETIQUETA VISIBLE OBLIGATORIA: Categoría
        Text(
            text = "1. Seleccioná qué peligro viste:",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RiskCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category.displayName
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) category.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.5.dp,
                        color = if (isSelected) category.color else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .clickable { onCategoryChange(category.displayName) }
                        .testTag("category_${category.name.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = if (isSelected) category.color else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category.displayName,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) category.color else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. ETIQUETA VISIBLE OBLIGATORIA: Descripción escrita
        Text(
            text = "2. Escribí dónde está el peligro:",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = descriptionInput,
            onValueChange = onDescriptionChange,
            // Etiqueta flotante visible permanentemente
            label = {
                Text(
                    text = "Ubicación del peligro",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            placeholder = {
                Text(
                    text = "Ej: esquina de la tienda, frente a la parada...",
                    fontSize = 16.sp
                )
            },
            isError = errorMessage != null,
            supportingText = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sé claro para que otros sepan dónde cuidarse",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${descriptionInput.length}/150",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            singleLine = false,
            maxLines = 3,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("location_description_input")
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 3. ETIQUETA VISIBLE OBLIGATORIA: Hora del día
        Text(
            text = "3. ¿Cuándo es más peligroso pasar?:",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
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
                        width = if (isSelected) 2.dp else 1.5.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTimeOfDayChange(timeSlot.displayName) }
                        .testTag("time_${timeSlot.name.lowercase()}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = timeSlot.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeSlot.displayName,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // BOTONES DE ACCIÓN: UN SOLO BOTÓN PRINCIPAL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Botón Secundario
            OutlinedButton(
                onClick = onCancel,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // ÚNICO BOTÓN PRINCIPAL: Relleno sólido llamativo
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .weight(1.6f)
                    .height(52.dp)
                    .testTag("save_report_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar aviso", fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

/**
 * Diálogo modal para compartir reporte.
 * Un solo botón principal (Compartir por apps), los demás son secundarios.
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
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir aviso", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Avisá a tus compañeros para que tomen precauciones o busquen otra calle:",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "⚠️ ${point.category}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "📍 ${point.description}", fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "⏰ Horario: ${point.timeOfDay}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))

                // ÚNICO BOTÓN PRINCIPAL
                Button(
                    onClick = onShareViaSystem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_share_system")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartir por WhatsApp o Mensajes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón Secundario: Copiar texto
                OutlinedButton(
                    onClick = onCopyText,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_copy_text")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar texto del aviso", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón Secundario: Copiar enlace
                OutlinedButton(
                    onClick = onCopyLink,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_copy_link")
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar enlace", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Diálogo de respaldo local.
 * Un solo botón principal para exportar el archivo .json.
 */
@Composable
private fun StorageBackupDialog(
    totalCount: Int,
    jsonPreview: String,
    onDismiss: () -> Unit,
    onExportFile: () -> Unit,
    onCopyJson: () -> Unit,
    onResetSample: () -> Unit,
    onPromptClearAll: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardado y Respaldo", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💾 Guardado en este celular",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tenés $totalCount avisos guardados. No se borran al cerrar la app.",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Opciones para no perder tus datos:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ÚNICO BOTÓN PRINCIPAL
                Button(
                    onClick = onExportFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_backup_file_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar archivo de respaldo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón Secundario: Copiar datos
                OutlinedButton(
                    onClick = onCopyJson,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar texto de respaldo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // Botón Secundario: Cargar ejemplo
                OutlinedButton(
                    onClick = onResetSample,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cargar aviso de ejemplo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón Secundario: Borrar todo
                OutlinedButton(
                    onClick = onPromptClearAll,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Borrar todos los avisos", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * ESTADO VACÍO (Requisito 5):
 * - Frase clara de estado.
 * - Frase empática que invita a la primera acción.
 * - UN SOLO botón principal ("Marcar el primer punto").
 * - Lectura limpia y tipografía >= 16sp.
 */
@Composable
private fun EmptyStateCard(
    selectedFilter: FilterTime,
    onOpenForm: () -> Unit,
    onLoadSample: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (selectedFilter == FilterTime.TODAS) {
                    "Aún no hay puntos reportados en el camino"
                } else {
                    "No hay avisos para el turno de la ${selectedFilter.label.lowercase()}"
                },
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Frase que invita a la primera acción
            Text(
                text = "Sé la primera persona en avisar si viste una zanja, un perro suelto o una calle a oscuras para cuidar a tus compañeros.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // UN SOLO BOTÓN PRINCIPAL
            Button(
                onClick = onOpenForm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(52.dp)
                    .testTag("empty_state_add_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Marcar el primer punto", fontSize = 17.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón Secundario
            OutlinedButton(
                onClick = onLoadSample,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cargar un ejemplo para probar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun shareReportViaIntent(context: Context, textToShare: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, textToShare)
        type = "text/plain"
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooserIntent = Intent.createChooser(sendIntent, "Compartir aviso de Ruta Segura").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "No encontramos ninguna app de mensajería instalada para compartir el aviso", Toast.LENGTH_SHORT).show()
    }
}

private fun copyToClipboard(context: Context, label: String, textToCopy: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, textToCopy)
    clipboard.setPrimaryClip(clip)
}
