package com.drupaltracker.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.drupaltracker.app.data.model.NotificationRecord
import com.drupaltracker.app.data.model.SeenIssue
import com.drupaltracker.app.data.model.StarredIssue
import com.drupaltracker.app.data.model.StarredProject

@Database(
    entities = [SeenIssue::class, StarredProject::class, StarredIssue::class, NotificationRecord::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun issueDao(): IssueDao
    abstract fun starredProjectDao(): StarredProjectDao
    abstract fun starredIssueDao(): StarredIssueDao
    abstract fun notificationRecordDao(): NotificationRecordDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE seen_issues ADD COLUMN cachedSummary TEXT")
                db.execSQL("ALTER TABLE seen_issues ADD COLUMN summarizedCommentCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS starred_projects (" +
                    "nid TEXT NOT NULL PRIMARY KEY, " +
                    "machineName TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "filterStatus TEXT, " +
                    "filterPriority TEXT, " +
                    "lastChecked INTEGER NOT NULL DEFAULT 0, " +
                    "starredAt INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL(
                    "INSERT INTO starred_projects (nid, machineName, title, filterStatus, filterPriority, lastChecked, starredAt) " +
                    "SELECT nid, machineName, title, filterStatus, filterPriority, lastChecked, addedAt FROM watched_projects"
                )
                db.execSQL("DROP TABLE watched_projects")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS starred_issues (" +
                    "nid TEXT NOT NULL PRIMARY KEY, " +
                    "projectNid TEXT NOT NULL, " +
                    "projectTitle TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "priority TEXT NOT NULL, " +
                    "changed INTEGER NOT NULL, " +
                    "url TEXT NOT NULL, " +
                    "commentCount INTEGER NOT NULL DEFAULT 0, " +
                    "cachedSummary TEXT, " +
                    "summarizedCommentCount INTEGER NOT NULL DEFAULT 0, " +
                    "starredAt INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS notification_records (" +
                    "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "body TEXT NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "recordType TEXT NOT NULL, " +
                    "targetNid TEXT NOT NULL, " +
                    "targetUrl TEXT NOT NULL, " +
                    "isProject INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drupal_tracker.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
