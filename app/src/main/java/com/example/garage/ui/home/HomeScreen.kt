package com.example.garage.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.garage.ui.components.EmptyState
import com.example.garage.ui.components.ServiceRecordRow
import com.example.garage.ui.components.TaskRow
import com.example.garage.ui.components.VehicleChip

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onAddVehicle: () -> Unit,
    onTaskClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (!state.isLoading && !state.hasVehicles) {
        EmptyState(
            title = "No vehicles yet",
            message = "Add your first vehicle to start tracking maintenance.",
            actionLabel = "Add a vehicle",
            onAction = onAddVehicle,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 10.dp)) {
            Text(text = "Garage", style = MaterialTheme.typography.titleMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                items(state.vehicles) { vehicle ->
                    VehicleChip(
                        label = vehicle.title,
                        selected = vehicle.id == state.selectedVehicleId,
                        onClick = { viewModel.selectVehicle(vehicle.id) }
                    )
                }
            }
        }

        if (state.dueTasks.isNotEmpty()) {
            SectionLabel("Due soon")
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                state.dueTasks.forEach { item ->
                    TaskRow(
                        task = item.task,
                        urgency = item.urgency,
                        onToggleComplete = { },
                        onClick = { onTaskClick(item.task.vehicleId) }
                    )
                }
            }
        }

        if (state.recentRecords.isNotEmpty()) {
            SectionLabel("Recent activity")
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                state.recentRecords.forEach { record ->
                    ServiceRecordRow(record = record, modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
    )
}
