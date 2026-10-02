package com.example.bookmarks

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bookmarks.data.extractUrl
import com.example.bookmarks.ui.BookmarksApp
import com.example.bookmarks.ui.SharedLink
import com.example.bookmarks.ui.theme.BookmarksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedLink = intent.toSharedLink()
        setContent {
            BookmarksTheme {
                BookmarksApp(
                    sharedLink = sharedLink,
                    onSharedLinkHandled = { saved ->
                        if (saved) Toast.makeText(this, R.string.link_saved, Toast.LENGTH_SHORT).show()
                        // Opened from another app's share sheet: go back to that app.
                        finish()
                    },
                )
            }
        }
    }
}

private fun Intent.toSharedLink(): SharedLink? {
    if (action != Intent.ACTION_SEND || type?.startsWith("text/") != true) return null
    val text = getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
    if (text.isEmpty()) return null
    val url = extractUrl(text) ?: text
    // Some apps share "Page title https://…": keep the rest of the text as title.
    val title = getStringExtra(Intent.EXTRA_SUBJECT)?.trim()
        ?: text.replace(url, "").trim().trimEnd('.', ',', ':', ';', '-', '–').trim()
    return SharedLink(url, title)
}
