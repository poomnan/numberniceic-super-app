#!/bin/bash
echo "🚀 Uploading Static JSON files to Server..."
echo "Target: tayap@43.228.85.200:/home/tayap/ananya-php/public/"
echo "--------------------------------------------------------"
echo "Please enter your Server SSH Password when prompted:"

scp /Users/tayap/project-number/number-php/public/static_tabian.json /Users/tayap/project-number/number-php/public/static_news.json tayap@43.228.85.200:/home/tayap/ananya-php/public/

echo "--------------------------------------------------------"
echo "✅ Upload Complete! Try refreshing the App."
