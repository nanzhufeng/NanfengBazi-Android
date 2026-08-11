#!/usr/bin/env python3
"""逐案例核对问真迁移包与南枫八字 Room 数据库；不读取或输出正文内容。"""

from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
import sys
import uuid
from pathlib import Path


STEMS = set("甲乙丙丁戊己庚辛壬癸")
BRANCHES = set("子丑寅卯辰巳午未申酉戌亥")


def stable_id(namespace: str, value: str) -> str:
    digest = bytearray(hashlib.md5(f"{namespace}:{value}".encode("utf-8")).digest())
    digest[6] = (digest[6] & 0x0F) | 0x30
    digest[8] = (digest[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(digest)))


def explicit(value: str | None) -> dict:
    normalized = (value or "").strip()
    return {"state": "PRESENT", "value": normalized} if normalized else {
        "state": "ABSENT",
        "value": None,
    }


def sex(value: str) -> str:
    normalized = value.strip()
    if normalized in {"男", "MAN", "1"}:
        return "MAN"
    if normalized in {"女", "WOMAN", "0"}:
        return "WOMAN"
    raise ValueError(f"unknown sex token: {normalized!r}")


def display_time(value: dict) -> str:
    return (
        f"{value['year']:04d}-{value['month']:02d}-{value['day']:02d} "
        f"{value['hour']:02d}:{value['minute']:02d}:{value.get('second', 0):02d}"
    )


def pillars(value: dict) -> dict:
    return {key: value[key] for key in ("year", "month", "day", "hour")}


def pillars_compact(value: dict) -> str:
    return "".join(value[key] for key in ("year", "month", "day", "hour"))


def pillars_valid(value: dict) -> bool:
    return all(
        len(value[key]) == 2 and value[key][0] in STEMS and value[key][1] in BRANCHES
        for key in ("year", "month", "day", "hour")
    )


