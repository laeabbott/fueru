package com.fueru.app.escalation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fueru.app.notifications.NotificationHelper
import com.fueru.app.ui.screens.ResistanceFlowScreen
import com.fueru.app.ui.theme.FueruTheme

/**
 * Stage 2's actual lock screen — project brief §7.2/§8.2. Draws over the lock screen and wakes it
 * (setShowWhenLocked/setTurnScreenOn, API 27+; older window flags below that), matching how a real
 * alarm-clock app behaves, without needing SYSTEM_ALERT_WINDOW (see the Phase 4 plan's "one
 * deliberate divergence" note).
 *
 * Dismissal condition, exactly per spec: reaching Ignite, not just opening this screen. System back
 * is disabled (BackHandler consumes it, no-op) until ResistanceFlowScreen's onIgnited fires — after
 * that the lock's job is done and the flow behaves like any other entry (back just abandons the
 * session, same as everywhere else).
 */
class EscalationLockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }

        val practiceId = intent.getLongExtra(EXTRA_PRACTICE_ID, -1L)
        if (practiceId == -1L) {
            finish()
            return
        }

        setContent {
            var ignited by remember { mutableStateOf(false) }
            FueruTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BackHandler(enabled = !ignited) {
                        // Consumed, deliberately no-op — locked until Ignite, per spec.
                    }
                    ResistanceFlowScreen(
                        practiceId = practiceId,
                        // Always the full flow here, even if this practice's fade offer (§6.3) was
                        // already accepted — reaching Stage 2 means the lighter-touch short-flow
                        // already failed to get it done today, so the lock deliberately doesn't
                        // offer that shortcut. Own call, flagged in the Phase 4 plan.
                        startAtIgnite = false,
                        onIgnited = {
                            ignited = true
                            NotificationHelper.cancelEscalationNotification(this@EscalationLockActivity, practiceId)
                            EscalationLockService.stop(this@EscalationLockActivity)
                        },
                        onDone = { finish() },
                    )
                }
            }
        }
    }

    companion object {
        fun intentFor(context: Context, practiceId: Long, practiceName: String): Intent =
            Intent(context, EscalationLockActivity::class.java).apply {
                putExtra(EXTRA_PRACTICE_ID, practiceId)
                putExtra(EXTRA_PRACTICE_NAME, practiceName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
    }
}
