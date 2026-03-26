#!/bin/bash

# --- Project Settings ---
PACKAGE_NAME="com.numberniceic"
LAUNCH_ACTIVITY=".ui.SplashActivity"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
S8_SERIAL="ce0517151aee3c680d"

echo "🚀 Starting Deployment to Samsung S8 and Emulator..."

# 1. Start Emulator if needed
AVD_NAME="Pixel_5_API_33"
echo "📱 Checking Emulator..."
EMULATOR_DEVICE=$(adb devices | grep "emulator" | cut -f1 | head -n 1)

if [ -z "$EMULATOR_DEVICE" ]; then
    echo "⚙️ Starting Emulator ($AVD_NAME)..."
    # Start emulator in background
    /Users/tayap/Library/Android/sdk/emulator/emulator -avd $AVD_NAME > /dev/null 2>&1 &
    
    echo "⏳ Waiting for emulator to start (this may take a minute)..."
    # Wait for adb to see a device
    adb wait-for-device
    
    # Get the device name again
    EMULATOR_DEVICE=$(adb devices | grep "emulator" | cut -f1 | head -n 1)
    
    # Wait for the system to be fully booted
    BOOT_COMPLETED=""
    while [ "$BOOT_COMPLETED" != "1" ]; do
        sleep 2
        BOOT_COMPLETED=$(adb -s "$EMULATOR_DEVICE" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
        echo -n "."
    done
    echo ""
fi
echo "✅ Emulator ready: $EMULATOR_DEVICE"

# 2. Install on Samsung S8
echo "📲 Installing on Samsung S8 ($S8_SERIAL)..."
adb -s $S8_SERIAL install -r $APK_PATH
if [ $? -eq 0 ]; then
    echo "✅ S8: Installation successful!"
    adb -s $S8_SERIAL shell am start -n "$PACKAGE_NAME/$LAUNCH_ACTIVITY"
else
    echo "❌ S8: Installation failed (Is it connected?)"
fi

# 3. Install on Emulator
echo "📲 Installing on Emulator ($EMULATOR_DEVICE)..."
adb -s $EMULATOR_DEVICE install -r $APK_PATH
if [ $? -eq 0 ]; then
    echo "✅ Emulator: Installation successful!"
    adb -s $EMULATOR_DEVICE shell am start -n "$PACKAGE_NAME/$LAUNCH_ACTIVITY"
else
    echo "❌ Emulator: Installation failed"
fi

echo "🎉 Finished!"
