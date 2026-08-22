---
name: nanfeng-bazi-development
description: Implement a scoped 南枫八字 Android feature or UI change. Use for Kotlin, Compose, domain, Room, backup, import, compatibility, AI, or cloud changes that must preserve project architecture and regression gates.
---

# 南枫八字开发

1. Read the relevant requirement, architecture, domain, test, and current handoff documents before editing.
2. Trace the existing flow from domain contract through repository/use case, `StageTwoViewModel`, and Compose. Reuse it; do not create a second calculator, direct Room write, backup parser, or cloud pipeline.
3. Keep calculation in `BaziEngine.calculate()`, persistence in `CaseRepository`, and backup/cloud snapshots in `CaseBackupService`.
4. Bind detail child state to `caseId + revision`; preserve source/calculated/adopted distinctions and user data.
5. For foldable layout work, define inner and outer screen rules separately. Do not let a shared offset change both postures.
6. Add the narrowest regression test that proves changed behavior, then run the matching JVM/build checks.
7. Never run `connected*AndroidTest`; use OPPO only under explicit user-authorized, same-signature main-APK coverage rules.
