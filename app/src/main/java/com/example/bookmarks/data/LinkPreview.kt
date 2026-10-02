package com.example.bookmarks.data

import java.io.IOException
import java.net.URI
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.UnsupportedMimeTypeException
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** What WhatsApp & co. show for a link: read from the page's Open Graph / Twitter / HTML tags. */
data class LinkPreview(
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val siteName: String? = null,
    val faviconUrl: String? = null,
)


/**
 * Identities tried in order. Many sites (Instagram, shops behind bot protection…) only send
 * preview tags, or only answer at all, to the crawlers of chat apps: the same trick WhatsApp
 * relies on. The browser identity comes last, for sites that treat crawlers differently.
 */
private val USER_AGENTS = listOf(
    "WhatsApp/2.24.20.89 A",
    "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)",
    "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Mobile Safari/537.36",
)

private const val TIMEOUT_MS = 10_000

private fun connect(url: String, userAgent: String) = Jsoup.connect(url)
    .userAgent(userAgent)
    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/*,*/*;q=0.8")
    .header("Accept-Language", "${Locale.getDefault().toLanguageTag()},en;q=0.8")
    .timeout(TIMEOUT_MS)

fun parsePreview(doc: Document): LinkPreview {
    fun meta(vararg keys: String): Element? = keys.firstNotNullOfOrNull { key ->
        doc.selectFirst("meta[property=\"$key\"], meta[name=\"$key\"]")
            ?.takeIf { it.attr("content").isNotBlank() }
    }
    fun text(vararg keys: String) = meta(*keys)?.attr("content")?.trim()
    fun url(vararg keys: String) = meta(*keys)?.absUrl("content")?.ifBlank { null }

    val favicon = (doc.selectFirst("link[rel~=(?i)apple-touch-icon]") ?: doc.selectFirst("link[rel~=(?i)^(shortcut )?icon$]"))
        ?.absUrl("href")?.ifBlank { null }
        ?: runCatching { URI(doc.location()).let { "${it.scheme}://${it.host}/favicon.ico" } }.getOrNull()

    return LinkPreview(
        title = text("og:title", "twitter:title") ?: doc.title().trim().ifBlank { null },
        description = text("og:description", "twitter:description", "description"),
        imageUrl = url("og:image:secure_url", "og:image", "og:image:url", "twitter:image", "twitter:image:src"),
        siteName = text("og:site_name", "application-name"),
        faviconUrl = favicon,
    )
}

/**
 * Downloads [url] and reads its preview, trying each of [USER_AGENTS] until one gives a title.
 * Returns an empty preview when no identity gets one (404, blocked, not
 * HTML…) and null on network errors, so that it is retried later.
 */
suspend fun fetchPreview(url: String): LinkPreview? = withContext(Dispatchers.IO) {
    var best: LinkPreview? = null
    for (userAgent in USER_AGENTS) {
        try {
            val preview = parsePreview(connect(url, userAgent).get())
            if (preview.title != null) return@withContext preview
            if (best == null) best = preview
        } catch (_: HttpStatusException) {
            // Blocked or missing for this identity: try the next one.
        } catch (_: UnsupportedMimeTypeException) {
            return@withContext LinkPreview()
        } catch (_: IllegalArgumentException) {
            return@withContext LinkPreview()
        } catch (_: IOException) {
            // Offline or timeout: retry the whole fetch later.
            return@withContext best
        }
    }
    best ?: LinkPreview()
}

/** Fetches previews in the background (app-wide scope, so they finish even if the screen closes). */
class LinkPreviewRepository(
    private val dao: BookmarkDao,
    private val scope: CoroutineScope,
) {
    private val inFlight = ConcurrentHashMap.newKeySet<Long>()
    /** Link id -> URL fetched in this session; screens may still hold the stale "not fetched" row. */
    private val fetched = ConcurrentHashMap<Long, String>()

    fun fetchMissing(bookmarks: List<Bookmark>) {
        bookmarks
            .filter { it.previewFetchedAt == null && fetched[it.id] != it.url }
            .forEach(::refresh)
    }

    fun refresh(bookmark: Bookmark) {
        if (!inFlight.add(bookmark.id)) return
        scope.launch {
            try {
                fetched[bookmark.id] = bookmark.url
                val preview = fetchPreview(bookmark.url) ?: run {
                    fetched.remove(bookmark.id) // network error: allow a retry
                    return@launch
                }
                dao.updatePreview(
                    id = bookmark.id,
                    url = bookmark.url,
                    fallbackTitle = hostOf(bookmark.url),
                    pageTitle = preview.title,
                    description = preview.description,
                    imageUrl = null,
                    siteName = preview.siteName,
                    faviconUrl = preview.faviconUrl,
                    fetchedAt = System.currentTimeMillis(),
                )
            } finally {
                inFlight.remove(bookmark.id)
            }
        }
    }
}
