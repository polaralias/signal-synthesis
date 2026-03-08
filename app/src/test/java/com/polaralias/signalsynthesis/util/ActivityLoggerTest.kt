package com.polaralias.signalsynthesis.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
class ActivityLoggerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        clearPrefs()
        UsageTracker.init(context)
    }

    @After
    fun tearDown() {
        clearPrefs()
        UsageTracker.init(context)
    }

    @Test
    fun `init archives persisted usage before resetting a new day`() {
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

        context.getSharedPreferences("usage_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("last_tracked_day", yesterday)
            .putInt("api_count_day", 4)
            .putInt("provider_Massive_category_DISCOVERY", 3)
            .putInt("provider_Massive_category_ANALYSIS", 1)
            .commit()

        UsageTracker.init(context)

        val archived = UsageTracker.archivedUsage.value.firstOrNull { it.date.toString() == yesterday }
        assertEquals(4, archived?.totalCalls)
        assertEquals(3, archived?.providerBreakdown?.get("Massive")?.get(ApiUsageCategory.DISCOVERY))
        assertEquals(1, archived?.providerBreakdown?.get("Massive")?.get(ApiUsageCategory.ANALYSIS))
        assertEquals(0, UsageTracker.dailyApiCount.value)
        assertTrue(UsageTracker.dailyProviderUsage.value.isEmpty())
    }

    private fun clearPrefs() {
        context.getSharedPreferences("usage_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("usage_archive_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }
}
