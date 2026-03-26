#!/bin/bash

# Configuration
PKG="com.numberniceic"
ACTIVITY=".ui.SplashActivity"

# Helper to launch
launch_app() {
    local device=$1
    echo "--------"
    echo "🚀 Deploying to $device..."
    adb -s $device install -r app/build/outputs/apk/debug/app-debug.apk
    adb -s $device shell am start -n $PKG/$ACTIVITY
}

# 1. Build first (once)
echo "🔨 Building APK..."
./gradlew assembleDebug

# 2. Get all connected devices and launch
echo "📱 Detecting devices..."
adb devices | grep "\tdevice" | cut -f1 | while read device; do
    launch_app "$device" &
done

wait
echo "✅ Done! App launched on all devices."
