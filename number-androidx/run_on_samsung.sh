#!/bin/bash
echo "🚀 Starting build for Samsung S8..."

# Try to clean first
./gradlew clean

# Build Release and Install
echo "📦 Building and Installing Release..."
./gradlew installRelease

if [ $? -ne 0 ]; then
    echo "❌ Build Failed. Please check the errors above."
    
    # Check for the specific lock file error and suggest fix
    if [ -f "/Users/tayap/.gradle/wrapper/dists/gradle-9.1.0-bin/9agqghryom9wkf8r80qlhnts3/gradle-9.1.0-bin.zip.lck" ]; then
        echo "⚠️  It looks like a Gradle lock file is causing issues."
        echo "You can try running this command to fix it:"
        echo "sudo rm /Users/tayap/.gradle/wrapper/dists/gradle-9.1.0-bin/9agqghryom9wkf8r80qlhnts3/gradle-9.1.0-bin.zip.lck"
    fi
    exit 1
fi

echo "✅ Build Success!"

# Launch the app
echo "📱 Launching App..."
devices=$(adb devices | grep -w "device" | awk '{print $1}')
for device in $devices
do
    echo "Launching on $device..."
    adb -s $device shell am start -n com.numberniceic/.ui.SplashActivity > /dev/null
done

echo "✨ Done."
