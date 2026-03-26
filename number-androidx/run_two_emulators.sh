#!/bin/bash

# Path to Android Emulator
EMULATOR_PATH="/Users/tayap/Library/Android/sdk/emulator/emulator"

# Available AVDs:
# - Medium_Tablet
# - Pixel_3a_API_34_extension_level_7_arm64-v8a
# - Pixel_5_API_33
# - seven-tablet

AVD1="Pixel_5_API_33"
AVD2="Pixel_3a_API_34_extension_level_7_arm64-v8a"

if [ ! -f "$EMULATOR_PATH" ]; then
    echo "Error: Emulator tool not found."
    exit 1
fi

echo "🚀 Starting Emulator 1: $AVD1..."
"$EMULATOR_PATH" -avd "$AVD1" -netdelay none -netspeed full > /dev/null 2>&1 &

sleep 2

echo "🚀 Starting Emulator 2: $AVD2..."
"$EMULATOR_PATH" -avd "$AVD2" -netdelay none -netspeed full > /dev/null 2>&1 &

echo "✅ Both emulators are starting in the background."
echo "Please wait a moment for the windows to appear."
