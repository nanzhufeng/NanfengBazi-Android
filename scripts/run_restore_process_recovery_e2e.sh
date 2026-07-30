#!/usr/bin/env bash
set -euo pipefail

serial="${ANDROID_SERIAL:-emulator-5554}"
if [[ "$serial" != emulator-* ]]; then
  echo "Refusing to run process-recovery E2E on non-emulator serial: $serial" >&2
  exit 2
fi

project_root="$(cd "$(dirname "$0")/.." && pwd)"
app_id="com.nanzhufeng.nanfengbazi"
runner="com.nanzhufeng.nanfengbazi.test/androidx.test.runner.AndroidJUnitRunner"
test_class="com.nanzhufeng.nanfengbazi.RestoreProcessRecoveryDeviceTest"
transaction_id="44444444-4444-4444-8444-444444444444"
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
seed_output="$(mktemp -t nanfeng-restore-seed.XXXXXX)"

cleanup() {
  rm -f "$seed_output"
}
trap cleanup EXIT

adb -s "$serial" get-state >/dev/null
test -f "$app_apk"
test -f "$test_apk"
adb -s "$serial" install -r "$app_apk" >/dev/null
adb -s "$serial" install -r "$test_apk" >/dev/null

adb -s "$serial" shell am instrument -w \
  -e processRecoveryPhase seed \
  -e class "$test_class#seedInterruptedRestoreAndWaitForHostKill" \
  "$runner" >"$seed_output" 2>&1 &
seed_host_pid=$!

ready=false
for _ in $(seq 1 30); do
  if adb -s "$serial" shell run-as "$app_id" \
    test -f files/restore-process-ready; then
    ready=true
    break
  fi
  sleep 1
done
if [[ "$ready" != true ]]; then
  kill "$seed_host_pid" 2>/dev/null || true
  wait "$seed_host_pid" 2>/dev/null || true
  echo "Seed phase did not reach the persisted interruption boundary." >&2
  exit 1
fi

seed_pid="$(adb -s "$serial" shell run-as "$app_id" \
  cat files/restore-process-ready | tr -d '\r')"
test -n "$seed_pid"
adb -s "$serial" shell run-as "$app_id" \
  test -f "files/attachments/.restore-journal/$transaction_id.json"
adb -s "$serial" shell run-as "$app_id" \
  test -f "files/attachments/restored/$transaction_id/orphan-attachment"
adb -s "$serial" shell am force-stop "$app_id"
wait "$seed_host_pid" 2>/dev/null || true
test -z "$(adb -s "$serial" shell pidof "$app_id" | tr -d '\r')"
adb -s "$serial" shell run-as "$app_id" \
  test -f "files/attachments/.restore-journal/$transaction_id.json"

adb -s "$serial" shell am instrument -w \
  -e processRecoveryPhase verify \
  -e class "$test_class#relaunchFinalizesInterruptedRestore" \
  "$runner"

for target in \
  "files/attachments/.restore-journal/$transaction_id.json" \
  "files/attachments/.restore-staging/$transaction_id" \
  "files/attachments/restored/$transaction_id"; do
  if adb -s "$serial" shell run-as "$app_id" test -e "$target"; then
    echo "Recovery artifact remains: $target" >&2
    exit 1
  fi
done

echo "Process recovery E2E passed; killed app PID $seed_pid and verified cold-start cleanup."
