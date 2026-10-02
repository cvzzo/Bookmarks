package com.example.bookmarks.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A saved link; [folderId] is null for links at the top level. */
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("folderId")],
)
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val folderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Link preview (Open Graph data read from the page).
    val description: String? = null,
    val imageUrl: String? = null,
    val siteName: String? = null,
    val faviconUrl: String? = null,
    /** When the preview was last fetched; null = not fetched yet. */
    val previewFetchedAt: Long? = null,
)
