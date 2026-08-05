package com.fueru.app.escalation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.fueru.app.data.AppDatabase
import com.fueru.app.data.DateUtils
import com.fueru.app.data.entity.Charity
import com.fueru.app.data.entity.ConsequenceEvent
import com.fueru.app.notifications.NotificationHelper

/**
 * Stage 4 (§7.2/§7.3/§7.4) — resolves and logs the charity pledge, no payment execution (v1
 * "pledge mode": a link the user taps themselves, per §7.4's own phased-approach recommendation).
 */
object ConsequenceExecutor {

    suspend fun fire(context: Context, database: AppDatabase, practiceId: Long, practiceName: String) {
        val charity = resolveCharity(database, practiceId) ?: return // no charities configured — nothing to pledge to, fails quiet same as the rest of this app's optional-integration code
        val online = isOnline(context)
        val event = ConsequenceEvent(
            practiceId = practiceId,
            timestamp = System.currentTimeMillis(),
            charityId = charity.id,
            charityName = charity.name,
            charityUrl = charity.url,
            queued = !online,
        )
        val eventId = database.consequenceEventDao().insert(event)
        if (online) {
            NotificationHelper.notifyConsequenceReady(context, eventId, practiceName, charity.name)
        } else {
            NotificationHelper.notifyConsequenceQueued(context, practiceName)
            ConsequenceRetryWorker.schedule(context)
        }
    }

    /** §7.3: first miss this week -> a charity from the "glad" list, repeat misses the same week -> "resent". Falls back to whichever list actually has entries if the resolved one is empty, rather than firing nothing. */
    private suspend fun resolveCharity(database: AppDatabase, practiceId: Long): Charity? {
        val weekStart = DateUtils.startOfWeek(DateUtils.todayEpochMillis())
        val firedThisWeek = database.consequenceEventDao().getForPracticeSinceWeekStart(practiceId, weekStart)
        val sentiment = if (firedThisWeek.isEmpty()) "glad" else "resent"
        val list = database.charityDao().getBySentiment(sentiment)
            .ifEmpty { database.charityDao().getBySentiment(if (sentiment == "glad") "resent" else "glad") }
        return list.randomOrNull()
    }

    private fun isOnline(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
