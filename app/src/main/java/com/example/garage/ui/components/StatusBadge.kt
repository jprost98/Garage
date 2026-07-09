package com.example.garage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.garage.domain.model.TaskUrgency
import com.example.garage.ui.theme.DangerContainer
import com.example.garage.ui.theme.DangerOnContainer
import com.example.garage.ui.theme.TealContainer
import com.example.garage.ui.theme.TealOnContainer
import com.example.garage.ui.theme.WarningContainer
import com.example.garage.ui.theme.WarningOnContainer

@Composable
fun TaskUrgencyBadge(urgency: TaskUrgency, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (urgency) {
        is TaskUrgency.Overdue -> Triple("Overdue", DangerContainer, DangerOnContainer)
        is TaskUrgency.DueSoon -> Triple(dueSoonLabel(urgency), WarningContainer, WarningOnContainer)
        is TaskUrgency.Upcoming -> Triple(upcomingLabel(urgency), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        TaskUrgency.UpToDate -> Triple("Up to date", TealContainer, TealOnContainer)
    }
    Text(
        text = label,
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .background(bg, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private fun dueSoonLabel(urgency: TaskUrgency.DueSoon): String = when {
    urgency.daysLeft != null -> "${urgency.daysLeft} days"
    urgency.milesLeft != null -> "${urgency.milesLeft} mi"
    else -> "Due soon"
}

private fun upcomingLabel(urgency: TaskUrgency.Upcoming): String = when {
    urgency.milesLeft != null -> "${urgency.milesLeft} mi left"
    urgency.daysLeft != null -> "${urgency.daysLeft} days left"
    else -> "Upcoming"
}
