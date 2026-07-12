package com.example.garage.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garage.data.repository.AuthRepository
import com.example.garage.data.repository.MaintenanceTaskRepository
import com.example.garage.data.repository.ServiceRecordRepository
import com.example.garage.data.repository.UserRepository
import com.example.garage.data.repository.VehicleRepository
import com.example.garage.domain.model.TaskUrgency
import com.example.garage.domain.usecase.TaskUrgencyCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    vehicleRepository: VehicleRepository,
    serviceRecordRepository: ServiceRecordRepository,
    maintenanceTaskRepository: MaintenanceTaskRepository,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    private val urgencyCalculator: TaskUrgencyCalculator
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        vehicleRepository.observeVehicles(),
        serviceRecordRepository.observeAll(),
        maintenanceTaskRepository.observeAll(),
        authRepository.observeAuthState().flatMapLatest { authUser ->
            if (authUser != null) userRepository.observeUser(authUser.uid) else flowOf(null)
        }
    ) { allVehicles, allRecords, allTasks, user ->
        // Archived vehicles shouldn't clutter the home overview, its stats, or its reminders.
        val vehicles = allVehicles.filterNot { it.isArchived }
        val activeVehicleIds = vehicles.mapTo(mutableSetOf()) { it.id }
        val records = allRecords.filter { it.vehicleId in activeVehicleIds }
        val tasks = allTasks.filter { it.vehicleId in activeVehicleIds }

        val odometerByVehicle = vehicles.associate { it.id to it.odometer }
        val vehicleById = vehicles.associateBy { it.id }
        // Only bother labeling tasks with their vehicle once there's more than one to tell apart.
        val showVehicleLabels = vehicles.size > 1

        val allTasksWithUrgency = tasks
            .filter { !it.completed }
            .map { task ->
                val odometer = odometerByVehicle[task.vehicleId] ?: 0
                val vehicleLabel = if (showVehicleLabels) vehicleById[task.vehicleId]?.displayName else null
                TaskWithUrgency(task, urgencyCalculator.urgencyFor(task, odometer), vehicleLabel)
            }

        val dueTasks = allTasksWithUrgency
            .filter { it.urgency is TaskUrgency.Overdue || it.urgency is TaskUrgency.DueSoon }
            .sortedBy { if (it.urgency is TaskUrgency.Overdue) 0 else 1 }

        val upcomingReminders = allTasksWithUrgency
            .filter { it.urgency is TaskUrgency.Upcoming }
            .take(3)

        val recentRecords = records
            .sortedByDescending { it.date }
            .take(5)

        val localCalendar = Calendar.getInstance()
        val hour = localCalendar.get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }

        val utcCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        utcCalendar.set(Calendar.DAY_OF_MONTH, 1)
        utcCalendar.set(Calendar.HOUR_OF_DAY, 0)
        utcCalendar.set(Calendar.MINUTE, 0)
        utcCalendar.set(Calendar.SECOND, 0)
        utcCalendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = utcCalendar.timeInMillis

        val costThisMonth = records
            .filter { it.date >= startOfMonth }
            .sumOf { it.cost ?: 0.0 }

        val totalSpent = records.sumOf { it.cost ?: 0.0 }
        val totalMileage = vehicles.sumOf { it.odometer }

        val vehicleOverviews = vehicles.map { vehicle ->
            val vehicleDueTasks = allTasksWithUrgency
                .filter {
                    it.task.vehicleId == vehicle.id &&
                            (it.urgency is TaskUrgency.Overdue || it.urgency is TaskUrgency.DueSoon)
                }
                .sortedBy { if (it.urgency is TaskUrgency.Overdue) 0 else 1 }

            VehicleOverview(
                vehicle = vehicle,
                dueTaskCount = vehicleDueTasks.size,
                nextDueTask = vehicleDueTasks.firstOrNull()
            )
        }

        HomeUiState(
            greeting = greeting,
            userName = user?.firstName ?: user?.displayName?.substringBefore(' ') ?: user?.email?.substringBefore('@'),
            vehicles = vehicles,
            vehicleOverviews = vehicleOverviews,
            dueTasks = dueTasks,
            recentRecords = recentRecords,
            upcomingReminders = upcomingReminders,
            totalMileage = totalMileage,
            totalSpent = totalSpent,
            costThisMonth = costThisMonth,
            totalRecords = records.size,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}