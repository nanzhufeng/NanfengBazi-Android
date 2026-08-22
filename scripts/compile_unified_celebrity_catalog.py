#!/usr/bin/env python3
"""Build the install-time celebrity catalog from review-only source evidence.

The output deliberately has one ``cases`` collection.  Source materials are
kept only as evidence nested under each canonical case; they are never exposed
as separate installable catalogs.  This script only selects CELEBRITY rows from
the audit database and refuses to run if that boundary changes.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sqlite3
import sys
import uuid
from collections import defaultdict
from datetime import date
from pathlib import Path


UNIFIED_GROUPS = {
    "unified-celebrity-emperor": "君主",
    "unified-celebrity-politics": "政界",
    "unified-celebrity-military": "军事",
    "unified-celebrity-business": "商界",
    "unified-celebrity-tech": "科技",
    "unified-celebrity-medicine": "医学",
    "unified-celebrity-culture": "文教",
    "unified-celebrity-entertainment": "娱乐传媒",
    "unified-celebrity-sports": "体育",
    "unified-celebrity-religion": "僧道",
}

EMPEROR = {"皇帝", "皇后", "开国皇帝", "东汉开国皇帝", "唯一女皇帝", "纳粹元首"}
POLITICS = {"政治", "公共事务", "革命", "外交", "法政", "法律", "统战", "民主党派", "名臣"}
MILITARY = {"军事", "名将"}
BUSINESS = {
    "商业", "实业", "价值投资", "长期投资", "企业经营", "私募", "资产管理", "基金管理", "企业家",
    "创业", "投资", "资本家", "实业家", "近代资本家", "近代中国实业家", "商圣", "巨富", "世界巨富",
    "面粉大王", "煤老板", "船王", "华人世界船王", "世界七大船王", "经营之神", "春秋巨富",
    "南浔“四象”之首", "南京巨富", "上海滩大亨", "上海滩教父", "斗富第一人",
}
TECH = {
    "科技", "科学", "科技工作", "技术", "工程", "人工智能", "互联网", "计算机", "芯片", "新能源",
    "电动汽车", "航天", "物理学", "化学", "生物学", "数学", "科学家", "美国发明家", "古建筑学",
}
MEDICINE = {"医学", "医学家", "郎中", "医疗", "公共卫生", "临床"}
EDUCATION = {"教育", "教育家", "教师", "著名学者"}
HERITAGE = {"敦煌学", "数字敦煌", "文化遗产", "文物保护", "考古学", "藏书家"}
LITERATURE = {"文学", "文学家", "作家", "诗人", "小说", "散文家", "文士", "史学家", "唐宋八大家", "出版", "语言文字"}
ENTERTAINMENT = {"美术", "书法", "书画家", "钢琴家", "戏曲家", "京剧名家", "京剧演员", "影视", "电影", "电视剧", "演员", "音乐", "歌手", "导演", "制片", "表演", "舞者", "舞蹈演员", "艺人", "作曲家", "综艺"}
SPORTS = {"体育", "体育明星", "篮球", "网球", "足球", "奥运", "武术"}
MEDIA = {"传媒", "主持", "新闻", "电视"}
RELIGION = {"宗教", "宗教事务", "佛教", "佛家", "道家"}
THOUGHT = {"思想", "理论", "哲学", "哲学家", "思想家", "理学家", "儒家创始人", "儒家五圣之一", "命理学家"}
FOREIGN_GROUPS = {
    "弗里德里希·尼采": "unified-celebrity-culture",
    "阿道夫·希特勒": "unified-celebrity-politics",
    "弗里德里希·恩格斯": "unified-celebrity-culture",
    "罗宾德拉纳特·泰戈尔": "unified-celebrity-culture",
    "约翰·D·洛克菲勒": "unified-celebrity-business",
    "阿尔伯特·爱因斯坦": "unified-celebrity-tech",
    "托马斯·爱迪生": "unified-celebrity-tech",
}
NAMED_GROUPS = {
    "丁丙": "unified-celebrity-culture",
    "樊锦诗": "unified-celebrity-culture",
    "岳飞": "unified-celebrity-military",
    "戚继光": "unified-celebrity-military",
    "沈辅": "unified-celebrity-culture",
}
LEGACY_SUPERSEDED_CASE_IDS = {
    # 早期问真导入曾给欧阳修分配过不同稳定 ID；只归档该来源记录，不触碰用户案例。
    "欧阳修": {"3d39cbc3-c933-3e04-9f9a-afa179efd6da"},
}

# 用户已明确要求这些早期资料包名人保持可见；它们必须由资料包自身恢复并投影到
# 当前主分组，而不是留在设备回收站等待人工逐条操作。
RESTORE_LEGACY_CASE_IDS = {
    "阿尔伯特·爱因斯坦": {"0e396fab-4da6-30e8-b4d2-7fea879f52fb"},
    "迈克尔·乔丹": {"1698c93b-bb71-3cfb-ae5e-fe22f2791e42"},
    "李小龙": {"2b4812a1-c3fd-36f8-a830-dc6c898e1a42"},
    "欧阳修": {"3d39cbc3-c933-3e04-9f9a-afa179efd6da"},
    "玛丽莲·梦露": {"7affda9a-6078-3950-a9fb-74b24bafeee7"},
}

# 君主资料不沿用问真导入中带有未来年份的排盘时间。这里直接保留史料记载的
# 原历法日期；若史料未记载出生时刻，12:00 只用于让排盘引擎构建候选，绝不作为
# 真实出生时刻展示或背书。日期来源以故宫博物院的宫廷世系为主。
HISTORICAL_RULER_BIRTH_OVERRIDES = {
    "康熙": ("LUNAR", 1654, 3, 18, "https://www.dpm.org.cn/court/lineage/226256.html"),
    "雍正": ("LUNAR", 1678, 10, 30, "https://www.dpm.org.cn/court/lineage/226259.html"),
    "乾隆": ("LUNAR", 1711, 8, 13, "https://www.dpm.org.cn/court/lineage/226263.html"),
    "嘉庆": ("LUNAR", 1760, 10, 6, "https://www.dpm.org.cn/court/lineage/226238.html"),
    "道光": ("LUNAR", 1782, 8, 10, "https://www.dpm.org.cn/court/lineage/226239.html"),
    "咸丰": ("LUNAR", 1831, 6, 9, "https://www.dpm.org.cn/court/lineage/226247.html"),
    "同治": ("LUNAR", 1856, 3, 23, "https://www.dpm.org.cn/court/lineage/226248.html"),
    "光绪": ("SOLAR", 1871, 8, 14, "https://www.dpm.org.cn/court/event/159203.html"),
    "慈禧太后": ("SOLAR", 1835, 11, 29, "https://news.cctv.com/china/20090130/102327_10.shtml"),
    "溥仪": ("LUNAR", 1906, 1, 14, "https://www.dpm.org.cn/court/lineage/226255.html"),
    "钮祜禄氏": ("SOLAR", 1837, 7, 12, "https://www.dpm.org.cn/topic/wedding_situation.html?linkage_id=1006390"),
    "嘉靖": ("LUNAR", 1507, 8, 10, "https://www.dpm.org.cn/court/lineage/226241.html"),
    "隆庆": ("LUNAR", 1537, 1, 23, "https://www.dpm.org.cn/court/lineage/226264.html"),
    "正德": ("LUNAR", 1491, 9, 24, "https://www.dpm.org.cn/court/lineage/226240.html"),
    "朱翊钧": ("LUNAR", 1563, 8, 17, "https://www.dpm.org.cn/court/lineage/226265.html"),
    "朱常洛": ("LUNAR", 1582, 8, 11, "https://www.dpm.org.cn/court/lineage/226242.html"),
    "朱由校": ("LUNAR", 1605, 11, 14, "https://www.dpm.org.cn/court/lineage/226243.html"),
    "崇祯": ("LUNAR", 1610, 12, 24, "https://www.dpm.org.cn/court/lineage/226246.html"),
    "明建文帝": ("LUNAR", 1377, 11, 5, "https://www.dpm.org.cn/court/lineage/226245.html"),
    "朱元璋": ("LUNAR", 1328, 9, 18, "https://www.dpm.org.cn/court/lineage/226244.html"),
    "朱棣": ("LUNAR", 1360, 4, 17, "https://www.dpm.org.cn/court/lineage/226257.html"),
    "皇太极": ("LUNAR", 1592, 10, 25, "https://www.dpm.org.cn/court/lineage/226251.html"),
    "顺治": ("LUNAR", 1638, 1, 30, "https://www.dpm.org.cn/court/lineage/226262.html"),
    "忽必烈": ("LUNAR", 1215, 8, 28, "https://zh.wikipedia.org/wiki/%E5%BF%BD%E5%BF%85%E7%83%88"),
    "元顺帝": ("LUNAR", 1320, 4, 17, "https://zh.wikipedia.org/wiki/%E5%85%83%E9%A0%86%E5%B8%9D"),
    # 刘秀生于建平元年十二月甲子，即公元前 5 年 1 月 15 日。Python/JSON 使用
    # 天文纪年：公元前 5 年 = -4；不得再保留来源包中错误的 1916 年。
    "汉世祖光武帝": ("SOLAR", -4, 1, 15, "https://zh.wikipedia.org/wiki/%E6%B1%89%E5%85%89%E6%AD%A6%E5%B8%9D"),
    "武则天": ("SOLAR", 624, 2, 17, "https://www.ioe.cas.cn/qtgn/zt/lstk/201302/t20130223_3765061.html"),
}


def java_name_uuid(namespace: str, value: str) -> str:
    digest = bytearray(hashlib.md5(f"{namespace}:{value}".encode("utf-8")).digest())
    digest[6] = (digest[6] & 0x0F) | 0x30
    digest[8] = (digest[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(digest)))


def kotlin_block(source: str, name: str) -> str:
    start = source.index(f"private val {name} = mapOf(")
    end = source.index("\n)\n", start) + 2
    return source[start:end]


def load_rules(view_model: Path):
    source = view_model.read_text(encoding="utf-8")
    alias_block = kotlin_block(source, "celebrityResearchAliases")
    aliases = dict(re.findall(r'"([^"]+)"\s+to\s+"([^"]+)"', alias_block))

    verified_block = kotlin_block(source, "independentlyVerifiedCelebrityBirthDates")
    verified = {
        name: (int(year), int(month), int(day))
        for name, year, month, day in re.findall(
            r'"([^"]+)"\s+to\s+IndependentlyVerifiedBirthDate\(\s*(\d+),\s*(\d+),\s*(\d+)',
            verified_block,
        )
    }
    conflict_block = kotlin_block(source, "independentlyVerifiedCelebrityDateConflicts")
    corrections = {}
    for name, body in re.findall(
        r'"([^"]+)"\s+to\s+IndependentlyVerifiedDateConflict\((.*?)(?=\n\s*"[^"]+"\s+to\s+IndependentlyVerifiedDateConflict\(|\n\))',
        conflict_block,
        flags=re.S,
    ):
        match = re.search(r'authoritativeDate\s*=\s*IndependentlyVerifiedBirthDate\(\s*(\d+),\s*(\d+),\s*(\d+)', body)
        if match:
            corrections[name] = tuple(map(int, match.groups()))
    return aliases, verified, corrections


def canonical_name(name: str, aliases: dict[str, str]) -> str:
    return aliases.get(name, name)


def group_id(name: str, groups: list[str], tags: list[str]) -> str:
    if name in NAMED_GROUPS:
        return NAMED_GROUPS[name]
    if name in FOREIGN_GROUPS:
        return FOREIGN_GROUPS[name]
    group_set, tag_set = set(groups), set(tags)
    if "君主" in group_set or tag_set & EMPEROR or any("皇帝" in tag for tag in tag_set):
        return "unified-celebrity-emperor"
    if tag_set & MILITARY:
        return "unified-celebrity-military"
    if "僧道" in group_set or "宗教与公共人物" in group_set or tag_set & RELIGION:
        return "unified-celebrity-religion"
    if tag_set & MEDICINE:
        return "unified-celebrity-medicine"
    if "科技" in group_set or "科学" in group_set or tag_set & TECH:
        return "unified-celebrity-tech"
    if tag_set & EDUCATION:
        return "unified-celebrity-culture"
    if tag_set & HERITAGE:
        return "unified-celebrity-culture"
    if tag_set & THOUGHT:
        return "unified-celebrity-culture"
    if "文学" in group_set or "文学与艺术" in group_set or tag_set & LITERATURE:
        return "unified-celebrity-culture"
    if "体育" in group_set or tag_set & SPORTS:
        return "unified-celebrity-sports"
    if "传媒" in group_set or tag_set & MEDIA:
        return "unified-celebrity-entertainment"
    if "娱乐" in group_set or tag_set & ENTERTAINMENT:
        return "unified-celebrity-entertainment"
    if "商界" in group_set or "商业" in group_set or tag_set & BUSINESS:
        return "unified-celebrity-business"
    if "政治" in group_set or "政治与公共人物" in group_set or tag_set & POLITICS:
        return "unified-celebrity-politics"
    return "unified-celebrity-culture"


def solar_date(birth_input: dict) -> tuple[int, int, int] | None:
    calendar = birth_input.get("calendarInput", {})
    datetime = calendar.get("dateTime", {})
    try:
        return int(datetime["year"]), int(datetime["month"]), int(datetime["day"])
    except (KeyError, TypeError, ValueError):
        return None


def apply_correction(birth_input: dict, correction: tuple[int, int, int] | None) -> dict:
    result = json.loads(json.dumps(birth_input))
    if correction is None:
        return result
    date_time = result.get("calendarInput", {}).get("dateTime")
    if isinstance(date_time, dict):
        date_time["year"], date_time["month"], date_time["day"] = correction
    return result


def normalize_calendar_discriminator(birth_input: dict) -> dict:
    result = json.loads(json.dumps(birth_input))
    calendar = result.get("calendarInput")
    if isinstance(calendar, dict):
        legacy_type = calendar.pop("type", None)
        if legacy_type == "SOLAR":
            calendar["_type"] = "com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Solar"
    return result


def normalize_historical_celebrity_time(birth_input: dict, canonical_name: str) -> dict:
    """Use reviewed ruler dates and never present imported placeholder hours as facts."""
    result = json.loads(json.dumps(birth_input))
    override = HISTORICAL_RULER_BIRTH_OVERRIDES.get(canonical_name)
    if override is not None:
        calendar, year, month, day, source_url = override
        if calendar == "LUNAR":
            result["calendarInput"] = {
                "_type": "com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Lunar",
                "dateTime": {
                    "year": year, "month": month, "day": day,
                    "hour": 12, "minute": 0, "second": 0, "isLeapMonth": False,
                },
            }
        else:
            result["calendarInput"] = {
                "_type": "com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Solar",
                "dateTime": {
                    "year": year, "month": month, "day": day,
                    "hour": 12, "minute": 0, "second": 0,
                },
            }
        result["timeSourceType"] = "OFFICIAL_RECORD"
        result["timePrecision"] = "APPROXIMATE"
        historical_date = (
            f"公元前{1 - year}年{month}月{day}日"
            if year <= 0 else f"{year}年{month}月{day}日"
        )
        date_evidence = f"君主出生日期按公开史料校正：{historical_date}；{source_url}"
    else:
        date_time = result.get("calendarInput", {}).get("dateTime")
        if isinstance(date_time, dict):
            # 没有覆盖规则的历史记录也不能保留来源中的伪分钟。
            date_time["minute"] = 0
            date_time["second"] = 0
        result["timePrecision"] = "DOUBLE_HOUR_ONLY"
        date_evidence = "原始历史资料尚未完成日期校勘"
    note = result.get("sourceNote", "").rstrip("；。")
    clarification = f"{date_evidence}；公开史料未载出生时刻，12:00 仅为排盘占位，不是出生时刻"
    if clarification not in note:
        result["sourceNote"] = f"{note}；{clarification}。"
    return result


def load_historical_cases(db_path: Path, aliases, verified, corrections):
    connection = sqlite3.connect(f"file:{db_path}?mode=ro", uri=True)
    connection.row_factory = sqlite3.Row
    rows = connection.execute(
        """
        SELECT id, alias, sexForFortuneDirection, birthInputJson, birthTimeCandidatesJson
        FROM cases
        WHERE libraryType = 'CELEBRITY' AND sourceType = 'WENZHEN_WEB_IMPORT'
        ORDER BY id
        """
    ).fetchall()
    if len(rows) != 351:
        raise RuntimeError(f"审核源名人记录数异常：{len(rows)}")
    group_rows = connection.execute(
        """
        SELECT x.caseId, g.name
        FROM case_group_cross_ref x
        JOIN case_groups g ON g.id = x.groupId
        JOIN cases c ON c.id = x.caseId
        WHERE c.libraryType = 'CELEBRITY' AND c.sourceType = 'WENZHEN_WEB_IMPORT'
        """
    ).fetchall()
    tag_rows = connection.execute(
        """
        SELECT x.caseId, t.name
        FROM case_tag_cross_ref x
        JOIN case_tags t ON t.id = x.tagId
        JOIN cases c ON c.id = x.caseId
        WHERE c.libraryType = 'CELEBRITY' AND c.sourceType = 'WENZHEN_WEB_IMPORT'
        """
    ).fetchall()
    connection.close()
    groups, tags = defaultdict(list), defaultdict(list)
    for row in group_rows:
        groups[row["caseId"]].append(row["name"])
    for row in tag_rows:
        tags[row["caseId"]].append(row["name"])
    result = []
    for row in rows:
        original_birth = json.loads(row["birthInputJson"])
        name = canonical_name(row["alias"].strip(), aliases)
        corrected_birth = normalize_historical_celebrity_time(
            normalize_calendar_discriminator(
                apply_correction(original_birth, corrections.get(name)),
            ),
            name,
        )
        original_birth = normalize_calendar_discriminator(original_birth)
        verified_identity = solar_date(corrected_birth) == verified.get(name)
        result.append({
            "id": row["id"],
            "canonicalName": name,
            "sex": row["sexForFortuneDirection"],
            "birthInput": corrected_birth,
            "birthTimeCandidates": [
                {
                    **candidate,
                    "birthInput": normalize_calendar_discriminator(
                        apply_correction(candidate["birthInput"], corrections.get(name)),
                    ),
                }
                for candidate in json.loads(row["birthTimeCandidatesJson"])
            ],
            "groups": sorted(set(groups[row["id"]])),
            "tags": sorted(set(tags[row["id"]])),
            "verifiedIdentity": verified_identity,
            "sourceEvidence": [{
                "sourceType": "WENZHEN_WEB_IMPORT",
                "sourceId": row["id"],
                "originalBirthInput": original_birth,
            }],
        })
    return result


def curated_birth_input(source: dict):
    candidate = source["defaultTime"]
    return {
        "calendarInput": {
            "_type": "com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Solar",
            "dateTime": {
                "year": source["birthDate"]["year"], "month": source["birthDate"]["month"],
                "day": source["birthDate"]["day"], "hour": candidate["hour"],
                "minute": candidate["minute"], "second": candidate.get("second", 0),
            },
        },
        "sexForFortuneDirection": "MAN" if source["sex"] in {"男", "MAN", "M"} else "WOMAN",
        "timePrecision": candidate["precision"],
        "timeZoneId": source["timeZoneId"],
        "locationName": source["birthPlace"],
        "longitude": source["longitude"], "latitude": source["latitude"],
        "coordinateSource": "USER_ENTERED", "useTrueSolarTime": False,
        "timeSourceType": "OFFICIAL_RECORD" if source["evidenceRating"] == "AA" else "OTHER_RECORD",
        "sourceNote": "名人案例统一资料库：公开资料已编审；原始来源见资料证据。",
    }


def historical_calculation_proxy(name: str, evidence: list[dict]) -> dict | None:
    """Return a source-preserving chart input for the one verified BCE exception."""
    if name != "汉世祖光武帝":
        return None
    original = next(
        (item.get("originalBirthInput") for item in evidence if item.get("originalBirthInput")),
        None,
    )
    if original is None:
        raise RuntimeError("汉世祖光武帝缺少问真原始排盘输入，不能生成历史排盘代理")
    proxy = normalize_calendar_discriminator(original)
    proxy["sourceNote"] = (
        "历史人物排盘代理日期：原问真来源输入 1916-01-28 04:00:00，仅用于复算已记录"
        "四柱乙卯、己丑、甲子、丙寅；不改变公元前5年1月15日史实出生日期。"
    )
    proxy["isHistoricalCalculationProxy"] = True
    return proxy


def compile_catalog(db_path: Path, curated_path: Path, view_model: Path, output: Path):
    aliases, verified, corrections = load_rules(view_model)
    historical = load_historical_cases(db_path, aliases, verified, corrections)
    curated_package = json.loads(curated_path.read_text(encoding="utf-8"))
    curated = curated_package["cases"]

    canonical: dict[str, dict] = {}
    for item in historical:
        key = item["canonicalName"] if item["verifiedIdentity"] else f"historical:{item['id']}"
        canonical[key] = {
            "caseId": item["id"],
            "canonicalName": item["canonicalName"],
            "sex": item["sex"],
            "birthInput": item["birthInput"],
            "birthTimeCandidates": item["birthTimeCandidates"],
            "groupId": group_id(item["canonicalName"], item["groups"], item["tags"]),
            "tags": item["tags"],
            "sourceEvidence": item["sourceEvidence"],
            "curatedCase": None,
            "supersededCaseIds": [],
            "restoreCaseIds": [],
        }
    for source in curated:
        name = canonical_name(source["name"].strip(), aliases)
        existing = canonical.get(name)
        curated_id = java_name_uuid("case", source["sourceId"])
        evidence = {"sourceType": "CURATED_CELEBRITY_CATALOG", "sourceId": source["sourceId"]}
        if existing is not None:
            existing["sourceEvidence"].append(evidence)
            existing["curatedCase"] = source
            existing["birthInput"] = curated_birth_input(source)
            existing["birthTimeCandidates"] = []
            existing["sex"] = "MAN" if source["sex"] in {"男", "MAN", "M"} else "WOMAN"
            existing["groupId"] = group_id(name, [source["groupName"]], source["tags"])
            existing["tags"] = sorted(set(existing["tags"]) | set(source["tags"]))
            existing["supersededCaseIds"].append(curated_id)
        else:
            canonical[name] = {
                "caseId": curated_id,
                "canonicalName": name,
                "sex": "MAN" if source["sex"] in {"男", "MAN", "M"} else "WOMAN",
                "birthInput": curated_birth_input(source),
                "birthTimeCandidates": [],
                "groupId": group_id(name, [source["groupName"]], source["tags"]),
                "tags": sorted(set(source["tags"])),
                "sourceEvidence": [evidence],
                "curatedCase": source,
                "supersededCaseIds": [],
                "restoreCaseIds": [],
            }

    cases = sorted(canonical.values(), key=lambda item: (item["canonicalName"], item["caseId"]))
    for item in cases:
        proxy = historical_calculation_proxy(item["canonicalName"], item["sourceEvidence"])
        if proxy is not None:
            item["calculationBirthInput"] = proxy
        item["supersededCaseIds"] = sorted(
            set(item["supersededCaseIds"]) |
            LEGACY_SUPERSEDED_CASE_IDS.get(item["canonicalName"], set()),
        )
        item["restoreCaseIds"] = sorted(
            set(item["supersededCaseIds"]) &
            RESTORE_LEGACY_CASE_IDS.get(item["canonicalName"], set()),
        )
    if len({item["caseId"] for item in cases}) != len(cases):
        raise RuntimeError("规范案例标识冲突")
    if any(not item["sourceEvidence"] for item in cases):
        raise RuntimeError("规范案例缺少来源证据")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps({
        "format": "nanfeng-bazi-unified-celebrity-catalog",
        "version": 1,
        "catalogVersion": "2026.08.22-unified-r8",
        "compiledAt": date.today().isoformat(),
        "groups": [{"id": key, "name": value} for key, value in UNIFIED_GROUPS.items()],
        "cases": cases,
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "canonicalCases": len(cases),
        "mergedEvidenceCases": sum(1 for item in cases if len(item["sourceEvidence"]) > 1),
        "correctedBirthDates": sum(
            1 for item in historical
            if solar_date(item["birthInput"]) != solar_date(item["sourceEvidence"][0]["originalBirthInput"])
        ),
        "sourceTimesMarkedDoubleHourOnly": sum(
            1 for item in historical
            if item["birthInput"].get("timePrecision") == "DOUBLE_HOUR_ONLY"
        ),
    }, ensure_ascii=False))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--audit-db", type=Path, required=True)
    parser.add_argument("--curated", type=Path, required=True)
    parser.add_argument("--view-model", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    compile_catalog(args.audit_db, args.curated, args.view_model, args.output)


if __name__ == "__main__":
    main()
