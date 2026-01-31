package com.polaralias.signalsynthesis.data.rss

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class RssParser {
    private val dateFormats = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US), // RSS 2.0
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }, // Atom
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
    )

    fun parse(inputStream: InputStream, feedUrl: String): List<RssItemEntity> {
        inputStream.use {
            val parser = Xml.newPullParser()
            try {
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(it, null)
                parser.nextTag()
                return readFeed(parser, feedUrl)
            } catch (e: Exception) {
                return emptyList()
            }
        }
    }

    private fun readFeed(parser: XmlPullParser, feedUrl: String): List<RssItemEntity> {
        val items = mutableListOf<RssItemEntity>()
        val name = parser.name
        when (name) {
            "rss" -> {
                // RSS 2.0: <rss><channel><item>...
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType != XmlPullParser.START_TAG) continue
                    if (parser.name == "channel") {
                        continue
                    }
                    if (parser.name == "item") {
                        items.add(readRssItem(parser, feedUrl))
                    }
                }
            }
            "feed" -> { // Atom
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType != XmlPullParser.START_TAG) continue
                    if (parser.name == "entry") {
                        items.add(readAtomEntry(parser, feedUrl))
                    }
                }
            }
        }
        return items
    }

    private fun readRssItem(parser: XmlPullParser, feedUrl: String): RssItemEntity {
        var title = ""
        var link = ""
        var description = ""
        var pubDate = 0L
        var guid = ""

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "link" -> link = readText(parser)
                "description" -> description = readText(parser)
                "pubDate" -> pubDate = parseDate(readText(parser))
                "guid" -> guid = readText(parser)
                else -> skip(parser)
            }
        }
        
        val finalGuid = guid.ifEmpty { link }
        return RssItemEntity(
            guidHash = hashString(finalGuid),
            feedUrl = feedUrl,
            title = title,
            link = link,
            publishedAt = if (pubDate == 0L) System.currentTimeMillis() else pubDate,
            snippet = stripHtml(description),
            fetchedAt = System.currentTimeMillis()
        )
    }

    private fun readAtomEntry(parser: XmlPullParser, feedUrl: String): RssItemEntity {
        var title = ""
        var link = ""
        var summary = ""
        var updated = 0L
        var id = ""

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "link" -> {
                    link = parser.getAttributeValue(null, "href") ?: ""
                    parser.nextTag() // Close link tag
                }
                "summary", "content" -> summary = readText(parser)
                "updated", "published" -> if (updated == 0L) updated = parseDate(readText(parser))
                "id" -> id = readText(parser)
                else -> skip(parser)
            }
        }
        
        val finalGuid = id.ifEmpty { link }
        return RssItemEntity(
            guidHash = hashString(finalGuid),
            feedUrl = feedUrl,
            title = title,
            link = link,
            publishedAt = if (updated == 0L) System.currentTimeMillis() else updated,
            snippet = stripHtml(summary),
            fetchedAt = System.currentTimeMillis()
        )
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    private fun parseDate(dateStr: String): Long {
        for (format in dateFormats) {
            try {
                return format.parse(dateStr.trim())?.time ?: 0L
            } catch (e: Exception) {
                // Continue
            }
        }
        return 0L
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
    }

    private fun hashString(input: String): String {
        return try {
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }
}
