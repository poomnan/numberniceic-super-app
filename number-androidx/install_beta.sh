#!/bin/bash
apk="NumberNiceic_v4.5_Beta.apk"

if [ ! -f "$apk" ]; then
    echo "❌ APK $apk not found!"
    exit 1
fi

echo "📱 Detecting devices..."
devices=$(adb devices | grep -w "device" | awk '{print $1}')

if [ -z "$devices" ]; then
    echo "⚠️ No devices connected"
    exit 1
fi

# Convert newlines to spaces for loop
for device in $devices
do
   echo "--------------------------------------------------"
   echo "🚀 Processing device: $device"
   echo "📦 Installing APK..."
   result=$(adb -s $device install -r "$apk" 2>&1)
   
   if echo "$result" | grep -q "Success"; then
       echo "✅ Installed successfully on $device"
       echo "📱 Launching App..."
       adb -s $device shell am start -n com.numberniceic/.ui.SplashActivity > /dev/null
   else
       echo "❌ Failed to install on $device"
       echo "Error output: $result"
   fi
done
echo "--------------------------------------------------"
echo "✨ Done."
