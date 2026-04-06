package com.android.daviddev.ecoscancmem.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScanEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EcoScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: EcoScanDatabase? = null

        fun getInstance(context: Context): EcoScanDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EcoScanDatabase::class.java,
                    "ecoscan.db"
                ).build().also { INSTANCE = it }
            }
    }
}