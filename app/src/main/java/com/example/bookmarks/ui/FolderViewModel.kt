package com.example.bookmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.bookmarks.BookmarksApplication
import com.example.bookmarks.data.Bookmark
import com.example.bookmarks.data.BookmarkDao
import com.example.bookmarks.data.Folder
import com.example.bookmarks.data.LinkPreviewRepository
import com.example.bookmarks.data.hostOf
import com.example.bookmarks.data.pathTo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FolderUiState(
    val loading: Boolean = true,
    /** From the top level down to the current folder; empty at the top level. */
    val path: List<Folder> = emptyList(),
    val subfolders: List<Folder> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val allFolders: List<Folder> = emptyList(),
    /** Folder id -> number of direct subfolders / links. */
    val subfolderCounts: Map<Long, Int> = emptyMap(),
    val bookmarkCounts: Map<Long, Int> = emptyMap(),
) {
    val folder: Folder? get() = path.lastOrNull()
}

/** Contents of one folder; [folderId] null means the top level. */
class FolderViewModel(
    private val dao: BookmarkDao,
    private val previews: LinkPreviewRepository,
    private val folderId: Long?,
) : ViewModel() {

    val uiState: StateFlow<FolderUiState> = combine(
        dao.allFolders(),
        dao.subfolders(folderId),
        dao.bookmarks(folderId).onEach(previews::fetchMissing),
        dao.subfolderCounts(),
        dao.bookmarkCounts(),
    ) { all, subfolders, bookmarks, subfolderCounts, bookmarkCounts ->
        FolderUiState(
            loading = false,
            path = pathTo(folderId, all.associateBy { it.id }),
            subfolders = subfolders,
            bookmarks = bookmarks,
            allFolders = all,
            subfolderCounts = subfolderCounts.associate { it.id to it.count },
            bookmarkCounts = bookmarkCounts.associate { it.id to it.count },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FolderUiState())

    fun createFolder(name: String, color: Int?) = viewModelScope.launch {
        dao.insert(Folder(name = name.trim(), parentId = folderId, color = color))
    }

    fun editFolder(folder: Folder, name: String, color: Int?) = viewModelScope.launch {
        dao.update(folder.copy(name = name.trim(), color = color))
    }

    fun moveFolder(folder: Folder, parentId: Long?) = viewModelScope.launch {
        dao.update(folder.copy(parentId = parentId))
    }

    fun deleteFolder(folder: Folder) = viewModelScope.launch { dao.delete(folder) }

    /** Inserts a new link when [existing] is null, otherwise updates it. */
    fun saveBookmark(existing: Bookmark?, url: String, title: String, folderId: Long?): Job =
        viewModelScope.launch {
            val finalTitle = title.trim().ifBlank { hostOf(url) }
            if (existing == null) {
                dao.insert(Bookmark(url = url, title = finalTitle, folderId = folderId))
            } else {
                val edited = existing.copy(url = url, title = finalTitle, folderId = folderId)
                // A different address means a different page: drop the old preview.
                dao.update(if (url == existing.url) edited else edited.withoutPreview())
            }
        }

    fun moveBookmark(bookmark: Bookmark, folderId: Long?) = viewModelScope.launch {
        dao.update(bookmark.copy(folderId = folderId))
    }

    fun refreshPreview(bookmark: Bookmark) = previews.refresh(bookmark)

    fun deleteBookmark(bookmark: Bookmark) = viewModelScope.launch { dao.delete(bookmark) }

    /** Undo for [deleteBookmark]; silently skipped if its folder has been deleted meanwhile. */
    fun restoreBookmark(bookmark: Bookmark) = viewModelScope.launch {
        runCatching { dao.insert(bookmark) }
    }

    companion object {
        fun factory(folderId: Long?) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as BookmarksApplication
                FolderViewModel(app.database.bookmarkDao(), app.previews, folderId)
            }
        }
    }
}

private fun Bookmark.withoutPreview() = copy(
    description = null,
    imageUrl = null,
    siteName = null,
    faviconUrl = null,
    previewFetchedAt = null,
)
