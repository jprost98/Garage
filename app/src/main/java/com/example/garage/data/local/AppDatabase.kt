package com.example.garage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.garage.data.local.dao.MaintenanceTaskDao
import com.example.garage.data.local.dao.ServiceRecordDao
import com.example.garage.data.local.dao.UserDao
import com.example.garage.data.local.dao.VehicleDao
import com.example.garage.data.local.entity.MaintenanceTaskEntity
import com.example.garage.data.local.entity.ServiceRecordEntity
import com.example.garage.data.local.entity.UserEntity
import com.example.garage.data.local.entity.VehicleEntity

/**
 * Room is the single source of truth the UI reads from. Firestore only
 * ever talks to the repository layer (see data/repository and
 * data/remote) - Compose screens and ViewModels never touch Firestore
 * directly. That's what makes the app work offline: the UI doesn't care
 * whether a write has synced yet.
 */
@Database(
    entities = [
        VehicleEntity::class,
        ServiceRecordEntity::class,
        MaintenanceTaskEntity::class,
        UserEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun maintenanceTaskDao(): MaintenanceTaskDao
    abstract fun userDao(): UserDao
}
