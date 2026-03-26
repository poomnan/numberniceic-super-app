#!/bin/bash
echo "🧹 Cleaning project..."
./gradlew clean

echo "🔨 Building APK (this may take a while)..."
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ Build Failed!"
    exit 1
fi

echo "✅ Build Success!"
echo "📂 Copying APK..."
cp app/build/outputs/apk/debug/app-debug.apk NumberNiceic_v4.5_Beta.apk

echo "📱 Detecting devices..."
devices=$(adb devices | grep -w "device" | awk '{print $1}')

if [ -z "$devices" ]; then
    echo "⚠️ No devices connected"
    exit 1
fi

for device in $devices
do
   echo "--------------------------------------------------"
   echo "🚀 Processing device: $device"
   echo "📦 Installing APK..."
   adb -s $device install -r "NumberNiceic_v4.5_Beta.apk"
   
   echo "📱 Launching App..."
   adb -s $device shell am start -n com.numberniceic/.ui.SplashActivity > /dev/null
done
echo "--------------------------------------------------"
echo "✨ DONE - V2 INSTALLED"
