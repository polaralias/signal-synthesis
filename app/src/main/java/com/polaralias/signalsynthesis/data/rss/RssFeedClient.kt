package com.polaralias.signalsynthesis.data.rss

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class RssFeedClient(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val rssDao: RssDao,
    private val parser: RssParser = RssParser()
) {
    /**
     * Fetches the RSS feed at the given URL and updates the local cache.
     * Uses ETag and Last-Modified headers for conditional GET.
     */
    open suspend fun fetchFeed(url: String) = withContext(Dispatchers.IO) {
        val state = rssDao.getFeedState(url)
        val requestBuilder = Request.Builder().url(url)
        
        state?.etag?.let { requestBuilder.header("If-None-Match", it) }
        state?.lastModified?.let { requestBuilder.header("If-Modified-Since", it) }
        
        val request = requestBuilder.build()
        
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.code == 304) {
                    // Not modified, update lastFetchedAt to satisfy TTL or refresh logic
                    state?.let {
                        rssDao.insertFeedState(it.copy(lastFetchedAt = System.currentTimeMillis()))
                    }
                    return@withContext
                }
                
                if (!response.isSuccessful) return@withContext
                
                val body = response.body ?: return@withContext
                val items = parser.parse(body.byteStream(), url)
                
                if (items.isNotEmpty()) {
                    rssDao.insertItems(items)
                }
                
                val newEtag = response.header("ETag")
                val newLastModified = response.header("Last-Modified")
                
                rssDao.insertFeedState(
                    RssFeedStateEntity(
                        feedUrl = url,
                        etag = newEtag,
                        lastModified = newLastModified,
                        lastFetchedAt = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            com.polaralias.signalsynthesis.util.Logger.e("RssFeedClient", "RSS fetch failed for $url: ${e.message}", e)
        }
    }

    /**
     * Fetches raw feed content for verification without caching or side effects.
     */
    open suspend fun fetchRaw(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                return@withContext response.body?.string()
            }
        } catch (e: Exception) {
            com.polaralias.signalsynthesis.util.Logger.e("RssFeedClient", "RSS raw fetch failed for $url: ${e.message}", e)
            return@withContext null
        }
    }
}
