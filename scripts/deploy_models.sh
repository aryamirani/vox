#!/usr/bin/env bash
# Push converted SNPE models to a connected Android device.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEVICE_PATH="/sdcard/vox/models"

echo "==> Deploying models to $DEVICE_PATH"
adb shell mkdir -p "$DEVICE_PATH/whisper" "$DEVICE_PATH/phi3"
adb push "$ROOT/models/whisper/." "$DEVICE_PATH/whisper/"
adb push "$ROOT/models/phi3/." "$DEVICE_PATH/phi3/"
echo "Done. Restart the Vox app to pick up new models."
