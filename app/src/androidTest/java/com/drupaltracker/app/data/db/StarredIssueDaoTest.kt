package com.drupaltracker.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drupaltracker.app.data.model.StarredIssue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StarredIssueDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: StarredIssueDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.starredIssueDao()
    }

    @After
    fun tearDown() = db.close()

    // --- upsert / getIssue ---

    @Test
    fun upsertedIssueCanBeRetrievedByNid() = runBlocking {
        dao.upsert(issue("100"))
        val result = dao.getIssue("100")
        assertNotNull(result)
        assertEquals("Test issue", result!!.title)
    }

    @Test
    fun upsertReplacesExistingIssueWithSameNid() = runBlocking {
        dao.upsert(issue("1", title = "Original"))
        dao.upsert(issue("1", title = "Updated"))

        assertEquals("Updated", dao.getIssue("1")?.title)
        assertEquals(1, dao.getAllIssuesOnce().size)
    }

    @Test
    fun getIssueReturnsNullForUnknownNid() = runBlocking {
        assertNull(dao.getIssue("nonexistent"))
    }

    // --- deleteByNid ---

    @Test
    fun deleteByNidRemovesIssue() = runBlocking {
        dao.upsert(issue("1"))
        dao.deleteByNid("1")
        assertNull(dao.getIssue("1"))
    }

    // --- updateChanged ---

    @Test
    fun updateChangedOnlyModifiesChangedField() = runBlocking {
        dao.upsert(issue("1", changed = 100L))
        dao.updateChanged("1", 999L)

        val result = dao.getIssue("1")!!
        assertEquals(999L, result.changed)
        assertEquals("Test issue", result.title) // unchanged
        assertEquals("1", result.status)          // unchanged
    }

    // --- updateSummary ---

    @Test
    fun updateSummaryPersistsSummaryAndCommentCount() = runBlocking {
        dao.upsert(issue("1"))
        dao.updateSummary("1", "Great summary", 42)

        val result = dao.getIssue("1")!!
        assertEquals("Great summary", result.cachedSummary)
        assertEquals(42, result.summarizedCommentCount)
    }

    @Test
    fun updateSummaryDoesNotAffectOtherFields() = runBlocking {
        dao.upsert(issue("1", title = "My Issue", changed = 500L))
        dao.updateSummary("1", "Summary text", 7)

        val result = dao.getIssue("1")!!
        assertEquals("My Issue", result.title)
        assertEquals(500L, result.changed)
    }

    // --- getAllIssuesOnce ---

    @Test
    fun getAllIssuesOnceReturnsAllRows() = runBlocking {
        dao.upsert(issue("1"))
        dao.upsert(issue("2"))
        assertEquals(2, dao.getAllIssuesOnce().size)
    }

    // --- helpers ---

    private fun issue(
        nid: String,
        title: String = "Test issue",
        changed: Long = 1_000L
    ) = StarredIssue(
        nid = nid,
        projectNid = "proj-1",
        projectTitle = "Test Project",
        title = title,
        status = "1",
        priority = "200",
        changed = changed,
        url = "https://drupal.org/node/$nid",
        commentCount = 0,
        starredAt = 0L
    )
}
