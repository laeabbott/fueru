---
# fueru-xs0k
title: Verify Pivot round 5 (Stage 3/4 consequences / pledge mode) on-device
status: in-progress
type: feature
priority: high
tags:
    - pivot
    - human:needed
created_at: 2026-09-27T09:54:16Z
updated_at: 2026-10-05T09:35:23Z
---

Rounds 1-4 of the practices pivot each closed with an explicit "fully verified end-to-end
on-device" note in HANDOFF.md. Round 5 (Stage 3/4 consequences, "pledge mode") does not —
it's the one round without that closing confirmation, which makes it the actual open item
right now rather than backlog.

What needs a real on-device pass (see HANDOFF.md's "Pivot round 5" section for full context):

- The "stakes" section on PracticeDetailScreen (charity toggle) and CharitiesScreen (manage
  glad/resent lists)
- Stage 3 firing at +45min (visible countdown notification) and Stage 4 at +55min
  (ConsequenceExecutor: charity resolution, ConsequencePledgeScreen, the manual
  "I did this" completion button)
- The offline path specifically: ConsequenceRetryWorker firing once connectivity returns,
  not just the online path
- Confirm the one self-caught bug (inert Remove button, fixed before ever building) actually
  behaves correctly live, since it was never exercised on-device

No Android SDK on this dev machine — same standing constraint as the rest of fueru. Report
the first real build/runtime error verbatim if one shows up.
