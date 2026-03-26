#!/bin/bash

# Define version
VERSION_NAME="v4.6.6"
OUTPUT_NAME="NumberNiceic_${VERSION_NAME}.apk"

echo "🔧 Preparing build environment..."
# Kill existing Gradle daemons to free up locks
pkill -f 'java.*gradle' || true

echo "🚀 Building Release APK version $VERSION_NAME..."

# Build Release APK
./gradlew assembleRelease

if [ $? -eq 0 ]; then
    echo "✅ Build Successful!"
    # The release APK typically needs signing. Assuming signing config is set up in build.gradle (which I saw earlier).
    # Path is usually app/build/outputs/apk/release/app-release.apk
    
    if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
        cp app/build/outputs/apk/release/app-release.apk "$OUTPUT_NAME"
        echo "📦 APK created: $OUTPUT_NAME"
        echo "📂 Location: $(pwd)/$OUTPUT_NAME"
        
        # Reveal in Finder
        open -R "$OUTPUT_NAME"
    else
        echo "⚠️  Build success but APK file not found at expected path."
    fi
else
    echo "❌ Build Failed."
fi
