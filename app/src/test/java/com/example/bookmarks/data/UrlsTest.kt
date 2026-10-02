package com.example.bookmarks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlsTest {
    @Test
    fun normalizeUrl_addsHttpsOnlyWhenSchemeIsMissing() {
        assertEquals("https://example.com", normalizeUrl("  example.com "))
        assertEquals("http://example.com/a", normalizeUrl("http://example.com/a"))
    }

    @Test
    fun isValidUrl_rejectsBlankAndTextWithSpaces() {
        assertFalse(isValidUrl(""))
        assertFalse(isValidUrl("ciao mondo"))
        assertTrue(isValidUrl("example.com/path?q=1"))
        assertTrue(isValidUrl("https://example.com"))
    }

    @Test
    fun extractUrl_findsLinkInsideSharedText() {
        assertEquals(
            "https://example.com/a?b=1",
            extractUrl("Guarda questo: https://example.com/a?b=1."),
        )
        assertNull(extractUrl("nessun link qui"))
    }

    @Test
    fun hostOf_stripsSchemePathAndWww() {
        assertEquals("example.com", hostOf("https://www.example.com/path?x=1"))
    }
}
