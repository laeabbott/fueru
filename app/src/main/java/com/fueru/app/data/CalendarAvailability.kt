package com.fueru.app.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BusyBlock(val title: String, val startMillis: Long, val endMillis: Long)

/**
 * Busy blocks from the device calendar for the 24h window starting at [dayStartMillis] — feeds
 * This Week's time-picker so scheduling a workout can avoid an obvious conflict. Returns an empty
 * list (not an error) when READ_CALENDAR isn't granted: the permission is only requested
 * contextually in onboarding and may have been declined, or revoked since in system settings, so a
 * live check is the only correct source of truth (same reasoning as the notification permission
 * check in notifications/NotificationHelper.kt).
 */
suspend fun busyBlocksForDay(context: Context, dayStartMillis: Long): List<BusyBlock> = withContext(Dispatchers.IO) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        return@withContext emptyList()
    }
    val dayEndMillis = dayStartMillis + 86_400_000L
    val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(uriBuilder, dayStartMillis)
    ContentUris.appendId(uriBuilder, dayEndMillis)
    val projection = arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
    )
    val blocks = mutableListOf<BusyBlock>()
    context.contentResolver.query(uriBuilder.build(), projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
        val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
        val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
        val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
        while (cursor.moveToNext()) {
            blocks.add(
                BusyBlock(
                    title = cursor.getString(titleIdx) ?: "Busy",
                    startMillis = cursor.getLong(beginIdx),
                    endMillis = cursor.getLong(endIdx),
                ),
            )
        }
    }
    blocks
}

/** Combines Android's CalendarContract (synced calendars) with any imported .ics file (Proton Calendar etc. — see IcsCalendarStore, which doesn't sync into CalendarContract). This is what UI code should call, not [busyBlocksForDay] alone. */
suspend fun allBusyBlocksForDay(context: Context, dayStartMillis: Long): List<BusyBlock> {
    val deviceBlocks = busyBlocksForDay(context, dayStartMillis)
    val icsBlocks = IcsCalendarStore.busyBlocksForDay(context, dayStartMillis)
    return (deviceBlocks + icsBlocks).sortedBy { it.startMillis }
}
