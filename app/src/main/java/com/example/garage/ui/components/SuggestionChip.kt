package com.example.garage.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.garage.domain.model.MaintenanceTask

@Composable
fun SuggestionChip(
    task: MaintenanceTask,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = {
            Column {
                Text(task.name)
                task.intervalMiles?.let {
                    Text("Every $it miles", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        modifier = modifier.fillMaxWidth()
    )
}
