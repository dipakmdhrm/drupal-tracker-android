package com.drupaltracker.app.data.db

import androidx.room.*
import com.drupaltracker.app.data.model.NotificationRecord

@Dao
interface NotificationRecordDao {
    @Query("SELECT * FROM notification_records ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int = 50, offset: Int = 0): List<NotificationRecord>

    @Query("SELECT COUNT(*) FROM notification_records")
    suspend fun count(): Int

    @Insert
    suspend fun insert(record: NotificationRecord)

    @Query("DELETE FROM notification_records WHERE id NOT IN (SELECT id FROM notification_records ORDER BY timestamp DESC LIMIT 500)")
    suspend fun pruneOldRecords()
}
