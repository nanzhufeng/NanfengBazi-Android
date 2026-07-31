#!/usr/bin/env bash
set -euo pipefail

serial="${ANDROID_SERIAL:-emulator-5554}"
if [[ "$serial" != emulator-* ]]; then
  echo "Refusing to run Stage 2 process-state E2E on non-emulator serial: $serial" >&2
  exit 2
fi

project_root="$(cd "$(dirname "$0")/.." && pwd)"
app_id="com.nanzhufeng.nanfengbazi"
draft_alias="ProcessStateDraft"
version_name="$(
  sed -n 's/^[[:space:]]*versionName = "\(.*\)"/\1/p' \
    "$project_root/app/build.gradle.kts"
)"
if [[ -z "$version_name" ]]; then
  echo "Unable to resolve versionName from app/build.gradle.kts." >&2
  exit 1
fi
artifact_prefix="NanfengBazi-Android-v$version_name"
app_apk="$project_root/app/build/outputs/apk/debug/$artifact_prefix-debug.apk"
ui_dump_raw="$(mktemp -t nanfeng-stage-two-state-raw.XXXXXX)"
ui_dump_xml="$(mktemp -t nanfeng-stage-two-state-xml.XXXXXX)"

cleanup() {
  rm -f "$ui_dump_raw" "$ui_dump_xml"
}
trap cleanup EXIT

refresh_ui_dump() {
  adb -s "$serial" exec-out uiautomator dump /dev/tty >"$ui_dump_raw"
  sed 's#UI hierchary dumped to: /dev/tty##' "$ui_dump_raw" >"$ui_dump_xml"
}

bounds_for_text() {
  local text="$1"
  refresh_ui_dump
  xmllint --xpath "string(//node[@text='$text']/@bounds)" "$ui_dump_xml"
}

wait_for_text() {
  local text="$1"
  local bounds=""
  for _ in $(seq 1 10); do
    bounds="$(bounds_for_text "$text")"
    if [[ -n "$bounds" ]]; then
      printf '%s' "$bounds"
      return 0
    fi
    sleep 0.5
  done
  echo "Unable to find UI text: $text" >&2
  return 1
}

tap_bounds() {
  local bounds="$1"
  local coordinates
  coordinates="$(
    printf '%s' "$bounds" |
      sed -E 's/^\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]$/\1 \2 \3 \4/'
  )"
  read -r left top right bottom <<<"$coordinates"
  adb -s "$serial" shell input tap "$(((left + right) / 2))" "$(((top + bottom) / 2))"
}

adb -s "$serial" get-state >/dev/null
test -f "$app_apk"
adb -s "$serial" install -r "$app_apk" >/dev/null
adb -s "$serial" shell am force-stop "$app_id"
adb -s "$serial" shell am start -W -n "$app_id/.MainActivity" >/dev/null

tap_bounds "$(wait_for_text "排盘")"
tap_bounds "$(wait_for_text "命例别名 *")"
adb -s "$serial" shell input text "$draft_alias"
wait_for_text "$draft_alias" >/dev/null
adb -s "$serial" shell input keyevent KEYCODE_HOME
sleep 1

killed_pid="$(
  adb -s "$serial" shell pidof "$app_id" | tr -d '\r'
)"
test -n "$killed_pid"

adb -s "$serial" shell am kill "$app_id"
for _ in $(seq 1 20); do
  if [[ -z "$(adb -s "$serial" shell pidof "$app_id" | tr -d '\r')" ]]; then
    break
  fi
  sleep 0.25
done
if [[ -n "$(adb -s "$serial" shell pidof "$app_id" | tr -d '\r')" ]]; then
  echo "System did not kill background app PID $killed_pid." >&2
  exit 1
fi

adb -s "$serial" shell am start -W -n "$app_id/.MainActivity" >/dev/null
restored_pid="$(
  adb -s "$serial" shell pidof "$app_id" | tr -d '\r'
)"
test -n "$restored_pid"
if [[ "$restored_pid" == "$killed_pid" ]]; then
  echo "App did not restart in a new process after killing PID $killed_pid." >&2
  exit 1
fi
wait_for_text "$draft_alias" >/dev/null
wait_for_text "新建命例" >/dev/null

echo "Stage 2 process-state E2E passed; killed PID $killed_pid, restarted as PID $restored_pid, and restored its draft."
