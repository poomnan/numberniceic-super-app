import os
import glob

def fix_colors_hex_only(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Replace the dark colors used for backgrounds with the semantic AppColors variables we updated.
    content = content.replace("const Color(0xFF1E293B)", "AppColors.bgDarker")
    content = content.replace("Color(0xFF1E293B)", "AppColors.bgDarker")
    
    content = content.replace("const Color(0xFF0F172A)", "AppColors.bgDark")
    content = content.replace("Color(0xFF0F172A)", "AppColors.bgDark")
    
    content = content.replace("const Color(0xFFFDA4AF)", "const Color(0xFFEF4444)")
    content = content.replace("Color(0xFFFDA4AF)", "const Color(0xFFEF4444)")

    # Ensure anything replaced previously as `Colors.black` isn't making text invisible if the background was dark. 
    # But since the background is now light (bgDarker/bgDark is light), text should be textLight or black.
    # AppColors.textLight is dark slate now, so we are good.

    with open(filepath, 'w') as f:
        f.write(content)

for filepath in glob.glob('lib/src/screens/*.dart'):
    fix_colors_hex_only(filepath)
