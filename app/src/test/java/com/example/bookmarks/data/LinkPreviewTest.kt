package com.example.bookmarks.data

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkPreviewTest {
    @Test
    fun parsePreview_readsOpenGraphTagsAndResolvesRelativeUrls() {
        val html = """
            <html><head>
              <title>Fallback title</title>
              <meta property="og:title" content="Jetpack Compose">
              <meta property="og:description" content="Build better apps faster">
              <meta property="og:image" content="/images/cover.png">
              <meta property="og:site_name" content="Android Developers">
              <link rel="icon" href="/favicon-32.png">
            </head></html>
        """.trimIndent()

        val preview = parsePreview(Jsoup.parse(html, "https://developer.android.com/compose"))

        assertEquals("Jetpack Compose", preview.title)
        assertEquals("Build better apps faster", preview.description)
        assertEquals("https://developer.android.com/images/cover.png", preview.imageUrl)
        assertEquals("Android Developers", preview.siteName)
        assertEquals("https://developer.android.com/favicon-32.png", preview.faviconUrl)
    }

    @Test
    fun parsePreview_fallsBackToHtmlTitleAndDefaultFavicon() {
        val html = """<html><head><title> Plain page </title><meta name="description" content="Desc"></head></html>"""

        val preview = parsePreview(Jsoup.parse(html, "https://example.com/a/b"))

        assertEquals("Plain page", preview.title)
        assertEquals("Desc", preview.description)
        assertNull(preview.imageUrl)
        assertEquals("https://example.com/favicon.ico", preview.faviconUrl)
    }
}
