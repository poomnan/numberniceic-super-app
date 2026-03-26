#!/bin/bash
export TMPDIR=/tmp
cd /Users/tayap/project-number/number-android

echo "📦 Cleaning and Building..."
# Try to kill any stale daemons
pkill -f java || true

./gradlew clean assembleRelease --no-daemon --stacktrace

if [ $? -eq 0 ]; then
    echo "✅ Build Successful!"
else
    echo "❌ Build Failed"
fi
