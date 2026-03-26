import os
import glob

def revert_colors(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Revert Colors.black.withOpacity back to Colors.white.withOpacity
    content = content.replace("Colors.black.withOpacity", "Colors.white.withOpacity")
    
    # Revert AppColors.bgDarker back to const Color(0xFF1E293B)
    content = content.replace("AppColors.bgDarker", "const Color(0xFF1E293B)")
    
    # Revert AppColors.bgDark back to const Color(0xFF0F172A)  
    # But be careful not to replace AppColors.bgDark in other contexts
    content = content.replace("AppColors.bgDark.withOpacity", "const Color(0xFF0F172A).withOpacity")
    # For standalone uses like "color: AppColors.bgDark,"
    # These are fine since AppColors.bgDark is now reverted in colors.dart
    
    with open(filepath, 'w') as f:
        f.write(content)

for filepath in glob.glob('lib/src/screens/*.dart'):
    revert_colors(filepath)
    print(f"Reverted {filepath}")
