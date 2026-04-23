package com.drupaltracker.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.drupaltracker.app.data.model.SeenIssue
import com.drupaltracker.app.data.model.WatchedProject

@Database(
    entities = [WatchedProject::class, SeenIssue::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun issueDao(): IssueDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE seen_issues ADD COLUMN cachedSummary TEXT")
                db.execSQL("ALTER TABLE seen_issues ADD COLUMN summarizedCommentCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drupal_tracker.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
