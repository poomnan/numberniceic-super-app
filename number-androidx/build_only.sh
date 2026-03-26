#!/bin/bash
export TMPDIR=/tmp
cd "$(dirname "$0")"
echo "📦 Building Release APK Only..."
./gradlew assembleRelease
