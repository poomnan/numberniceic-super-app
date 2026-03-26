import re

with open('lib/src/screens/naming_screen.dart', 'r') as f:
    content = f.read()

# Replace Colors.white with AppColors.textLight for text/icons, but keeping borders might be tricky.
# We will just replace Colors.white with AppColors.textLight and we'll fix exceptions.
# We know bg is white, so text and icons need to be dark. AppColors.textLight is actually dark now.
content = content.replace("Colors.white", "AppColors.textLight")
content = content.replace("Colors.white70", "AppColors.textGray")
content = content.replace("Colors.white54", "AppColors.textGray")
content = content.replace("Colors.white38", "AppColors.textGray")

# Inverted theme: black/transparent borders instead of white
content = content.replace("AppColors.textLight.withOpacity(", "Colors.black.withOpacity(")
content = content.replace("AppColors.textLight.withOpacity", "Colors.black.withOpacity")

with open('lib/src/screens/naming_screen.dart', 'w') as f:
    f.write(content)
