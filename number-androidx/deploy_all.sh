#!/bin/bash

# --- Configuration ---
PACKAGE_NAME="com.numberniceic"
LAUNCH_ACTIVITY=".ui.SplashActivity"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "🚀 Starting Multi-Device Deploy & Test Tool..."
echo "------------------------------------------------"

# 1. Build the APK
echo "📦 Step 1: Building Debug APK..."
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ Build Failed! Please check the errors above."
    exit 1
fi

# 2. Identify Devices
echo "🔍 Step 2: Detecting Devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device$" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
    echo "❌ No devices or emulators found. Please connect your S8 or start the emulator."
    exit 1
fi

echo "Found devices:"
echo "$DEVICES"
echo "------------------------------------------------"

# 3. Deploy to each device
for DEVICE_ID in $DEVICES
do
    echo "📲 Deploying to [$DEVICE_ID]..."
    
    # Uninstall old version to ensure clean state (Optional, but recommended for testing)
    # adb -s $DEVICE_ID uninstall $PACKAGE_NAME
    
    # Install the new APK
    adb -s $DEVICE_ID install -r "$APK_PATH"
    
    if [ $? -eq 0 ]; then
        echo "✅ Installed successfully on [$DEVICE_ID]"
        
        # Start the App
        echo "🎬 Launching App on [$DEVICE_ID]..."
        adb -s $DEVICE_ID shell am start -n "$PACKAGE_NAME/$LAUNCH_ACTIVITY"
    else
        echo "⚠️ Failed to install on [$DEVICE_ID]"
    fi
    echo "------------------------------------------------"
done

echo "🎉 All Done! Test your changes on both screens now."
