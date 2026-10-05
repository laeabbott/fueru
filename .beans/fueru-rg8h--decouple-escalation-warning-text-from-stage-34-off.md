---
# fueru-rg8h
title: Decouple escalation warning text from STAGE_3/4_OFFSET_MINUTES constants
status: todo
type: task
priority: normal
tags:
    - escalation
    - human:not-needed
created_at: 2026-09-27T09:54:26Z
updated_at: 2026-10-05T09:35:23Z
---

NotificationHelper.notifyEscalationWarning's deadline text hardcodes "fires in 10 minutes"
rather than computing it from STAGE_4_OFFSET_MINUTES - STAGE_3_OFFSET_MINUTES in
EscalationScheduler.kt. Correct today only because those constants happen to be exactly
10 apart (45/55) by design — confirmed NOT actually linked in code: shrinking the gap to
1 minute during round 5's testing produced a visibly wrong "fires at +10min" notification.

Flagged directly in HANDOFF.md's Pivot round 5 section as "not fixed this round." If the
production offsets ever change independently, this text silently goes stale.
