#!/bin/bash
echo "♻️ Restarting ADB Server..."
adb kill-server
adb start-server
sleep 2

echo "📱 Checking devices..."
adb devices

echo "📦 Installing to Emulator (emulator-5554)..."
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk

echo "📦 Installing to Samsung S8 (ce0517151aee3c680d)..."
adb -s ce0517151aee3c680d install -r app/build/outputs/apk/debug/app-debug.apk

echo "✨ Installation Complete!"
