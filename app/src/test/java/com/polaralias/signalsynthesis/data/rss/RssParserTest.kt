package com.polaralias.signalsynthesis.data.rss

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class RssParserTest {
    private val parser = RssParser()

    @Test
    fun `parse RSS 2_0 correctly`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <rss version="2.0">
            <channel>
                <title>Test Feed</title>
                <item>
                    <title>Apple shares surge $2</title>
                    <link>https://example.com/aapl</link>
                    <description><![CDATA[<p>AAPL is doing well.</p>]]></description>
                    <pubDate>Fri, 30 Jan 2026 10:00:00 +0000</pubDate>
                    <guid>aapl-1</guid>
                </item>
                <item>
                    <title>Tesla announcement</title>
                    <link>https://example.com/tsla</link>
                    <description>TSLA news.</description>
                    <pubDate>Fri, 30 Jan 2026 11:00:00 +0000</pubDate>
                </item>
            </channel>
            </rss>
        """.trimIndent()
        
        val items = parser.parse(ByteArrayInputStream(xml.toByteArray()), "https://example.com/feed")
        
        assertEquals(2, items.size)
        assertEquals("Apple shares surge $2", items[0].title)
        assertEquals("AAPL is doing well.", items[0].snippet)
        assertEquals("https://example.com/aapl", items[0].link)
        assertEquals("https://example.com/feed", items[0].feedUrl)
        assertTrue(items[0].publishedAt > 0)
    }

    @Test
    fun `parse Atom correctly`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <title>Test Atom</title>
                <entry>
                    <title>Microsoft acquisition</title>
                    <link href="https://example.com/msft"/>
                    <id>msft-1</id>
                    <updated>2026-01-30T10:00:00Z</updated>
                    <summary>MSFT buys a company.</summary>
                </entry>
            </feed>
        """.trimIndent()
        
        val items = parser.parse(ByteArrayInputStream(xml.toByteArray()), "https://example.com/atom")
        
        assertEquals(1, items.size)
        assertEquals("Microsoft acquisition", items[0].title)
        assertEquals("MSFT buys a company.", items[0].snippet)
        assertEquals("https://example.com/msft", items[0].link)
        assertEquals("https://example.com/atom", items[0].feedUrl)
    }

    @Test
    fun `strip HTML from snippet`() {
        val xml = """
            <rss version="2.0"><channel><item>
                <title>HTML Test</title>
                <description>&lt;p&gt;This is &lt;b&gt;bold&lt;/b&gt; text.&lt;/p&gt;</description>
            </item></channel></rss>
        """.trimIndent()
        
        val items = parser.parse(ByteArrayInputStream(xml.toByteArray()), "url")
        
        assertEquals("This is bold text.", items[0].snippet)
    }
}
