#!/bin/bash
# Script to run PHP project (ananya-php)

PROJECT_PATH="../../apps-php/ananya-php"

echo "Changing directory to $PROJECT_PATH..."
cd "$PROJECT_PATH" || { echo "Error: Directory not found"; exit 1; }

# Starting PHP built-in server
# Default port 8000, pointing to public/ directory
echo "Starting PHP built-in server at http://localhost:8000..."
php -S localhost:8000 -t public/
