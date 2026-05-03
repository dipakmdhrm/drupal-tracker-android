package com.drupaltracker.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drupaltracker.app.data.model.SeenIssue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IssueDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: IssueDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.issueDao()
    }

    @After
    fun tearDown() = db.close()

    // --- upsert / getIssue ---

    @Test
    fun upsertedIssueCanBeRetrievedByNid() = runBlocking {
        dao.upsert(issue("10"))
        val result = dao.getIssue("10")
        assertNotNull(result)
        assertEquals("Bug title", result!!.title)
        assertEquals("1", result.status)
    }

    @Test
    fun upsertReplacesExistingIssueOnConflict() = runBlocking {
        dao.upsert(issue("1", status = "1", commentCount = 3))
        dao.upsert(issue("1", status = "8", commentCount = 7)) // same nid, updated fields

        val result = dao.getIssue("1")!!
        assertEquals("8", result.status)
        assertEquals(7, result.commentCount)
    }

    @Test
    fun getIssueReturnsNullForUnknownNid() = runBlocking {
        assertNull(dao.getIssue("unknown"))
    }

    // --- updateSummary ---

    @Test
    fun updateSummaryPersistsSummaryAndCommentCount() = runBlocking {
        dao.upsert(issue("1"))
        dao.updateSummary("1", "Cached summary text", 15)

        val result = dao.getIssue("1")!!
        assertEquals("Cached summary text", result.cachedSummary)
        assertEquals(15, result.summarizedCommentCount)
    }

    @Test
    fun updateSummaryDoesNotAffectOtherFields() = runBlocking {
        dao.upsert(issue("1", status = "14", commentCount = 10))
        dao.updateSummary("1", "Summary", 10)

        val result = dao.getIssue("1")!!
        assertEquals("14", result.status)
        assertEquals(10, result.commentCount)
        assertEquals("Bug title", result.title)
    }

    @Test
    fun freshIssueHasNullCachedSummary() = runBlocking {
        dao.upsert(issue("1"))
        assertNull(dao.getIssue("1")?.cachedSummary)
    }

    // --- helpers ---

    private fun issue(
        nid: String,
        status: String = "1",
        commentCount: Int = 0
    ) = SeenIssue(
        nid = nid,
        projectNid = "proj-1",
        title = "Bug title",
        status = status,
        priority = "200",
        changed = 1_000L,
        url = "https://drupal.org/node/$nid",
        commentCount = commentCount
    )
}
