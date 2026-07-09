package com.example.garage.domain.usecase

import com.example.garage.domain.model.MaintenanceTask
import com.example.garage.domain.model.TaskUrgency
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Derives urgency at read time instead of storing it. This is the piece
 * that used to live implicitly in `TaskUtils.kt` in the old app - pulling
 * it into its own class makes it independently unit-testable (see
 * app/src/test/java/.../domain/TaskUrgencyCalculatorTest.kt).
 */
class TaskUrgencyCalculator @Inject constructor() {

    private val dueSoonWindowMs = TimeUnit.DAYS.toMillis(14)
    private val dueSoonMileageWindow = 500

    fun urgencyFor(task: MaintenanceTask, currentOdometer: Int, now: Long = System.currentTimeMillis()): TaskUrgency {
        val milesLeft = task.dueOdometer?.let { it - currentOdometer }
        val daysLeft = task.dueDate?.let { TimeUnit.MILLISECONDS.toDays(it - now) }

        val isOverdue = (milesLeft != null && milesLeft < 0) || (daysLeft != null && daysLeft < 0)
        if (isOverdue) {
            return TaskUrgency.Overdue(
                milesPast = milesLeft?.let { -it },
                daysPast = daysLeft?.let { -it.toInt() }
            )
        }

        val isDueSoon = (milesLeft != null && milesLeft <= dueSoonMileageWindow) ||
            (daysLeft != null && daysLeft * TimeUnit.DAYS.toMillis(1) <= dueSoonWindowMs)
        if (isDueSoon) {
            return TaskUrgency.DueSoon(milesLeft = milesLeft, daysLeft = daysLeft?.toInt())
        }

        if (milesLeft == null && daysLeft == null) return TaskUrgency.UpToDate

        return TaskUrgency.Upcoming(milesLeft = milesLeft, daysLeft = daysLeft?.toInt())
    }
}
