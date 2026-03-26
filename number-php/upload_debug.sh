#!/bin/bash
# Upload debug_tabian.php to the server public directory
echo "🚀 Starting Upload: debug_tabian.php"
echo "Target: tayap@43.228.85.200:/home/tayap/ananya-php/public/"
echo "--------------------------------------------------------"
echo "Please enter your Server SSH Password when prompted:"

scp /Users/tayap/project-number/number-php/public/debug_tabian.php tayap@43.228.85.200:/home/tayap/ananya-php/public/

echo "--------------------------------------------------------"
echo "✅ Upload Complete! Try refreshing the App or Browser."
