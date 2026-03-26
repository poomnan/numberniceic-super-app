#!/bin/bash
APK_PATH=$(ls app/build/outputs/apk/debug/NumberNice_v*.apk | head -n 1)
PACKAGE_NAME="com.numberniceic"
START_ACTIVITY="com.numberniceic.ui.SplashActivity"

if [ -z "$APK_PATH" ]; then
    echo "❌ APK not found! Run ./gradlew assembleDebug first."
    exit 1
fi

# Get list of online devices
DEVICES=$(adb devices | grep -v "List" | grep "device$" | cut -f1)

if [ -z "$DEVICES" ]; then
    echo "❌ No devices detected!"
    exit 1
fi

echo "🚀 Found APK: $APK_PATH"
echo "📱 Found devices: "
echo "$DEVICES"
echo "------------------------------------------------"

# Function to handle single device install and launch
install_and_launch() {
    local ID=$1
    echo "📲 [Target: $ID] Starting..."
    
    # Force Stop
    adb -s "$ID" shell am force-stop "$PACKAGE_NAME" > /dev/null 2>&1
    
    # Uninstall first to prevent Signature Mismatch / Incompatible update errors
    echo "🧹 [Target: $ID] Uninstalling old version..."
    adb -s "$ID" uninstall "$PACKAGE_NAME" > /dev/null 2>&1
    
    # Install (using -r -t -g)
    echo "📥 [Target: $ID] Installing... (Waiting for device response)"
    if [[ "$ID" == *"ce05"* ]]; then
        echo "⚠️  CHECK SAMSUNG SCREEN ($ID) NOW -> PRESS 'ALLOW' !"
    fi
    
    INSTALL_RESULT=$(adb -s "$ID" install -r -t -g "$APK_PATH" 2>&1)
    
    if [[ "$INSTALL_RESULT" == *"Success"* ]]; then
        echo "✅ [Target: $ID] Install Successful!"
        echo "🚀 [Target: $ID] Launching app..."
        adb -s "$ID" shell am start -n "$PACKAGE_NAME/$START_ACTIVITY" > /dev/null 2>&1
    else
        echo "❌ [Target: $ID] Install Failed or Timed out!"
        echo "$INSTALL_RESULT"
    fi
}

# Run for each device in BACKGROUND
for ID in $DEVICES; do
    install_and_launch "$ID" &
done

echo "⏳ Triggered installation for all devices. Waiting for completions..."
wait
echo "✨ All operations finished."
