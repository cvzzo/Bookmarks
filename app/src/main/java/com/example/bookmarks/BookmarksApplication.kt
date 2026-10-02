package com.example.bookmarks

import android.app.Application
import com.example.bookmarks.data.AppDatabase
import com.example.bookmarks.data.LinkPreviewRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BookmarksApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val previews: LinkPreviewRepository by lazy { LinkPreviewRepository(database.bookmarkDao(), appScope) }

    override fun onCreate() {
        super.onCreate()
        // Preview images and page screenshots saved by earlier versions are no longer used.
        appScope.launch(Dispatchers.IO) { File(filesDir, "snapshots").deleteRecursively() }
    }
}
