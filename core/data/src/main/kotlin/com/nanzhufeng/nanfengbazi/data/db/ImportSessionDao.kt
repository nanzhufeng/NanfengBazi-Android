package com.nanzhufeng.nanfengbazi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
internal interface ImportSessionDao {
    @Query("SELECT * FROM import_sessions WHERE id = :id")
    suspend fun findById(id: String): ImportSessionEntity?

    @Query("SELECT * FROM import_sessions ORDER BY updatedAtEpochMillis DESC, id")
    suspend fun all(): List<ImportSessionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ImportSessionEntity)

    @Update
    suspend fun update(entity: ImportSessionEntity)

    @Query("DELETE FROM import_sessions WHERE id = :id")
    suspend fun delete(id: String): Int
}
