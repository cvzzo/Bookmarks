package com.example.bookmarks.data

private val urlInText = Regex("""https?://[^\s<>"]+""", RegexOption.IGNORE_CASE)
private val validUrl = Regex("""^[a-z][a-z0-9+.\-]*://[^\s/?#]+\S*$""", RegexOption.IGNORE_CASE)

/** Adds `https://` when the user typed an address without a scheme. */
fun normalizeUrl(input: String): String {
    val trimmed = input.trim()
    return if (trimmed.contains("://")) trimmed else "https://$trimmed"
}

fun isValidUrl(input: String): Boolean = input.isNotBlank() && validUrl.matches(normalizeUrl(input))

/** The first http(s) link in shared text such as "Look at this https://example.com." */
fun extractUrl(text: String): String? = urlInText.find(text)?.value?.trimEnd('.', ',', ';', '!', '?')

fun hostOf(url: String): String = url
    .substringAfter("://")
    .substringBefore('/')
    .substringBefore('?')
    .substringBefore('#')
    .substringAfter('@')
    .removePrefix("www.")
