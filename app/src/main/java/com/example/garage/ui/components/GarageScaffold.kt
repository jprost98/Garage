package com.example.garage.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.garage.ui.navigation.Routes

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(Routes.HOME, "Home", Icons.Filled.Home),
    BottomDestination(Routes.VEHICLES, "Vehicles", Icons.Filled.DirectionsCar),
    BottomDestination(Routes.MAINTENANCE, "Checkups", Icons.Filled.Checklist),
    BottomDestination(Routes.PROFILE, "Profile", Icons.Filled.Person)
)

/**
 * Shared chrome for the four top-level tabs: bottom nav + a context-aware
 * FAB. On Home the FAB logs a service record; on Vehicles it adds a
 * vehicle; on Checkups it opens a small menu (recurring vs one-time),
 * matching the original app's FAB behavior but rebuilt with Compose state
 * instead of manual Animation objects.
 */
@Composable
fun GarageScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onFabAction: () -> Unit,
    showFab: Boolean = true,
    onFabSecondaryAction: (() -> Unit)? = null,
    fabSecondaryLabel: String? = null,
    onAssistantClick: () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = { onNavigate(dest.route) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                /* FloatingActionButton(
                    onClick = onAssistantClick,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Garage Assistant")
                } */
                if (showFab) {
                    if (onFabSecondaryAction != null) {
                        AnimatedVisibility(visible = menuOpen) {
                            SmallFloatingActionButton(
                                onClick = { menuOpen = false; onFabSecondaryAction() },
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(Icons.Filled.Build, contentDescription = fabSecondaryLabel)
                            }
                        }
                    }
                    FloatingActionButton(
                        onClick = {
                            if (onFabSecondaryAction != null) menuOpen = !menuOpen else onFabAction()
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                }
            }
        }
    ) { padding ->
        content(Modifier.padding(padding).statusBarsPadding())
    }
}
