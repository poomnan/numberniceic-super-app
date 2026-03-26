#!/bin/bash
# Script to run Flutter project (chuedee-flutter)

PROJECT_PATH="../chuedee-flutter"

echo "Changing directory to $PROJECT_PATH..."
cd "$PROJECT_PATH" || { echo "Error: Directory not found"; exit 1; }

# You can add -d <DEVICE_ID> if you want to target a specific device
echo "Running Flutter application..."
flutter run
