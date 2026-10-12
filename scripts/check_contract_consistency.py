#!/usr/bin/env python3
"""Read-only contract checks using CONTRACT_INDEX as the sole scope registry."""
import argparse
import json
import re
from pathlib import Path
from urllib.parse import unquote

INDEX = "docs/CONTRACT_INDEX.md"
LINK = re.compile(r"\[[^\]]+\]\(([^)]+)\)")
ROLES = {"现行", "审计", "历史", "接续"}


def local_target(root, parent, target):
    if target.startswith(("https:", "http:", "mailto:", "codex:")):
        return None
    local, _, anchor = target.strip("<>").partition("#")
    path = (parent / unquote(local)).resolve()
    try:
        path.relative_to(root)
    except ValueError as error:
        raise ValueError("local link escapes project") from error
    return path, unquote(anchor)


def anchors(text):
    result, counts, fenced = set(), {}, False
    for line in text.splitlines():
        if line.lstrip().startswith(("```", "~~~")):
            fenced = not fenced
            continue
        heading = re.match(r"^ {0,3}#{1,6}\s+(.+?)\s*#*\s*$", line)
        if fenced or not heading:
            continue
        value = re.sub(r"<[^>]+>", "", heading[1]).lower()
        slug = re.sub(r"[^\w\- ]", "", value).replace(" ", "-")
        count = counts.get(slug, 0)
        counts[slug] = count + 1
        result.add(slug + (f"-{count}" if count else ""))
    return result


def command_blocks(text):
    blocks, current, fenced = [], [], False
    for line in text.splitlines():
        if line.lstrip().startswith(("```", "~~~")):
            if fenced:
                blocks.append("\n".join(current))
                current = []
            fenced = not fenced
        elif fenced:
            if not line.lstrip().startswith("#"):
                current.append(line)
        else:
            blocks.extend(re.findall(r"`([^`]+)`", line))
            if re.search(r"(?:^|\s)(?:\.[/\\])?gradlew?(?:\.bat)?\s", line):
                blocks.append(line)
    if current:
        blocks.append("\n".join(current))
    return blocks


def skill_ui_complete(path):
    if not path.is_file():
        return False
    text = path.read_text(encoding="utf-8")
    for key in ("display_name", "short_description", "default_prompt"):
        value = re.search(r"^[ \t]*" + key + r":[ \t]*([^\n]*)$", text, re.M)
        if not value:
            return False
        raw = value[1].strip()
        if not raw or raw.startswith("#") or not raw.strip("\"'").strip():
            return False
    return True


def check(root):
    root = root.resolve()
    errors, paths, links = [], [INDEX], 0
    def fail(name, issue, **extra):
        errors.append(dict(file=name, issue=issue, **extra))

    registry = root / INDEX
    if registry.is_file():
        for number, line in enumerate(registry.read_text(encoding="utf-8").splitlines(), 1):
            if not line.startswith("| "):
                continue
            columns = [value.strip() for value in line.strip("|").split("|")]
            if columns[0] == "合同/信息" or all(re.fullmatch(r"[-: ]+", x) for x in columns):
                continue
            if len(columns) != 4 or columns[-1] not in ROLES:
                fail(INDEX, "invalid contract registry row", line=number)
                continue
            if columns[-1] in {"现行", "审计"}:
                targets = LINK.findall(columns[1])
                if not targets:
                    fail(INDEX, "active registry row has no contract target", line=number)
                for target in targets:
                    try:
                        resolved = local_target(root, registry.parent, target)
                        if resolved is None:
                            fail(INDEX, "active contract must be local", line=number)
                        else:
                            paths.append(str(resolved[0].relative_to(root)))
                    except ValueError:
                        fail(INDEX, "active contract escapes project", line=number)
    if "AGENTS.md" not in paths or "docs/DATA_CONTRACT.md" not in paths:
        fail(INDEX, "required safety/data owner missing from active registry")
    paths = list(dict.fromkeys(paths))
    for name in paths:
        path = root / name
        if not path.is_file():
            fail(name, "missing active contract")
            continue
        text = path.read_text(encoding="utf-8")
        for block in command_blocks(text):
            if re.search(r"(?:^|[\s/\\])gradlew?(?:\.bat)?\b", block) and re.search(r"(?:^|[\s:])connected[\w*]*AndroidTest\b", block):
                fail(name, "connected test invocation")
        for number, line in enumerate(text.splitlines(), 1):
            if re.search(r"(?:OPPO|主设备).*?(?:默认允许|可以自动|允许自动).*?(?:启动|打开)", line) or re.search(r"(?:默认允许|允许自动).*?(?:OPPO|主设备).*?自动(?:启动|打开)", line):
                fail(name, "automatic primary-device launch", line=number)
            if "Manifest" in line and "无联网权限" in line and not any(word in line for word in ("不成立", "不代表")):
                fail(name, "stale no-network Manifest claim", line=number)
        for target in LINK.findall(text):
            try:
                resolved = local_target(root, path.parent, target)
            except ValueError:
                fail(name, "local link escapes project", target=target)
                continue
            if resolved is None:
                continue
            destination, anchor = resolved
            # A pure #fragment belongs to the current document.
            if target.startswith("#"):
                destination = path
            links += 1
            if not destination.exists():
                fail(name, "missing link target", target=target)
            elif anchor and destination.is_file() and destination.suffix == ".md":
                if anchor not in anchors(destination.read_text(encoding="utf-8")):
                    fail(name, "missing Markdown anchor", target=target)
        if path.name == "SKILL.md":
            metadata = path.parent / "agents/openai.yaml"
            if not skill_ui_complete(metadata):
                fail(name, "missing or incomplete Skill UI metadata")
    source = root / "core/data/src/main/kotlin/com/nanzhufeng/nanfengbazi/data/db/NanfengBaziDatabase.kt"
    data = root / "docs/DATA_CONTRACT.md"
    if source.is_file() and data.is_file():
        actual = re.search(r"const val SCHEMA_VERSION\s*=\s*(\d+)", source.read_text())
        documented = re.search(r"当前版本：\s*(\d+)", data.read_text())
        if not actual or not documented or actual[1] != documented[1]:
            fail("docs/DATA_CONTRACT.md", "Schema version missing or differs from implementation")
    else:
        fail("docs/DATA_CONTRACT.md", "Schema evidence unavailable")
    return dict(result="FAIL" if errors else "PASS", active_files=len(paths), local_links=links, errors=errors,
                scope="index-discovered active contracts/audits; history is evidence only; no device/network/build/business mutation; semantic review still required")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    result = check(parser.parse_args().root)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return int(result["result"] != "PASS")


if __name__ == "__main__":
    raise SystemExit(main())
