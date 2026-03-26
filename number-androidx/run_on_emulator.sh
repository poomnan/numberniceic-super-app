#!/bin/bash
echo "🚀 Starting build for Emulator..."

# Try to clean first
./gradlew clean

# Build Debug
echo "📦 Building Debug APK..."
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ Build Failed. Please check the errors above."
    exit 1
fi

echo "✅ Build Success!"

echo "📱 Detecting Emulators..."
# Get only the emulator device serial
device=$(adb devices | grep "emulator" | head -n 1 | awk '{print $1}')

if [ -z "$device" ]; then
    echo "⚠️ No emulator detected."
    exit 1
fi

echo "🎯 Target Emulator: $device"

# Try to uninstall completely for all users (just in case)
echo "🧹 Trying to cleanup old installation..."
adb -s $device shell pm uninstall --user 0 com.numberniceic > /dev/null 2>&1
adb -s $device uninstall com.numberniceic > /dev/null 2>&1

# Install with flags: -r (reinstall/replace) -t (allow test packages) -g (grant permissions)
echo "📦 Installing APK..."
adb -s $device install -r -t -g app/build/outputs/apk/debug/app-debug.apk

if [ $? -eq 0 ]; then
    echo "✅ Installed successfully!"
    echo "📱 Launching App..."
    adb -s $device shell am start -n com.numberniceic/.ui.SplashActivity > /dev/null
else
    echo "❌ Install Failed!"
    echo "💡 Recommendation: Please Factory Reset the Emulator if the error persists."
fi

echo "✨ Done."
