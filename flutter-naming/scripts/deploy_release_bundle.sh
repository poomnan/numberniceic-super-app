#!/bin/bash

set -euo pipefail

PROJECT_DIR="/Users/tayap/Numberniceic-Super-Apps/flutter-naming"
PUBSPEC_FILE="$PROJECT_DIR/pubspec.yaml"
OUTPUT_DIR="$PROJECT_DIR/build/app/outputs/bundle/release"

VERSION_LINE=$(grep '^version:' "$PUBSPEC_FILE" | head -n 1 | awk '{print $2}')
VERSION_NAME="${VERSION_LINE%%+*}"
VERSION_CODE="${VERSION_LINE##*+}"

if [[ -z "$VERSION_NAME" || -z "$VERSION_CODE" ]]; then
  echo "ไม่สามารถอ่าน version จาก pubspec.yaml ได้"
  exit 1
fi

echo "==> Building Chuedee $VERSION_NAME ($VERSION_CODE)"
cd "$PROJECT_DIR"

flutter clean
flutter pub get
flutter build appbundle --release

SOURCE_AAB="$OUTPUT_DIR/app-release.aab"
TARGET_AAB="$PROJECT_DIR/Chuedee_v${VERSION_NAME}_${VERSION_CODE}_signed.aab"

if [[ ! -f "$SOURCE_AAB" ]]; then
  echo "ไม่พบไฟล์ AAB ที่ $SOURCE_AAB"
  exit 1
fi

cp "$SOURCE_AAB" "$TARGET_AAB"

echo "==> Build complete"
echo "AAB: $TARGET_AAB"
