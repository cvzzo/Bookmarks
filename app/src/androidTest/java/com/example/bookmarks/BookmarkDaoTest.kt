package com.example.bookmarks

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookmarks.data.AppDatabase
import com.example.bookmarks.data.Bookmark
import com.example.bookmarks.data.Folder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
    }

    @After
    fun closeDb() = db.close()

    @Test
    fun deletingFolder_deletesSubfoldersAndTheirLinks() = runBlocking {
        val dao = db.bookmarkDao()
        val work = dao.insert(Folder(name = "Lavoro"))
        val projects = dao.insert(Folder(name = "Progetti", parentId = work))
        dao.insert(Bookmark(url = "https://a.com", title = "A", folderId = projects))
        dao.insert(Bookmark(url = "https://b.com", title = "B"))

        dao.delete(Folder(id = work, name = "Lavoro"))

        assertEquals(emptyList<Folder>(), dao.allFolders().first())
        assertEquals(emptyList<Bookmark>(), dao.bookmarks(projects).first())
        assertEquals(listOf("B"), dao.bookmarks(null).first().map { it.title })
    }

    @Test
    fun subfolders_onlyReturnsDirectChildren() = runBlocking {
        val dao = db.bookmarkDao()
        val work = dao.insert(Folder(name = "Lavoro"))
        val projects = dao.insert(Folder(name = "Progetti", parentId = work))
        dao.insert(Folder(name = "Android", parentId = projects))

        assertEquals(listOf("Lavoro"), dao.subfolders(null).first().map { it.name })
        assertEquals(listOf("Progetti"), dao.subfolders(work).first().map { it.name })
    }
}
