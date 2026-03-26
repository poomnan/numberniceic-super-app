#!/bin/bash
echo "🚀 Uploading Real-time PHP Bridge files to Server..."
echo "Target: tayap@43.228.85.200:/home/tayap/ananya-php/public/"
echo "--------------------------------------------------------"
echo "Please enter your Server SSH Password when prompted:"

scp /Users/tayap/project-number/number-php/public/debug_tabian.php /Users/tayap/project-number/number-php/public/debug_news.php tayap@43.228.85.200:/home/tayap/ananya-php/public/

echo "--------------------------------------------------------"
echo "✅ Upload Complete! Now build and install the Android app."
