#!/bin/bash
export TMPDIR=/tmp
cd "$(dirname "$0")"

echo "📦 Starting Full Build and Deployment..."
# Removed redundant steps here as they are handled below per-device.


echo "📱 Detecting connected devices..."
devices=$(adb devices | grep -w "device" | awk '{print $1}')

if [ -z "$devices" ]; then
    echo "⚠️ No devices connected"
    exit 1
fi

echo "🧹 Cleaning up old installations on all devices..."
for device in $devices
do
    echo "  - Cleaning $device..."
    adb -s $device shell pm uninstall --user 0 com.numberniceic > /dev/null 2>&1
    adb -s $device uninstall com.numberniceic > /dev/null 2>&1
done

echo "📦 Building Release APK..."
./gradlew assembleRelease

if [ $? -ne 0 ]; then
    echo "❌ Build Failed"
    exit 1
fi

echo "🚀 Installing and Launching on all devices..."
APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
if [ ! -f "$APK_PATH" ]; then
    # Fallback if the path is different
    APK_PATH=$(find app/build/outputs/apk/release -name "*.apk" | head -n 1)
fi

for device in $devices
do
   echo "📱 Installing on $device..."
   adb -s $device install -r -t -g "$APK_PATH"
   echo "🎬 Starting on $device..."
   adb -s $device shell am start -n com.numberniceic/.ui.SplashActivity
done

echo "✅ Done!"
