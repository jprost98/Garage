package com.example.garage.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.garage.domain.model.ServiceRecord
import com.example.garage.domain.model.Vehicle
import com.example.garage.ui.components.EmptyState
import com.example.garage.ui.components.ServiceRecordRow
import com.example.garage.ui.components.StatCard
import com.example.garage.ui.components.TaskRow

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onTaskClick: (String) -> Unit,
    onRecordClick: (String) -> Unit,
    onAddVehicle: () -> Unit,
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            GreetingSection(greeting = state.greeting, userName = state.userName)
        }

        // Only shown once there's more than one vehicle to give an overview of.
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        label = "Total spent",
                        value = "$${"%,.2f".format(state.totalSpent)}",
                        icon = Icons.Filled.Receipt,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Spent this month",
                        value = "$${"%,.2f".format(state.costThisMonth)}",
                        icon = Icons.Filled.Receipt,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        label = "Total records",
                        value = "${state.totalRecords}",
                        icon = Icons.Filled.MiscellaneousServices,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Active vehicles",
                        value = "${state.vehicles.size}",
                        icon = Icons.Filled.DirectionsCar,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (state.dueTasks.isNotEmpty()) {
            item {
                MaintenanceDueSection(
                    dueTasks = state.dueTasks,
                    onTaskClick = onTaskClick
                )
            }
        }

        item {
            RecentServiceSection(
                records = state.recentRecords,
                vehicles = state.vehicles,
                onRecordClick = onRecordClick
            )
        }

        item {
            UpcomingRemindersSection(
                reminders = state.upcomingReminders,
                onTaskClick = onTaskClick
            )
        }
    }
}

@Composable
private fun GreetingSection(greeting: String, userName: String?) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "$greeting, ${userName ?: "there"}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    leadingIcon: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leadingIcon?.invoke()
                if (leadingIcon != null) Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun MaintenanceDueSection(
    dueTasks: List<TaskWithUrgency>,
    onTaskClick: (String) -> Unit
) {
    SectionCard(
        title = "Maintenance due",
        titleColor = MaterialTheme.colorScheme.error,
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    ) {
        dueTasks.forEach { item ->
            TaskListItem(item = item, onTaskClick = onTaskClick)
        }
    }
}

@Composable
private fun RecentServiceSection(
    records: List<ServiceRecord>,
    vehicles: List<Vehicle>,
    onRecordClick: (String) -> Unit
) {
    SectionCard(title = "Recent service") {
        if (records.isEmpty()) {
            Text(
                text = "No service records logged yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            records.forEach { record ->
                val vehicle = vehicles.find { it.id == record.vehicleId }
                ServiceRecordRow(
                    record = record,
                    vehicle = vehicle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    onClick = { onRecordClick(record.id) }
                )
            }
        }
    }
}

@Composable
private fun UpcomingRemindersSection(
    reminders: List<TaskWithUrgency>,
    onTaskClick: (String) -> Unit
) {
    SectionCard(title = "Upcoming reminders") {
        if (reminders.isEmpty()) {
            Text(
                text = "No upcoming maintenance tasks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            reminders.forEach { item ->
                TaskListItem(item = item, onTaskClick = onTaskClick)
            }
        }
    }
}

@Composable
private fun TaskListItem(
    item: TaskWithUrgency,
    onTaskClick: (String) -> Unit
) {
    Column {
        if (item.vehicleLabel != null) {
            Text(
                text = item.vehicleLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        TaskRow(
            task = item.task,
            urgency = item.urgency,
            onToggleComplete = { },
            onClick = { onTaskClick(item.task.vehicleId) }
        )
    }
}