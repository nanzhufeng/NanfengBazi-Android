---
name: nanfeng-bazi-debugging
description: Diagnose a 南枫八字 Android defect with reproducible evidence before any fix. Use for data loss appearances, compatibility, backup, import, cloud, navigation, foldable layout, or Compose interaction failures.
---

# 南枫八字排错

1. State the observed symptom, scope, evidence layer, and whether it is inner screen, outer screen, device-specific, or data-specific.
2. Reproduce with safe local inputs first. Follow the real path: domain → data → ViewModel → UI, and inspect IDs, revisions, insets, and layout constraints rather than guessing from a screenshot.
3. Treat cached absence as unknown until the repository is checked. Treat coroutine cancellation separately from external failure.
4. For layout defects, measure the actual anchors and account for system insets exactly once. Keep inner/outer posture parameters separate and change one controlled variable at a time.
5. For compatibility, verify input snapshots, analyzer signals, persisted history, and displayed wording independently; fixed UI copy is not proof of a personalized result.
6. Add or update a regression test only after the root cause is identified. Report root cause, change, automated evidence, and unverified device/service scope separately.
7. Do not run instrumentation on a user device or touch real user data while diagnosing.
