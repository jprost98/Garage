package com.example.garage.ui.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.garage.domain.model.ServiceCategory
import com.example.garage.domain.model.ServiceRecord
import com.example.garage.ui.components.EmptyState
import com.example.garage.ui.components.StatCard
import com.example.garage.ui.components.TaskRow
import com.example.garage.ui.components.VehicleChip
import com.example.garage.ui.theme.TealContainer
import com.example.garage.ui.theme.TealOnContainer
import com.example.garage.ui.theme.WarningContainer
import com.example.garage.ui.theme.WarningOnContainer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    modifier: Modifier = Modifier,
    onTaskClick: (String, String) -> Unit,
    onAddVehicle: () -> Unit,
    viewModel: MaintenanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (!state.hasVehicles) {
        EmptyState(
            title = "No vehicles yet",
            message = "Add a vehicle to start tracking its checkups.",
            actionLabel = "Add a vehicle",
            onAction = onAddVehicle,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (!state.hasAnyTasks) {
        EmptyState(
            title = "No maintenance tasks yet",
            message = "Tasks you add for your vehicles will show up here, grouped by how soon they're due.",
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val tasksShown = state.overdueTasks.isNotEmpty() || state.dueSoonTasks.isNotEmpty() ||
            state.upcomingTasks.isNotEmpty() || state.upToDateTasks.isNotEmpty() ||
            (state.showCompleted && state.completedTasks.isNotEmpty())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Checkups",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    label = "Overdue",
                    value = "${state.overdueTasks.size}",
                    icon = Icons.Filled.Warning,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Due soon",
                    value = "${state.dueSoonTasks.size}",
                    icon = Icons.Filled.Schedule,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!tasksShown) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks found for this vehicle",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (state.overdueTasks.isNotEmpty()) {
            item {
                MaintenanceSection(
                    title = "Overdue",
                    titleColor = MaterialTheme.colorScheme.error,
                    iconBackground = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                    icon = Icons.Filled.Warning,
                    tasks = state.overdueTasks,
                    onTaskClick = onTaskClick,
                    onToggleComplete = viewModel::toggleTaskComplete
                )
            }
        }

        if (state.dueSoonTasks.isNotEmpty()) {
            item {
                MaintenanceSection(
                    title = "Due soon",
                    titleColor = WarningOnContainer,
                    iconBackground = WarningContainer,
                    iconTint = WarningOnContainer,
                    icon = Icons.Filled.Schedule,
                    tasks = state.dueSoonTasks,
                    onTaskClick = onTaskClick,
                    onToggleComplete = viewModel::toggleTaskComplete
                )
            }
        }

        if (state.upcomingTasks.isNotEmpty()) {
            item {
                MaintenanceSection(
                    title = "Upcoming",
                    icon = Icons.Filled.Upcoming,
                    tasks = state.upcomingTasks,
                    onTaskClick = onTaskClick,
                    onToggleComplete = viewModel::toggleTaskComplete
                )
            }
        }

        if (state.upToDateTasks.isNotEmpty()) {
            item {
                MaintenanceSection(
                    title = "Up to date",
                    titleColor = TealOnContainer,
                    iconBackground = TealContainer,
                    iconTint = TealOnContainer,
                    icon = Icons.Filled.CheckCircle,
                    tasks = state.upToDateTasks,
                    onTaskClick = onTaskClick,
                    onToggleComplete = viewModel::toggleTaskComplete
                )
            }
        }

        if (state.completedTasks.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TextButton(onClick = viewModel::toggleShowCompleted) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            if (state.showCompleted) "Hide completed (${state.completedTasks.size})"
                            else "Show completed (${state.completedTasks.size})"
                        )
                    }
                }
            }
            if (state.showCompleted) {
                item {
                    MaintenanceSection(
                        title = "Completed",
                        tasks = state.completedTasks,
                        onTaskClick = onTaskClick,
                        onToggleComplete = viewModel::toggleTaskComplete
                    )
                }
            }
        }
    }

    state.pendingRecordReview?.let { record ->
        ServiceRecordReviewDialog(
            record = record,
            onConfirm = viewModel::confirmRecordAndCompleteTask,
            onSkip = viewModel::skipRecordAndCompleteTask,
            onDismiss = viewModel::dismissReview
        )
    }

    state.showUndoCompletionWarning?.let { task ->
        AlertDialog(
            onDismissRequest = viewModel::dismissUndoWarning,
            title = { Text("Unmark as complete?") },
            text = { Text("The associated service record will also be deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmUndoCompletion) {
                    Text("Unmark & Delete Record", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissUndoWarning) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceRecordReviewDialog(
    record: ServiceRecord,
    onConfirm: (ServiceRecord) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(record.title) }
    var category by remember { mutableStateOf(record.category) }
    var date by remember { mutableStateOf(record.date) }
    var odometer by remember { mutableStateOf(record.odometer.toString()) }
    var cost by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Service Record") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "You've completed this task. Would you like to add a service record to your vehicle's history?",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        items(ServiceCategory.entries.toList()) { cat ->
                            CategoryChip(
                                label = cat.label,
                                selected = cat == category,
                                onClick = { category = cat }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateFormatter.format(
                            Instant.ofEpochMilli(date)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                        ),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Date") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = odometer,
                        onValueChange = { odometer = it },
                        label = { Text("Odometer") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it },
                        label = { Text("Cost") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    record.copy(
                        title = title,
                        category = category,
                        date = date,
                        odometer = odometer.toIntOrNull() ?: record.odometer,
                        cost = cost.toDoubleOrNull(),
                        description = description.ifBlank { null }
                    )
                )
            }) {
                Text("Add Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip Record")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Text(
        text = label,
        color = contentColor,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(shape)
            .background(background)
            .then(
                if (!selected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun MaintenanceSection(
    title: String,
    tasks: List<TaskWithUrgency>,
    onTaskClick: (String, String) -> Unit,
    onToggleComplete: (String, Boolean) -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconBackground: Color? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(iconBackground ?: MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(
                    text = "$title (${tasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            tasks.forEach { item ->
                TaskListItem(item = item, onTaskClick = onTaskClick, onToggleComplete = onToggleComplete)
            }
        }
    }
}

@Composable
private fun TaskListItem(
    item: TaskWithUrgency,
    onTaskClick: (String, String) -> Unit,
    onToggleComplete: (String, Boolean) -> Unit
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
            onToggleComplete = { onToggleComplete(item.task.id, !item.task.completed) },
            onClick = { onTaskClick(item.task.vehicleId, item.task.id) }
        )
    }
}
