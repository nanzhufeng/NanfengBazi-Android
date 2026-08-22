---
name: nanfeng-bazi-testing
description: Select and run safe, sufficient verification for a 南枫八字 Android change. Use for unit tests, static checks, Debug builds, device acceptance planning, and evidence-level reporting.
---

# 南枫八字测试

1. Classify required proof as static, JVM automation, build, isolated device, user device, or real external service. Never collapse levels.
2. Run the smallest relevant test first; for broad changes use `./gradlew test assembleDebug lintDebug` with Android Studio JBR, then `git diff --check`.
3. Algorithm changes require frozen/golden and boundary cases. Room/backup changes require migration, integrity, tamper, and rollback coverage. UI changes require behavior/interaction contracts and visual acceptance where requested.
4. Never execute `connected*AndroidTest`. Do not install, uninstall, clear data, seed databases, or run tests on OPPO/user devices.
5. For authorized coverage installs, verify package, version, debuggable state, certificate, APK hash, and data fingerprint before and after same-signature `pm install -r --user 0`.
6. Report commands, pass/fail result, and what each result cannot prove. Do not claim real OCR, Google/Supabase, or user-data recovery from a successful build.
