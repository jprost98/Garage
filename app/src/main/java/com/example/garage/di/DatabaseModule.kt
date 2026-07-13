package com.example.garage.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.garage.data.local.AppDatabase
import com.example.garage.data.local.dao.MaintenanceTaskDao
import com.example.garage.data.local.dao.ServiceRecordDao
import com.example.garage.data.local.dao.UserDao
import com.example.garage.data.local.dao.VehicleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE maintenance_tasks ADD COLUMN lastNotifiedUrgencyLevel TEXT")
            }
        }
        
        return Room.databaseBuilder(context, AppDatabase::class.java, "garage.db")
            .addMigrations(MIGRATION_5_6)
            .fallbackToDestructiveMigration() // Added for version bump
            .build()
    }

    @Provides
    fun provideVehicleDao(db: AppDatabase): VehicleDao = db.vehicleDao()

    @Provides
    fun provideServiceRecordDao(db: AppDatabase): ServiceRecordDao = db.serviceRecordDao()

    @Provides
    fun provideMaintenanceTaskDao(db: AppDatabase): MaintenanceTaskDao = db.maintenanceTaskDao()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
}