class Reconciler:
    def __init__(self, source: dict, connection: sqlite3.Connection, allow_existing: bool = False):
        self.source = source
        self.db = connection
        self.db.row_factory = sqlite3.Row
        self.allow_existing = allow_existing
        self.errors: list[str] = []
        self.expected_case_ids: set[str] = set()
        self.expected_record_ids: set[str] = set()
        self.expected_event_ids: set[str] = set()
        self.expected_tag_links: set[tuple[str, str]] = set()
        self.expected_group_links: set[tuple[str, str]] = set()

    def check(self, label: str, expected, actual) -> None:
        if expected != actual:
            self.errors.append(f"{label}: expected={expected!r}, actual={actual!r}")

    def one(self, query: str, parameters: tuple) -> sqlite3.Row | None:
        return self.db.execute(query, parameters).fetchone()

    def reconcile_case(self, source_case: dict, library: str) -> None:
        source_id = source_case["sourceId"]
        case_id = stable_id("user-case" if library == "USER" else "celebrity-case", source_id)
        self.expected_case_ids.add(case_id)
        row = self.one("SELECT * FROM cases WHERE id = ?", (case_id,))
        if row is None:
            self.errors.append(f"{library}/{source_id}: missing case")
            return
        prefix = f"{library}/{source_id}"
        name = (source_case.get("name") or "").strip()
        self.check(f"{prefix}/alias", name or ("未命名案例" if library == "USER" else "未命名名人"), row["alias"])
        self.check(f"{prefix}/name", explicit(name), {"state": row["nameState"], "value": row["nameValue"]})
        self.check(f"{prefix}/sex", sex(source_case["sex"]), row["sexForFortuneDirection"])
        self.check(f"{prefix}/sourceType", "WENZHEN_WEB_IMPORT", row["sourceType"])
        self.check(f"{prefix}/libraryType", library, row["libraryType"])

        source_time = source_case["adoptedSourceTime"] if library == "USER" else source_case["solarTime"]
        birth = json.loads(row["birthInputJson"])
        self.check(f"{prefix}/birthTime", {**source_time, "second": source_time.get("second", 0)}, birth["calendarInput"]["dateTime"])
        self.check(f"{prefix}/birthSex", sex(source_case["sex"]), birth["sexForFortuneDirection"])
        self.check(f"{prefix}/trueSolar", False, birth["useTrueSolarTime"])
        self.check(f"{prefix}/timeSource", "WENZHEN_WEB_IMPORT", birth["timeSourceType"])
        expected_location = (source_case.get("location") or "").strip() or None if library == "USER" else None
        self.check(f"{prefix}/location", expected_location, birth["locationName"])
        note = birth.get("sourceNote") or ""
        if library == "USER":
            required_note_parts = (
                display_time(source_case["originalSolarTime"]),
                display_time(source_case["adoptedSourceTime"]),
                pillars_compact(source_case["fourPillars"]),
            )
        else:
            required_note_parts = (display_time(source_case["solarTime"]), pillars_compact(source_case["fourPillars"]))
        for part in required_note_parts:
            if part not in note:
                self.errors.append(f"{prefix}/sourceNote missing structured source value")

        profile = json.loads(row["profileJson"])
        source_profile = source_case.get("profile") or {}
        for field in ("occupation", "education", "finance", "marriage", "health"):
            self.check(f"{prefix}/profile/{field}", explicit(source_profile.get(field)), profile[field])

        expected_group = (source_case.get("groupName") or "").strip()
        groups = [item[0] for item in self.db.execute(
            "SELECT g.name FROM case_group_cross_ref r JOIN case_groups g ON g.id=r.groupId WHERE r.caseId=? ORDER BY g.name",
            (case_id,),
        )]
        self.check(f"{prefix}/groups", [expected_group] if expected_group else [], groups)
        if expected_group:
            self.expected_group_links.add((case_id, expected_group))

        expected_tags = [] if library == "USER" else list(dict.fromkeys(
            value.strip() for value in (source_case.get("periodTag") or "", source_case.get("identityTag") or "") if value.strip()
        ))
        tags = [item[0] for item in self.db.execute(
            "SELECT t.name FROM case_tag_cross_ref r JOIN case_tags t ON t.id=r.tagId WHERE r.caseId=? ORDER BY r.rowid",
            (case_id,),
        )]
        self.check(f"{prefix}/tags", expected_tags, tags)
        for tag in expected_tags:
            self.expected_tag_links.add((case_id, tag))

        expected_records = []
        for kind, record_type, field in (
            ("owner-feedback", "OWNER_FEEDBACK", "ownerFeedback"),
            ("master-commentary", "MASTER_COMMENTARY", "masterCommentary"),
        ):
            content = (source_case.get(field) or "").strip()
            if content:
                record_id = stable_id(f"user-record:{source_id}", kind)
                self.expected_record_ids.add(record_id)
                expected_records.append((record_id, record_type, content))
        records = [tuple(item) for item in self.db.execute(
            "SELECT id,type,content FROM text_records WHERE caseId=? ORDER BY sortOrder", (case_id,)
        )]
        self.check(f"{prefix}/records", expected_records, records)
        bad_sources = self.one(
            "SELECT COUNT(*) AS count FROM text_records WHERE caseId=? AND sourceType!='WENZHEN_WEB_IMPORT'", (case_id,)
        )["count"]
        self.check(f"{prefix}/recordSource", 0, bad_sources)

        expected_events = []
        namespace = f"{'user' if library == 'USER' else 'celebrity'}-event:{source_id}"
        for event in sorted(source_case.get("timeline") or [], key=lambda value: value["order"]):
            event_id = stable_id(namespace, event["sourceId"])
            self.expected_event_ids.add(event_id)
            expected_events.append((event_id, event))
        actual_events = list(self.db.execute(
            "SELECT id,eventJson FROM case_events WHERE caseId=? ORDER BY sortOrder", (case_id,)
        ))
        self.check(f"{prefix}/eventCount", len(expected_events), len(actual_events))
        for (expected_id, source_event), actual_event in zip(expected_events, actual_events):
            event = json.loads(actual_event["eventJson"])
            self.check(f"{prefix}/eventId", expected_id, actual_event["id"])
            self.check(f"{prefix}/eventTitle", (source_event.get("sourceLabel") or "").strip() or None, event["title"])
            self.check(f"{prefix}/eventYear", source_event["year"], event["year"])
            self.check(f"{prefix}/eventStemBranch", (source_event.get("stemBranch") or "").strip(), event["stemBranch"])
            self.check(f"{prefix}/eventStatus", (source_event.get("status") or "").strip() or None, event["status"])
            self.check(f"{prefix}/eventContent", (source_event.get("content") or "").strip(), event["rawText"])
            self.check(f"{prefix}/eventLevel", source_event["level"], event["timelineLevel"])

        snapshot_namespace = "user-calculation" if library == "USER" else "celebrity-calculation"
        snapshot = self.one("SELECT * FROM calculation_snapshots WHERE caseId=?", (case_id,))
        if snapshot is None:
            self.errors.append(f"{prefix}/snapshot missing")
        else:
            self.check(f"{prefix}/snapshotId", stable_id(snapshot_namespace, source_id), snapshot["id"])
            self.check(f"{prefix}/snapshotAdopted", 1, snapshot["adopted"])
            result = json.loads(snapshot["resultJson"])["result"]
            source_pillars = source_case["fourPillars"]
            if pillars_valid(source_pillars):
                self.check(f"{prefix}/pillars", pillars(source_pillars), result["fourPillars"])
            else:
                warning_codes = {item["code"] for item in result.get("warnings", [])}
                if "WENZHEN_INVALID_SOURCE_PILLARS_RECALCULATED" not in warning_codes:
                    self.errors.append(f"{prefix}/invalidPillars missing recalculation warning")

    def run(self) -> None:
        for source_case in self.source["userCases"]:
            self.reconcile_case(source_case, "USER")
        for source_case in self.source["celebrityCases"]:
            self.reconcile_case(source_case, "CELEBRITY")

        database_case_ids = {item[0] for item in self.db.execute("SELECT id FROM cases")}
        database_record_ids = {item[0] for item in self.db.execute("SELECT id FROM text_records")}
        database_event_ids = {item[0] for item in self.db.execute("SELECT id FROM case_events")}
        if self.allow_existing:
            self.check("database/missingCaseIds", set(), self.expected_case_ids - database_case_ids)
            self.check("database/missingRecordIds", set(), self.expected_record_ids - database_record_ids)
            self.check("database/missingEventIds", set(), self.expected_event_ids - database_event_ids)
        else:
            self.check("database/caseIds", self.expected_case_ids, database_case_ids)
            self.check("database/recordIds", self.expected_record_ids, database_record_ids)
            self.check("database/eventIds", self.expected_event_ids, database_event_ids)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("database", type=Path)
    parser.add_argument(
        "--allow-existing",
        action="store_true",
        help="允许数据库保留导入前已有案例，但仍逐项核对迁移包内的全部数据。",
    )
    args = parser.parse_args()
    source = json.loads(args.source.read_text(encoding="utf-8"))
    with sqlite3.connect(args.database) as connection:
        reconciler = Reconciler(source, connection, allow_existing=args.allow_existing)
        reconciler.run()
    if reconciler.errors:
        print(f"WENZHEN_DB_RECONCILIATION_FAILED differences={len(reconciler.errors)}")
        for error in reconciler.errors[:20]:
            print(error)
        return 1
    print(
        "WENZHEN_DB_RECONCILIATION_OK "
        f"cases={len(reconciler.expected_case_ids)} "
        f"records={len(reconciler.expected_record_ids)} "
        f"events={len(reconciler.expected_event_ids)} "
        f"tagLinks={len(reconciler.expected_tag_links)} "
        f"groupLinks={len(reconciler.expected_group_links)}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
