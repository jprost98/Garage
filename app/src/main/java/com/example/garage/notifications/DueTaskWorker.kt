package com.example.garage.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.garage.data.repository.MaintenanceTaskRepository
import com.example.garage.domain.model.MaintenanceTask
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val CHANNEL_ID = "maintenance_due"
private const val CHANNEL_NAME = "Maintenance reminders"

/**
 * Runs once a day (see [ReminderScheduler]). Checks every active task
 * against "due within 7 days or 300 miles" and fires one notification per
 * task that qualifies. Deliberately simple - a real app would also dedupe
 * notifications already shown today, but this demonstrates the pattern.
 */
@HiltWorker
class DueTaskWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: MaintenanceTaskRepository,
    private val vehicleRepository: com.example.garage.data.repository.VehicleRepository,
    private val urgencyCalculator: com.example.garage.domain.usecase.TaskUrgencyCalculator
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        ensureChannel()
        val tasks = taskRepository.getActiveTasksOnce()
        val now = System.currentTimeMillis()

        tasks.forEach { task ->
            val vehicle = vehicleRepository.getVehicleById(task.vehicleId) ?: return@forEach
            val urgency = urgencyCalculator.urgencyFor(task, vehicle.odometer, now)
            
            val urgencyLevelStr = when (urgency) {
                is com.example.garage.domain.model.TaskUrgency.Overdue -> "OVERDUE"
                is com.example.garage.domain.model.TaskUrgency.DueSoon -> "DUE_SOON"
                else -> null
            }
            
            if (urgencyLevelStr != null && urgencyLevelStr != task.lastNotifiedUrgencyLevel) {
                notify(task, urgency)
                taskRepository.addTask(task.copy(lastNotifiedUrgencyLevel = urgencyLevelStr))
            } else if (urgencyLevelStr == null && task.lastNotifiedUrgencyLevel != null) {
                // If it's no longer due/overdue (e.g., was completed), clear the notified state
                taskRepository.addTask(task.copy(lastNotifiedUrgencyLevel = null))
            }
        }
        return Result.success()
    }

    private fun notify(task: MaintenanceTask, urgency: com.example.garage.domain.model.TaskUrgency) {
        val message = when (urgency) {
            is com.example.garage.domain.model.TaskUrgency.Overdue -> "Task is overdue!"
            is com.example.garage.domain.model.TaskUrgency.DueSoon -> "Task is due soon."
            else -> "Coming up on your vehicle"
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(task.name)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(task.id.hashCode(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
    }
}
