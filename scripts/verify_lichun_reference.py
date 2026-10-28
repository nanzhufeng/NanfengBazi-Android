#!/usr/bin/env python3
"""Development-only astronomical cross-check; never imported by the Android app.

Input CSV: year,tyme_utc, exported using the locked Tyme4j jar's SolarTerm API.
Requires pyswisseph==2.10.3.2 in a separate development environment. Requests
Swiss files and records the actual flags, including Moshier fallback. Does not
download ephemerides, access devices/user records, or alter production rules.
"""
import argparse
import csv
import hashlib
import json
from datetime import datetime, timezone, timedelta
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tyme-csv", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--reference-csv", type=Path, required=True)
    args = parser.parse_args()
    import swisseph as swe
    rows = []
    for raw in csv.DictReader(args.tyme_csv.open()):
        year = int(raw["year"])
        lo, hi = swe.julday(year, 2, 2, 0), swe.julday(year, 2, 6, 0)
        flags_seen = set()
        for _ in range(50):
            mid = (lo + hi) / 2
            position, flags = swe.calc_ut(mid, swe.SUN, swe.FLG_SWIEPH | swe.FLG_SPEED)
            flags_seen.add(flags)
            if position[0] < 315:
                lo = mid
            else:
                hi = mid
        jd = (lo + hi) / 2
        reference = datetime(1970, 1, 1, tzinfo=timezone.utc) + timedelta(days=jd - 2440587.5)
        tyme = datetime.fromisoformat(raw["tyme_utc"].replace("Z", "+00:00"))
        delta = (tyme - reference).total_seconds()
        rows.append(dict(year=year, tyme_utc=raw["tyme_utc"],
                         reference_ut=reference.isoformat(), model_delta_seconds=round(delta, 6),
                         actual_flags=sorted(flags_seen)))
    with args.reference_csv.open("w", newline="") as out:
        writer = csv.writer(out)
        writer.writerow(["year", "tyme_utc", "reference_ut", "model_delta_seconds"])
        for row in rows:
            writer.writerow([row[key] for key in ["year", "tyme_utc", "reference_ut", "model_delta_seconds"]])
    review = [r for r in rows if 1950 <= r["year"] <= 2050 and abs(r["model_delta_seconds"]) > 5]
    report = dict(
        provider="Tyme4j 1.5.1 qiAccurate2, China civil -> UTC once",
        reference=f"pyswisseph {swe.__version__}; engine {swe.version}",
        definition="apparent geocentric tropical solar longitude 315 degrees",
        requested_flags=swe.FLG_SWIEPH | swe.FLG_SPEED,
        actual_flags=sorted({v for r in rows for v in r["actual_flags"]}),
        ephemeris_note="Moshier when actual_flags includes FLG_MOSEPH; no complete Swiss/DE files verified",
        time_note="calc_ut is UT; UTC-labelled modern clocks are compared at seconds scale. Historical UT/UTC and future delta-T uncertainty are not millisecond guarantees.",
        rounding_note="Tyme public SolarTime is rounded to whole seconds; subsecond reference values express numerical output, not an accuracy claim.",
        year_range=[min(r["year"] for r in rows), max(r["year"] for r in rows)],
        max_abs_delta_seconds=max(abs(r["model_delta_seconds"]) for r in rows),
        review_threshold_seconds_1950_2050=5,
        review_required=review,
        table_sha256=hashlib.sha256(args.reference_csv.read_bytes()).hexdigest(),
        rows=rows,
    )
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({k: report[k] for k in ["reference", "actual_flags", "year_range", "max_abs_delta_seconds", "review_required"]}, ensure_ascii=False))
    return 1 if review else 0


if __name__ == "__main__":
    raise SystemExit(main())
