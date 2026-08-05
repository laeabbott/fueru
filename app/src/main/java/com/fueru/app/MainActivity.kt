package com.fueru.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.fueru.app.ui.navigation.FueruNavGraph
import com.fueru.app.ui.theme.FueruTheme

class MainActivity : ComponentActivity() {

    private var pendingResistanceFlowPracticeId by mutableStateOf<Long?>(null)
    private var pendingConsequenceEventId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingResistanceFlowPracticeId = practiceIdFrom(intent)
        pendingConsequenceEventId = consequenceEventIdFrom(intent)
        setContent {
            FueruTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    FueruNavGraph(
                        navController = navController,
                        pendingResistanceFlowPracticeId = pendingResistanceFlowPracticeId,
                        onPendingResistanceFlowConsumed = { pendingResistanceFlowPracticeId = null },
                        pendingConsequenceEventId = pendingConsequenceEventId,
                        onPendingConsequenceConsumed = { pendingConsequenceEventId = null },
                    )
                }
            }
        }
    }

    /** A Stage 0/1/4 (or offline-resolved) notification tap re-delivers here (MainActivity is already running in the common case) rather than triggering a fresh onCreate — see NotificationHelper's PendingIntents. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        practiceIdFrom(intent)?.let { pendingResistanceFlowPracticeId = it }
        consequenceEventIdFrom(intent)?.let { pendingConsequenceEventId = it }
    }

    private fun practiceIdFrom(intent: Intent): Long? =
        intent.getLongExtra(EXTRA_OPEN_RESISTANCE_FLOW_PRACTICE_ID, -1L).takeIf { it != -1L }

    private fun consequenceEventIdFrom(intent: Intent): Long? =
        intent.getLongExtra(EXTRA_OPEN_CONSEQUENCE_EVENT_ID, -1L).takeIf { it != -1L }

    companion object {
        const val EXTRA_OPEN_RESISTANCE_FLOW_PRACTICE_ID = "openResistanceFlowPracticeId"
        const val EXTRA_OPEN_CONSEQUENCE_EVENT_ID = "openConsequenceEventId"
    }
}
