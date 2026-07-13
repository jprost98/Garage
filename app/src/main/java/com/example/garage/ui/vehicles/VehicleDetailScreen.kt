package com.example.garage.ui.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.ui.components.EmptyState
import com.example.garage.ui.components.ServiceRecordRow
import com.example.garage.ui.components.StatCard
import com.example.garage.ui.components.SuggestionChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    onBack: () -> Unit,
    onEdit: (vehicleId: String) -> Unit,
    onLogRecord: (vehicleId: String) -> Unit,
    onRecordClick: (recordId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VehicleDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(state.vehicle?.title ?: "Vehicle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(viewModel.vehicleId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit vehicle")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                onClick = {
                                    showMenu = false
                                    showArchiveConfirm = true
                                },
                                leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirm = true
                                },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Filled.Delete, 
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    ) 
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onLogRecord(viewModel.vehicleId) }) {
                Icon(Icons.Filled.Add, contentDescription = "Log a service")
            }
        }
    ) { padding ->
        if (state.isSuggesting) {
            AlertDialog(
                onDismissRequest = { /* Cannot dismiss while loading */ },
                title = { Text("Analyzing...") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Gemini is analyzing your vehicle's maintenance needs...")
                    }
                },
                confirmButton = {}
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading && !state.isSuggesting) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                VehicleDetailContent(
                    state = state,
                    onRecordClick = onRecordClick,
                    onSortChange = viewModel::updateSortOption,
                    onToggleSuggestion = viewModel::toggleSuggestion,
                    onAddSuggestions = viewModel::addSelectedSuggestions,
                    onDismissSuggestions = viewModel::dismissSuggestions,
                    onSuggestTasks = viewModel::suggestTasks
                )
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Vehicle?") },
                text = { Text("This will permanently remove the vehicle and all its service history from both your device and cloud backup. This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteVehicle()
                            showDeleteConfirm = false
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showArchiveConfirm) {
            AlertDialog(
                onDismissRequest = { showArchiveConfirm = false },
                title = { Text("Archive Vehicle?") },
                text = { Text("This vehicle will be moved to your archives. Its history will be preserved, but it will no longer appear in your active garage.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.archiveVehicle()
                            showArchiveConfirm = false
                            onBack()
                        }
                    ) {
                        Text("Archive")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showArchiveConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun VehicleDetailContent(
    state: VehicleDetailUiState,
    onRecordClick: (String) -> Unit,
    onSortChange: (RecordSortOption) -> Unit,
    onToggleSuggestion: (MaintenanceTask) -> Unit,
    onAddSuggestions: () -> Unit,
    onDismissSuggestions: () -> Unit,
    onSuggestTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        state.vehicle?.let { vehicle ->
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            
                            OutlinedButton(
                                onClick = onSuggestTasks
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "Analyze with AI",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyze")
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = vehicle.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        vehicle.engine?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            if (state.suggestions.isNotEmpty() || state.suggestionResponse != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            state.suggestionResponse?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            if (state.suggestions.isNotEmpty()) {
                                Text(
                                    text = "AI Recommendations",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.suggestions.forEach { task ->
                                        SuggestionChip(
                                            task = task,
                                            selected = state.selectedSuggestionNames.contains(task.name),
                                            onToggle = { onToggleSuggestion(task) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = onDismissSuggestions,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Dismiss")
                                }
                                if (state.suggestions.isNotEmpty()) {
                                    Button(
                                        onClick = onAddSuggestions,
                                        modifier = Modifier.weight(1f),
                                        enabled = state.selectedSuggestionNames.isNotEmpty()
                                    ) {
                                        Text("Add selected")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        label = "Odometer",
                        value = "${"%,d".format(vehicle.odometer)} mi",
                        icon = Icons.Filled.Speed,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = if (state.records.size == 1) "Record logged" else "Records logged",
                        value = "${state.records.size}",
                        icon = Icons.Filled.Build,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            vehicle.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Service history",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort records",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        RecordSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onSortChange(option)
                                    showSortMenu = false
                                },
                                trailingIcon = {
                                    if (option == state.sortOption) {
                                        Text("✓", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (state.records.isEmpty()) {
            item {
                EmptyState(
                    title = "No service history yet",
                    message = "Tap + to log the first service record for this vehicle.",
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        } else {
            items(state.records, key = { it.id }) { record ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ServiceRecordRow(
                        record = record,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        onClick = { onRecordClick(record.id) }
                    )
                }
            }
        }
    }
}
