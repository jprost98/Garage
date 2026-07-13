package com.example.garage.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.garage.ui.components.EmptyState
import com.example.garage.ui.components.ServiceRecordRow
import com.example.garage.ui.components.TaskRow
import com.example.garage.domain.model.TaskUrgency
import com.example.garage.ui.vehicles.VehicleListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    onVehicleClick: (String) -> Unit,
    onRecordClick: (String) -> Unit,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Archive") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Vehicles") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Records") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Tasks") }
                )
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> ArchivedVehiclesList(
                        vehicles = state.archivedVehicles,
                        onVehicleClick = onVehicleClick,
                        onUnarchive = viewModel::unarchiveVehicle
                    )
                    1 -> ArchivedRecordsList(
                        records = state.archivedRecords,
                        onRecordClick = onRecordClick
                    )
                    2 -> ArchivedTasksList(
                        tasks = state.archivedTasks,
                        onTaskClick = onTaskClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivedVehiclesList(
    vehicles: List<VehicleListItem>,
    onVehicleClick: (String) -> Unit,
    onUnarchive: (String) -> Unit
) {
    if (vehicles.isEmpty()) {
        EmptyState(
            title = "No archived vehicles",
            message = "Vehicles you archive will appear here.",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(vehicles, key = { it.vehicle.id }) { item ->
                ArchivedVehicleCard(
                    item = item,
                    onClick = { onVehicleClick(item.vehicle.id) },
                    onUnarchive = { onUnarchive(item.vehicle.id) }
                )
            }
        }
    }
}

@Composable
private fun ArchivedVehicleCard(
    item: VehicleListItem,
    onClick: () -> Unit,
    onUnarchive: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.vehicle.title, style = MaterialTheme.typography.bodyLarge)
                val subtitle = buildString {
                    append("%,d mi".format(item.vehicle.odometer))
                    append(" · ${item.recordCount} records")
                    if (item.openTaskCount > 0) {
                        append(" · ${item.openTaskCount} tasks")
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onUnarchive) {
                Icon(
                    imageVector = Icons.Default.Unarchive,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ArchivedRecordsList(
    records: List<com.example.garage.domain.model.ServiceRecord>,
    onRecordClick: (String) -> Unit
) {
    if (records.isEmpty()) {
        EmptyState(
            title = "No archived records",
            message = "Records from archived vehicles will appear here.",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(records, key = { it.id }) { record ->
                ServiceRecordRow(
                    record = record,
                    onClick = { onRecordClick(record.id) }
                )
            }
        }
    }
}

@Composable
private fun ArchivedTasksList(
    tasks: List<com.example.garage.domain.model.MaintenanceTask>,
    onTaskClick: (String) -> Unit
) {
    if (tasks.isEmpty()) {
        EmptyState(
            title = "No archived tasks",
            message = "Tasks from archived vehicles will appear here.",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    urgency = TaskUrgency.UpToDate, // Urgency doesn't matter for archived vehicles
                    onToggleComplete = { }, // Can't complete tasks of archived vehicles
                    onClick = { onTaskClick(task.id) }
                )
            }
        }
    }
}
