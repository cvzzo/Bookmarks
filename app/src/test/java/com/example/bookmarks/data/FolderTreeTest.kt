package com.example.bookmarks.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderTreeTest {
    private val work = Folder(id = 1, name = "Lavoro")
    private val projects = Folder(id = 2, name = "Progetti", parentId = 1)
    private val android = Folder(id = 3, name = "Android", parentId = 2)
    private val articles = Folder(id = 4, name = "Articoli")
    private val all = listOf(work, projects, android, articles)

    @Test
    fun flattenTree_listsChildrenAfterParentSortedByName() {
        assertEquals(
            listOf(articles to 0, work to 0, projects to 1, android to 2),
            flattenTree(all).map { it.folder to it.depth },
        )
    }

    @Test
    fun descendantsAndSelf_includesWholeSubtree() {
        assertEquals(setOf(1L, 2L, 3L), descendantsAndSelf(1, all))
        assertEquals(setOf(4L), descendantsAndSelf(4, all))
    }

    @Test
    fun pathTo_goesFromTopLevelDown() {
        val byId = all.associateBy { it.id }
        assertEquals(listOf(work, projects, android), pathTo(3, byId))
        assertEquals(emptyList<Folder>(), pathTo(null, byId))
    }
}
