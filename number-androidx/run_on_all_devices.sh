#!/bin/bash

# Configuration
PACKAGE_NAME="com.numberniceic"
ACTIVITY_NAME="com.numberniceic.ui.SplashActivity"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "🚀 Starting Run Sequence..."

# 1. Check for connected devices
DEVICES=$(adb devices | grep -w "device" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
    echo "❌ No connected Android devices found!"
    echo "   Please connect your device via USB or start an emulator."
    exit 1
fi

echo "📱 Found devices:"
echo "$DEVICES"
echo "--------------------------------------------------"

# 2. Build the APK
echo "🔨 Building APK (Debug)..."
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ Build Failed!"
    exit 1
fi

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at $APK_PATH"
    exit 1
fi

echo "✅ Build Success!"
echo "--------------------------------------------------"

# 3. Iterate through devices and install/run
for DEVICE in $DEVICES; do
    echo "📲 Processing Device: $DEVICE"
    
    # Install
    echo "   📦 Installing APK..."
    install_output=$(adb -s "$DEVICE" install -r "$APK_PATH" 2>&1)
    
    if [[ "$install_output" == *"Success"* ]]; then
         echo "   ✅ Install Success"
    else
         echo "   ⚠️ Install Warning/Error: $install_output"
         
         # Check for signature mismatch which requires uninstall
         if [[ "$install_output" == *"INSTALL_FAILED_UPDATE_INCOMPATIBLE"* ]]; then
             echo "   🔄 Signature mismatch detected. Uninstalling old version..."
             adb -s "$DEVICE" uninstall "$PACKAGE_NAME"
             
             echo "   📦 Re-installing..."
             adb -s "$DEVICE" install -r "$APK_PATH"
         fi
    fi

    # Launch
    echo "   🚀 Launching App..."
    adb -s "$DEVICE" shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
    
    echo "   ✨ Done for $DEVICE"
    echo "--------------------------------------------------"
done

echo "🎉 All operations completed successfully!"
