package com.example.garage.ui.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.ServiceCategory
import com.example.garage.domain.model.TaskUrgency
import com.example.garage.ui.components.StatCard
import com.example.garage.ui.components.TaskUrgencyBadge
import com.example.garage.ui.theme.DangerContainer
import com.example.garage.ui.theme.DangerOnContainer
import com.example.garage.ui.theme.TealContainer
import com.example.garage.ui.theme.TealOnContainer
import com.example.garage.ui.theme.WarningContainer
import com.example.garage.ui.theme.WarningOnContainer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    onEdit: (vehicleId: String, taskId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Maintenance task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.task?.let { task ->
                        IconButton(onClick = { onEdit(task.vehicleId, task.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        val task = state.task
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                task != null -> {
                    TaskDetailContent(
                        task = task,
                        urgency = state.urgency,
                        onToggleComplete = viewModel::toggleComplete
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this task?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TaskDetailContent(
    task: MaintenanceTask,
    urgency: TaskUrgency,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = categoryStyle(task.category)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = style.container),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(style.onContainer.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = style.onContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    TaskUrgencyBadge(urgency = urgency)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = style.onContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.category.label.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall,
                    color = style.onContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onToggleComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (task.completed) style.onContainer else style.onContainer.copy(alpha = 0.1f),
                        contentColor = if (task.completed) style.container else style.onContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (task.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (task.completed) "Completed" else "Mark as done")
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val frequency = frequencyLabel(task)
            StatCard(
                label = "Frequency",
                value = frequency,
                icon = Icons.Filled.Build,
                modifier = Modifier.weight(1f)
            )
            
            val due = dueLabel(task)
            StatCard(
                label = "Next due",
                value = due,
                icon = Icons.Filled.Warning,
                modifier = Modifier.weight(1f)
            )
        }

        task.notes?.takeIf { it.isNotBlank() }?.let { notes ->
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

private fun frequencyLabel(task: MaintenanceTask): String {
    val parts = mutableListOf<String>()
    task.intervalMiles?.let { parts.add("${it} mi") }
    task.intervalMonths?.let { parts.add("$it mo") }
    return if (parts.isEmpty()) "Once" else parts.joinToString(" / ")
}

private fun dueLabel(task: MaintenanceTask): String {
    val parts = mutableListOf<String>()
    task.dueOdometer?.let { parts.add("${"%,d".format(it)} mi") }
    task.dueDate?.let {
        parts.add(dateFormatter.format(Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()))
    }
    return if (parts.isEmpty()) "—" else parts.joinToString("\n")
}

private data class CategoryStyle(val icon: ImageVector, val container: Color, val onContainer: Color)

@Composable
private fun categoryStyle(category: ServiceCategory): CategoryStyle = when (category) {
    ServiceCategory.OIL_CHANGE -> CategoryStyle(Icons.Filled.Opacity, TealContainer, TealOnContainer)
    ServiceCategory.FLUIDS -> CategoryStyle(Icons.Filled.WaterDrop, TealContainer, TealOnContainer)
    ServiceCategory.TIRES -> CategoryStyle(Icons.Filled.TripOrigin, WarningContainer, WarningOnContainer)
    ServiceCategory.INSPECTION -> CategoryStyle(Icons.AutoMirrored.Filled.FactCheck, WarningContainer, WarningOnContainer)
    ServiceCategory.BRAKES -> CategoryStyle(Icons.Filled.Warning, DangerContainer, DangerOnContainer)
    ServiceCategory.BATTERY -> CategoryStyle(Icons.Filled.BatteryChargingFull, DangerContainer, DangerOnContainer)
    ServiceCategory.OTHER -> CategoryStyle(
        Icons.Filled.Build,
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
