package com.example.garage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.TaskUrgency

@Composable
fun TaskRow(
    task: MaintenanceTask,
    urgency: TaskUrgency,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onToggleComplete, modifier = Modifier.size(20.dp)) {
            Icon(
                imageVector = if (task.completed) Icons.Filled.Circle else Icons.Outlined.Circle,
                contentDescription = if (task.completed) "Mark incomplete" else "Mark complete",
                tint = MaterialTheme.colorScheme.outline
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = task.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = frequencyLabel(task),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TaskUrgencyBadge(urgency = urgency)
    }
}

private fun frequencyLabel(task: MaintenanceTask): String {
    val parts = mutableListOf<String>()
    task.intervalMiles?.let { parts.add("every ${it} mi") }
    task.intervalMonths?.let { parts.add("every $it mo") }
    return if (parts.isEmpty()) "One-time" else parts.joinToString(" · ")
}
