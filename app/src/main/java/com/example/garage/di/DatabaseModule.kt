package com.example.garage.di

import android.content.Context
import androidx.room.Room
import com.example.garage.data.local.AppDatabase
import com.example.garage.data.local.dao.MaintenanceTaskDao
import com.example.garage.data.local.dao.ServiceRecordDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "garage.db").build()

    @Provides
    fun provideVehicleDao(db: AppDatabase): VehicleDao = db.vehicleDao()

    @Provides
    fun provideServiceRecordDao(db: AppDatabase): ServiceRecordDao = db.serviceRecordDao()

    @Provides
    fun provideMaintenanceTaskDao(db: AppDatabase): MaintenanceTaskDao = db.maintenanceTaskDao()
}
