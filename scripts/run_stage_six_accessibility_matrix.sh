#!/usr/bin/env bash
set -euo pipefail

serial="${ANDROID_SERIAL:-emulator-5554}"
if [[ "$serial" != emulator-* ]]; then
  echo "Refusing to run accessibility matrix on non-emulator serial: $serial" >&2
  exit 2
fi

project_root="$(cd "$(dirname "$0")/.." && pwd)"
app_id="com.nanzhufeng.nanfengbazi"
runner="com.nanzhufeng.nanfengbazi.test/androidx.test.runner.AndroidJUnitRunner"
test_class="com.nanzhufeng.nanfengbazi.StageSixAccessibilityTest"
talkback_service="$(
  printf '%s' \
    "com.google.android.marvin.talkback/" \
    "com.google.android.marvin.talkback.TalkBackService"
)"
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
test_apk="$project_root/app/build/outputs/apk/androidTest/debug/$artifact_prefix-debug-androidTest.apk"
test_output="$(mktemp -t nanfeng-stage-six-accessibility.XXXXXX)"

old_override_size="$(
  adb -s "$serial" shell wm size |
    sed -n 's/^Override size: //p' |
    tr -d '\r'
)"
old_font_scale="$(
  adb -s "$serial" shell settings get system font_scale | tr -d '\r'
)"
old_services="$(
  adb -s "$serial" shell settings get secure enabled_accessibility_services |
    tr -d '\r'
)"
old_accessibility="$(
  adb -s "$serial" shell settings get secure accessibility_enabled | tr -d '\r'
)"
old_touch_exploration="$(
  adb -s "$serial" shell settings get secure touch_exploration_enabled | tr -d '\r'
)"
if adb -s "$serial" shell dumpsys package com.google.android.marvin.talkback |
  grep -q 'android.permission.POST_NOTIFICATIONS: granted=true'; then
  old_talkback_notifications=true
else
  old_talkback_notifications=false
fi

restore_setting() {
  local namespace="$1"
  local key="$2"
  local value="$3"
  if [[ "$value" == "null" || -z "$value" ]]; then
    adb -s "$serial" shell settings delete "$namespace" "$key" >/dev/null
  else
    adb -s "$serial" shell settings put "$namespace" "$key" "$value"
  fi
}

cleanup() {
  if [[ -z "$old_override_size" ]]; then
    adb -s "$serial" shell wm size reset >/dev/null
  else
    adb -s "$serial" shell wm size "$old_override_size" >/dev/null
  fi
  restore_setting system font_scale "$old_font_scale"
  restore_setting secure enabled_accessibility_services "$old_services"
  restore_setting secure accessibility_enabled "$old_accessibility"
  restore_setting secure touch_exploration_enabled "$old_touch_exploration"
  if [[ "$old_talkback_notifications" == true ]]; then
    adb -s "$serial" shell pm grant com.google.android.marvin.talkback \
      android.permission.POST_NOTIFICATIONS
  else
    adb -s "$serial" shell pm revoke com.google.android.marvin.talkback \
      android.permission.POST_NOTIFICATIONS
  fi
  rm -f "$test_output"
}
trap cleanup EXIT

set_talkback() {
  local enabled="$1"
  if [[ "$enabled" == true ]]; then
    adb -s "$serial" shell settings put secure \
      enabled_accessibility_services "$talkback_service"
    adb -s "$serial" shell settings put secure accessibility_enabled 1
    adb -s "$serial" shell settings put secure touch_exploration_enabled 1
  else
    adb -s "$serial" shell settings delete secure enabled_accessibility_services >/dev/null
    adb -s "$serial" shell settings put secure accessibility_enabled 0
    adb -s "$serial" shell settings put secure touch_exploration_enabled 0
  fi
}

run_case() {
  local label="$1"
  local size="$2"
  local font_scale="$3"
  local talkback="$4"
  if [[ "$size" == reset ]]; then
    adb -s "$serial" shell wm size reset >/dev/null
  else
    adb -s "$serial" shell wm size "$size" >/dev/null
  fi
  adb -s "$serial" shell settings put system font_scale "$font_scale"
  set_talkback "$talkback"
  adb -s "$serial" shell am force-stop "$app_id"
  adb -s "$serial" shell am instrument -w \
    -e class "$test_class" \
    "$runner" >"$test_output" 2>&1
  if ! grep -q 'OK (1 test)' "$test_output" ||
    grep -q 'FAILURES!!!' "$test_output"; then
    sed -n '1,180p' "$test_output" >&2
    echo "Accessibility matrix failed: $label" >&2
    exit 1
  fi
  echo "Accessibility matrix passed: $label"
}

adb -s "$serial" get-state >/dev/null
test -f "$app_apk"
test -f "$test_apk"
adb -s "$serial" install -r "$app_apk" >/dev/null
adb -s "$serial" install -r "$test_apk" >/dev/null
adb -s "$serial" shell pm grant com.google.android.marvin.talkback \
  android.permission.POST_NOTIFICATIONS

run_case "mobile / 1.0 font / TalkBack off" reset 1.0 false
run_case "mobile / 2.0 font / TalkBack on" reset 2.0 true
run_case "expanded / 1.0 font / TalkBack off" 2600x2200 1.0 false
run_case "expanded / 2.0 font / TalkBack on" 2600x2200 2.0 true

echo "Stage 6 accessibility matrix passed all 4 viewport and accessibility combinations."
