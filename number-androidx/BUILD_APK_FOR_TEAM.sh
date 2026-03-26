#!/bin/bash

# Define version
VERSION_NAME="v4.6.5"
VERSION_CODE="1028"
OUTPUT_NAME="NumberNiceic_${VERSION_NAME}_${VERSION_CODE}_Debug.apk"

echo "🔧 Preparing build environment..."
# 1. Kill existing Gradle daemons to free up locks
pkill -f 'java.*gradle' || true

# 2. Try to remove the lock files that cause permission errors
echo "🧹 Cleaning up lock files..."
rm -f ~/.gradle/wrapper/dists/gradle-9.1.0-bin/*/gradle-9.1.0-bin.zip.lck 2>/dev/null

echo "🚀 Building APK version $VERSION_NAME..."

# 3. Build Debug APK
./gradlew assembleDebug --no-daemon

if [ $? -eq 0 ]; then
    echo "✅ Build Successful!"
    cp app/build/outputs/apk/debug/app-debug.apk "$OUTPUT_NAME"
    echo "📦 APK created: $OUTPUT_NAME"
    echo "📂 Location: $(pwd)/$OUTPUT_NAME"
    
    # Reveal in Finder
    open -R "$OUTPUT_NAME"
else
    echo "❌ Build Failed."
    echo ""
    echo "👉 If you still see 'Operation not permitted' errors, please try this:"
    echo "   1. Open Finder"
    echo "   2. Go to folder: ~/.gradle/wrapper/dists/gradle-9.1.0-bin/"
    echo "   3. Delete the folder inside it"
    echo "   4. Run this script again"
fi
