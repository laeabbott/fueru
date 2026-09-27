---
# fueru-30gb
title: Schedule escalation alarms outside of app-open (BOOT_COMPLETED / daily WorkManager)
status: draft
type: feature
priority: low
tags:
    - escalation
created_at: 2026-09-27T09:54:26Z
updated_at: 2026-09-27T09:54:26Z
---

FueruApplication.onCreate() (re)schedules today's escalation alarms every launch — a day
the app is never opened gets no alarms at all. Flagged as "the next gap to close if this
matters in practice" at the end of Pivot round 4 (Escalation Engine) in HANDOFF.md.

Not urgent yet on its own; worth revisiting once round 5 is verified and there's a sense
of how often this gap actually bites in practice.
