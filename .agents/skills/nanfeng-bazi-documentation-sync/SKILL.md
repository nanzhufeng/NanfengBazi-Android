---
name: nanfeng-bazi-documentation-sync
description: Synchronize 南枫八字 Android documentation, handoff, decisions, and reusable workflow notes from current code, tests, configuration, and Git evidence. Use after a verified increment or during project audit.
---

# 南枫八字文档同步

1. Start with current code, configuration, tests, Git history, and recorded verification; use handoff notes only as leads.
2. Separate durable contracts (`AGENTS.md`, architecture, domain, data, testing) from temporary status (`CURRENT_HANDOFF.md`) and one-time decisions (`decision-log.md`).
3. Record evidence source, validation level, version/commit scope, known limits, and code-document conflicts. Code and verified evidence override stale prose.
4. Never include real cases, attachments, OCR source text, credentials, recovery codes, tokens, or cloud ciphertext.
5. Do not mark a build as device acceptance, or an installation as real cloud/OCR/recovery completion.
6. Keep long-term instructions concise and avoid copying the same rule among AGENTS, skills, and product documents; link to the authority instead.
7. After edits, check links/references, run `git diff --check`, and report changed files plus unresolved facts.
