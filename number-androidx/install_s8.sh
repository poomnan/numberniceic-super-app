#!/bin/bash
DEVICE_ID="ce0517151aee3c680d"
APK_PATH=$(ls app/build/outputs/apk/debug/NumberNice_v*.apk | head -n 1)
PACKAGE_NAME="com.numberniceic"

echo "🚀 Starting Robust Install for Samsung S8+ ($DEVICE_ID)..."

if [ -z "$APK_PATH" ]; then
    echo "❌ Error: APK not found. Please run ./gradlew assembleDebug first."
    exit 1
fi

echo "📦 Found APK: $APK_PATH"

# 1. Force kill existing app to prevent lock
echo "🛑 Closing existing app..."
adb -s $DEVICE_ID shell am force-stop $PACKAGE_NAME

# 2. Try to uninstall first (to avoid Signature/Update conflicts)
echo "🗑️  Uninstalling old version (to prevent Signature mismatch)..."
adb -s $DEVICE_ID uninstall $PACKAGE_NAME

echo "📥 Installing new version..."
echo "⚠️  IMPORTANT: Please look at your Samsung S8+ screen NOW!"
echo "⚠️  If a 'Security/Allow Install' popup appears, you MUST press 'Allow' or 'Install'."
echo "----------------------------------------------------------------"

# 3. Install with -t (test) and -g (grant permissions) flags
adb -s $DEVICE_ID install -r -t -g "$APK_PATH"

if [ $? -eq 0 ]; then
    echo "✅ Install Successful!"
    echo "🚀 Launching app..."
    adb -s $DEVICE_ID shell am start -n $PACKAGE_NAME/com.numberniceic.ui.SplashActivity
else
    echo "❌ Install failed. If it hung at 'Performing Streamed Install', it's likely you didn't press 'Allow' on the phone screen fast enough."
fi
