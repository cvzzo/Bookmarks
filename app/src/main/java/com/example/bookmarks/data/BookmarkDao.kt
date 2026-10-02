package com.example.bookmarks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** How many items a folder directly contains. */
data class FolderCount(val id: Long, val count: Int)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE")
    fun allFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentId IS :parentId ORDER BY name COLLATE NOCASE")
    fun subfolders(parentId: Long?): Flow<List<Folder>>

    @Query("SELECT * FROM bookmarks WHERE folderId IS :folderId ORDER BY createdAt DESC")
    fun bookmarks(folderId: Long?): Flow<List<Bookmark>>

    @Query("SELECT parentId AS id, COUNT(*) AS count FROM folders WHERE parentId IS NOT NULL GROUP BY parentId")
    fun subfolderCounts(): Flow<List<FolderCount>>

    @Query("SELECT folderId AS id, COUNT(*) AS count FROM bookmarks WHERE folderId IS NOT NULL GROUP BY folderId")
    fun bookmarkCounts(): Flow<List<FolderCount>>


    @Insert
    suspend fun insert(folder: Folder): Long
    @Update
    suspend fun update(folder: Folder)

    @Delete
    suspend fun delete(folder: Folder)

    @Insert
    suspend fun insert(bookmark: Bookmark): Long

    @Update
    suspend fun update(bookmark: Bookmark)

    @Delete
    suspend fun delete(bookmark: Bookmark)

    /**
     * Stores a fetched preview, unless the link's URL was edited meanwhile.
     * The page title replaces the title only when the user did not type one ([fallbackTitle]).
     */
    @Query(
        """
        UPDATE bookmarks SET
            description = :description,
            imageUrl = :imageUrl,
            siteName = :siteName,
            faviconUrl = :faviconUrl,
            previewFetchedAt = :fetchedAt,
            title = CASE WHEN title = :fallbackTitle AND :pageTitle IS NOT NULL THEN :pageTitle ELSE title END
        WHERE id = :id AND url = :url
        """,
    )
    suspend fun updatePreview(
        id: Long,
        url: String,
        fallbackTitle: String,
        pageTitle: String?,
        description: String?,
        imageUrl: String?,
        siteName: String?,
        faviconUrl: String?,
        fetchedAt: Long,
    )
}
