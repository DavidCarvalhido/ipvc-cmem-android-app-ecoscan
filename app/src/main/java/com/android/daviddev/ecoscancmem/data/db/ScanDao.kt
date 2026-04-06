package com.android.daviddev.ecoscancmem.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scan_history WHERE id = :id")
    suspend fun getScanById(id: Int): ScanEntity?

    @Query("SELECT COUNT(*) FROM scan_history")
    fun getTotalScans(): Flow<Int>

    @Query("SELECT SUM(co2SavedGrams) FROM scan_history WHERE isRecyclable = 1")
    fun getTotalCo2Saved(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity): Long

    @Delete
    suspend fun deleteScan(scan: ScanEntity)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()
}