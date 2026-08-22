---
name: nanfeng-bazi-code-review
description: Review a 南枫八字 Android change for correctness, privacy, data safety, architecture drift, UI regressions, and missing tests. Use before handoff, release, or when auditing compatibility and backup work.
---

# 南枫八字代码审查

1. Inspect the scoped diff and impacted contracts before reviewing details. Preserve unrelated dirty worktree changes.
2. Check domain single-entry rules, revision ownership, repository-only writes, Room migrations, backup path/hash/size validation, and explicit restore plans.
3. Check that AI/cloud requests are user-initiated, scoped, privacy-preserving, and do not replace local truth; reject secrets and real user data in code, tests, logs, or docs.
4. Review Compose changes for navigation continuity, shape/press consistency, accessibility size, inner/outer foldable separation, and content reachability above floating navigation.
5. For compatibility changes, verify per-pair facts travel from analyzer to history and UI; reject template summaries presented as individualized findings.
6. Require targeted tests for changed failure modes and distinguish test/build evidence from device and external-service evidence.
7. Report only evidence-backed findings with file and line locations; name unresolved uncertainty rather than inventing a defect.
