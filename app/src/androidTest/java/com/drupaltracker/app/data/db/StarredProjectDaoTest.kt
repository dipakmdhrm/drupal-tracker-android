package com.drupaltracker.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drupaltracker.app.data.model.StarredProject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StarredProjectDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: StarredProjectDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.starredProjectDao()
    }

    @After
    fun tearDown() = db.close()

    // --- upsert / getProject ---

    @Test
    fun upsertedProjectCanBeRetrievedByNid() = runBlocking {
        dao.upsert(project("42", "views", "Views"))
        val result = dao.getProject("42")
        assertNotNull(result)
        assertEquals("views", result!!.machineName)
        assertEquals("Views", result.title)
    }

    @Test
    fun upsertReplacesExistingProjectWithSameNid() = runBlocking {
        dao.upsert(project("1", "views", "Views"))
        dao.upsert(project("1", "views", "Views (Updated)"))

        assertEquals("Views (Updated)", dao.getProject("1")?.title)
        assertEquals(1, dao.getAllProjectsOnce().size)
    }

    @Test
    fun getProjectReturnsNullForUnknownNid() = runBlocking {
        assertNull(dao.getProject("nonexistent"))
    }

    // --- deleteByNid ---

    @Test
    fun deleteByNidRemovesProject() = runBlocking {
        dao.upsert(project("1", "views", "Views"))
        dao.deleteByNid("1")
        assertNull(dao.getProject("1"))
    }

    @Test
    fun deleteByNidOnAbsentNidIsIdempotent() = runBlocking {
        dao.upsert(project("1", "views", "Views"))
        dao.deleteByNid("999")
        assertNotNull(dao.getProject("1"))
    }

    // --- updateLastChecked ---

    @Test
    fun updateLastCheckedOnlyModifiesTimestamp() = runBlocking {
        dao.upsert(project("1", "views", "Views", lastChecked = 0L))
        dao.updateLastChecked("1", 99_999L)

        val result = dao.getProject("1")!!
        assertEquals(99_999L, result.lastChecked)
        assertEquals("Views", result.title) // unchanged
    }

    // --- getAllProjectsOnce ---

    @Test
    fun getAllProjectsOnceReturnsAllRows() = runBlocking {
        dao.upsert(project("1", "views", "Views"))
        dao.upsert(project("2", "token", "Token"))
        dao.upsert(project("3", "ctools", "CTools"))

        assertEquals(3, dao.getAllProjectsOnce().size)
    }

    @Test
    fun getAllProjectsOnceOnEmptyTableReturnsEmptyList() = runBlocking {
        assertTrue(dao.getAllProjectsOnce().isEmpty())
    }

    // --- helpers ---

    private fun project(nid: String, machineName: String, title: String, lastChecked: Long = 0L) =
        StarredProject(
            nid = nid,
            machineName = machineName,
            title = title,
            lastChecked = lastChecked,
            starredAt = 0L
        )
}
