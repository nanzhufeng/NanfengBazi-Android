#!/usr/bin/env python3
"""Generate the bundled province/city/district catalog from a pinned source checkout."""

from __future__ import annotations

import csv
import shutil
import subprocess
import sys
from pathlib import Path


EXPECTED_COMMIT = "ca2ada5ea608b57c7b0178aa568ced6e363b57f7"
RESOURCE_PATH = Path("app/src/main/resources/birthplace/china-districts.csv")
LICENSE_PATH = Path("app/src/main/resources/birthplace/LICENSE-province-city-china.txt")


def read_rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as source:
        return list(csv.DictReader(source))


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: generate_china_district_catalog.py SOURCE_REPO")
    source_root = Path(sys.argv[1]).resolve()
    commit = subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=source_root, text=True
    ).strip()
    if commit != EXPECTED_COMMIT:
        raise SystemExit(f"unexpected source commit: {commit}")

    dist = source_root / "packages/core/dist"
    provinces = {row["province"]: row["name"] for row in read_rows(dist / "province.csv")}
    cities = {
        (row["province"], row["city"]): row["name"]
        for row in read_rows(dist / "city.csv")
    }
    areas = read_rows(dist / "area.csv")

    RESOURCE_PATH.parent.mkdir(parents=True, exist_ok=True)
    with RESOURCE_PATH.open("w", encoding="utf-8", newline="") as target:
        target.write("# province-city-china\n")
        target.write(f"# commit={commit}\n")
        target.write("region|city|district\n")
        for area in areas:
            region = provinces[area["province"]]
            city = cities.get((area["province"], area["city"]), region)
            target.write(f"{region}|{city}|{area['name']}\n")

    shutil.copyfile(source_root / "LICENSE", LICENSE_PATH)
    print(f"generated {len(areas)} district rows at {RESOURCE_PATH}")


if __name__ == "__main__":
    main()
