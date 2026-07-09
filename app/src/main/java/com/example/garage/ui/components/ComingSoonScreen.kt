package com.example.garage.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Stand-in for tabs not built yet in this phase. Swap each call site for
 * the real screen as it's built - see the project README for the phase
 * plan (Vehicles + Add Vehicle, then Log Record + Maintenance, then
 * Profile for real).
 */
@Composable
fun ComingSoonScreen(title: String, modifier: Modifier = Modifier) {
    EmptyState(
        title = title,
        message = "This screen is coming in a later phase.",
        modifier = modifier.fillMaxSize()
    )
}
