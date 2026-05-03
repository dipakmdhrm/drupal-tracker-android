package com.drupaltracker.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drupaltracker.app.data.model.NotificationRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationRecordDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: NotificationRecordDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.notificationRecordDao()
    }

    @After
    fun tearDown() = db.close()

    // --- insert / count ---

    @Test
    fun insertReturnsPositiveId() = runBlocking {
        val id = dao.insert(record("Test"))
        assertTrue(id > 0)
    }

    @Test
    fun insertedRecordsAreReflectedInCount() = runBlocking {
        dao.insert(record("A"))
        dao.insert(record("B"))
        dao.insert(record("C"))
        assertEquals(3, dao.count())
    }

    // --- getPage ordering ---

    @Test
    fun getPageReturnsNewestFirst() = runBlocking {
        dao.insert(record("Old",    timestamp = 1_000L))
        dao.insert(record("Middle", timestamp = 2_000L))
        dao.insert(record("New",    timestamp = 3_000L))

        val page = dao.getPage(limit = 3, offset = 0)
        assertEquals("New",    page[0].title)
        assertEquals("Middle", page[1].title)
        assertEquals("Old",    page[2].title)
    }

    @Test
    fun getPageRespectsLimitAndOffset() = runBlocking {
        repeat(10) { i -> dao.insert(record("R$i", timestamp = i.toLong())) }

        val page = dao.getPage(limit = 3, offset = 4)
        assertEquals(3, page.size)
    }

    @Test
    fun getPageOnEmptyTableReturnsEmptyList() = runBlocking {
        assertTrue(dao.getPage(limit = 50, offset = 0).isEmpty())
    }

    // --- deleteAll ---

    @Test
    fun deleteAllRemovesEveryRecord() = runBlocking {
        repeat(5) { dao.insert(record("R$it")) }
        dao.deleteAll()
        assertEquals(0, dao.count())
    }

    @Test
    fun deleteAllOnEmptyTableIsIdempotent() = runBlocking {
        dao.deleteAll()
        assertEquals(0, dao.count())
    }

    // --- pruneOldRecords ---

    @Test
    fun pruneKeepsExactly500NewestRecords() = runBlocking {
        // Insert 502 records with distinct ascending timestamps so order is deterministic
        for (i in 1..502) {
            dao.insert(record("R$i", timestamp = i.toLong()))
        }
        dao.pruneOldRecords()
        assertEquals(500, dao.count())
    }

    @Test
    fun pruneRemovesOldestRecordsFirst() = runBlocking {
        for (i in 1..502) {
            dao.insert(record("R$i", timestamp = i.toLong()))
        }
        dao.pruneOldRecords()

        // The two records with the lowest timestamps should be gone
        val remaining = dao.getPage(limit = 500, offset = 0).map { it.title }.toSet()
        assertFalse("R1 (oldest) should be pruned", remaining.contains("R1"))
        assertFalse("R2 (second oldest) should be pruned", remaining.contains("R2"))
        assertTrue("R502 (newest) should survive", remaining.contains("R502"))
        assertTrue("R3 (third oldest) should survive", remaining.contains("R3"))
    }

    @Test
    fun pruneIsNoOpWhenFewerthan500Records() = runBlocking {
        repeat(10) { dao.insert(record("R$it")) }
        dao.pruneOldRecords()
        assertEquals(10, dao.count())
    }

    // --- helpers ---

    private fun record(title: String, timestamp: Long = System.currentTimeMillis()) =
        NotificationRecord(
            title = title,
            body = "body",
            timestamp = timestamp,
            recordType = "issue_update",
            targetNid = "123",
            targetUrl = "https://drupal.org/node/123",
            isProject = false
        )
}
