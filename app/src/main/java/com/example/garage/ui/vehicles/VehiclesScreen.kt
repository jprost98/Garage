package com.example.garage.ui.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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

@Composable
fun VehiclesScreen(
    modifier: Modifier = Modifier,
    onAddVehicle: () -> Unit,
    onVehicleClick: (String) -> Unit,
    viewModel: VehiclesViewModel = hiltViewModel()
) {
    val vehicles by viewModel.vehicles.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Vehicles",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp)
        )

        if (vehicles.isEmpty()) {
            EmptyState(
                title = "No vehicles yet",
                message = "Add a vehicle to start logging its service history.",
                actionLabel = "Add a vehicle",
                onAction = onAddVehicle,
                modifier = Modifier.fillMaxSize()
            )
            return@Column
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(vehicles, key = { it.vehicle.id }) { item ->
                VehicleCard(item = item, onClick = { onVehicleClick(item.vehicle.id) })
            }
        }
    }
}

@Composable
private fun VehicleCard(item: VehicleListItem, onClick: () -> Unit) {
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
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column {
                Text(text = item.vehicle.title, style = MaterialTheme.typography.bodyLarge)
                val status = if (item.openTaskCount > 0) "${item.openTaskCount} tasks due" else "up to date"
                Text(
                    text = "${"%,d".format(item.vehicle.odometer)} mi · $status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
