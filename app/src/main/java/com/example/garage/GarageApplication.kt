package com.example.garage

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.garage.data.sync.SyncCoordinator
import com.example.garage.di.ApplicationScope
import com.example.garage.notifications.ReminderScheduler
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltAndroidApp
class GarageApplication : Application(), Configuration.Provider {

    @Inject lateinit var syncCoordinator: SyncCoordinator
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )
        syncCoordinator.start(appScope)
        reminderScheduler.scheduleDailyCheck()
    }
}
