#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PUBSPEC_FILE="$PROJECT_ROOT/pubspec.yaml"
ANDROID_LOCAL_PROPERTIES="$PROJECT_ROOT/android/local.properties"
OUTPUT_DIR="$PROJECT_ROOT/build/app/outputs/bundle/release"

cd "$PROJECT_ROOT"

if [[ ! -f "$PUBSPEC_FILE" ]]; then
  echo "pubspec.yaml not found"
  exit 1
fi

if [[ ! -f "$ANDROID_LOCAL_PROPERTIES" ]]; then
  echo "android/local.properties not found"
  exit 1
fi

VERSION_LINE="$(grep '^version:' "$PUBSPEC_FILE" | head -n 1 | tr -d '\r')"
VERSION_FULL="${VERSION_LINE#version: }"
VERSION_NAME="${VERSION_FULL%%+*}"
VERSION_CODE="${VERSION_FULL##*+}"

echo "Building Play Store bundle for versionName=$VERSION_NAME versionCode=$VERSION_CODE"

flutter clean
flutter pub get
flutter build appbundle --release

AAB_SOURCE="$OUTPUT_DIR/app-release.aab"
AAB_TARGET="$PROJECT_ROOT/Chuedee_v${VERSION_NAME}_${VERSION_CODE}_signed.aab"

if [[ ! -f "$AAB_SOURCE" ]]; then
  echo "Expected bundle not found at $AAB_SOURCE"
  exit 1
fi

cp "$AAB_SOURCE" "$AAB_TARGET"

echo ""
echo "Build complete"
echo "Source bundle: $AAB_SOURCE"
echo "Deploy bundle: $AAB_TARGET"
