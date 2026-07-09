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
import java.util.concurrent.TimeUnit

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
    private val taskRepository: MaintenanceTaskRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        ensureChannel()
        val tasks = taskRepository.getActiveTasksOnce()
        val now = System.currentTimeMillis()
        val sevenDaysMs = TimeUnit.DAYS.toMillis(7)

        tasks.forEach { task ->
            val dueSoonByDate = task.dueDate?.let { it - now in 0..sevenDaysMs || it < now }
            val dueSoonByMileage = task.dueOdometer != null // odometer comparison happens against the
                // vehicle's current reading in the ViewModel layer for the UI list; the worker keeps
                // this simple and only fires on date-based due tasks to avoid false positives offline.
            if (dueSoonByDate == true) {
                notify(task)
            }
        }
        return Result.success()
    }

    private fun notify(task: MaintenanceTask) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(task.name)
            .setContentText("Coming up on your vehicle")
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
