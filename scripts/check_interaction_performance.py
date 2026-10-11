#!/usr/bin/env python3
"""Read UI / sample frames on an explicitly selected emulator, never a phone.

No install, data reset, instrumentation, network call, or account operation.
Examples: --serial emulator-5600 --action tap --text 设置 --output /tmp/frames.json
Frame samples include emulator overhead; they are not physical-device acceptance.
"""
import argparse
import json
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "com.nanzhufeng.nanfengbazi"


class EmulatorUi:
    def __init__(self, serial):
        if not re.fullmatch(r"emulator-\d+", serial):
            raise ValueError("Only an explicit emulator serial is allowed")
        self.serial = serial
        if self.adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
            raise ValueError("Target is not an emulator")

    def adb(self, *args):
        return subprocess.run(
            ["adb", "-s", self.serial, *args], check=True,
            capture_output=True, text=True, timeout=45,
        ).stdout

    def snapshot(self):
        dumped = self.adb("shell", "uiautomator", "dump", "/sdcard/bazi-perf-window.xml")
        if "dumped to" not in dumped:
            raise RuntimeError("Emulator UI hierarchy unavailable; refusing to read stale XML")
        return ET.fromstring(self.adb("exec-out", "cat", "/sdcard/bazi-perf-window.xml"))

    def find(self, text=None, description=None):
        root = self.snapshot()
        nodes = [n for n in root.iter("node") if
                 (text is not None and n.get("text") == text) or
                 (description is not None and description in n.get("content-desc", ""))]
        if not nodes:
            labels = [n.get("text") or n.get("content-desc") for n in root.iter("node")]
            raise ValueError(f"Missing control {text or description!r}: " +
                             " | ".join(x for x in labels if x)[:650])
        return nodes[0]

    def tap(self, text=None, description=None):
        node = self.find(text, description)
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        self.adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))

    def scroll(self, direction="up"):
        root = self.snapshot()
        bounds = [list(map(int, re.findall(r"\d+", n.get("bounds", ""))))
                  for n in root.iter("node")]
        width = max(b[2] for b in bounds if len(b) == 4)
        height = max(b[3] for b in bounds if len(b) == 4)
        x = width//2
        y1, y2 = int(height*.78), int(height*.34)
        if direction == "down":
            y1, y2 = y2, y1
        self.adb("shell", "input", "swipe", str(x), str(y1), str(x), str(y2), "450")

    def sample(self, name, action):
        self.adb("shell", "dumpsys", "gfxinfo", PACKAGE, "reset")
        start = time.monotonic()
        action()
        # The UI dump waits for accessibility idle. Do not label its duration as input latency.
        root = self.snapshot()
        raw = self.adb("shell", "dumpsys", "gfxinfo", PACKAGE)
        keys = {
            "frames": r"Total frames rendered: (\d+)",
            "janky_frames": r"Janky frames: (\d+)",
            "median_ms": r"50th percentile: (\d+)ms",
            "p90_ms": r"90th percentile: (\d+)ms",
            "p95_ms": r"95th percentile: (\d+)ms",
            "slow_ui_frames": r"Number Slow UI thread: (\d+)",
        }
        values = {key: int(match.group(1)) if (match := re.search(pattern, raw)) else None
                  for key, pattern in keys.items()}
        # gfxinfo reports a histogram sentinel even when no frame was rendered.
        if not values["frames"]:
            for key in ("median_ms", "p90_ms", "p95_ms"):
                values[key] = None
        values.update(operation=name, serial=self.serial,
                      observation_seconds=round(time.monotonic()-start, 3),
                      host_load_1m=round(os.getloadavg()[0], 2) if hasattr(os, "getloadavg") else None,
                      loading_visible=any("正在" in n.get("text", "") for n in root.iter("node")))
        return values


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--action", choices=["snapshot", "tap", "scroll", "back"], required=True)
    parser.add_argument("--text")
    parser.add_argument("--description")
    parser.add_argument("--direction", choices=["up", "down"], default="up")
    parser.add_argument("--label", default="interaction")
    parser.add_argument("--output")
    args = parser.parse_args()
    ui = EmulatorUi(args.serial)
    if args.action == "snapshot":
        root = ui.snapshot()
        labels = [{"text": n.get("text"), "description": n.get("content-desc"),
                   "bounds": n.get("bounds")} for n in root.iter("node")
                  if n.get("text") or n.get("content-desc")]
        print(json.dumps(labels[:50], ensure_ascii=False))
        return
    actions = {"tap": lambda: ui.tap(args.text, args.description),
               "scroll": lambda: ui.scroll(args.direction),
               "back": lambda: ui.adb("shell", "input", "keyevent", "4")}
    result = ui.sample(args.label, actions[args.action])
    if args.output:
        path = Path(args.output)
        records = json.loads(path.read_text()) if path.exists() else []
        records.append(result)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(records, ensure_ascii=False, indent=2)+"\n")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
