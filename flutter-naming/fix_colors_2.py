import re

with open('lib/src/screens/naming_screen.dart', 'r') as f:
    content = f.read()

content = content.replace("AppColors.textLight38", "AppColors.textGray")
content = content.replace("AppColors.textLight54", "AppColors.textGray")
content = content.replace("AppColors.textLight70", "AppColors.textGray")
content = content.replace("AppColors.textLight10", "Colors.black.withOpacity(0.1)")

with open('lib/src/screens/naming_screen.dart', 'w') as f:
    f.write(content)
