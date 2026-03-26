import sys

def find_class_end(file_path, class_name):
    with open(file_path, 'r') as f:
        lines = f.readlines()
        
    start_line = -1
    for i, line in enumerate(lines):
        if class_name in line and 'class ' in line:
            start_line = i
            break
            
    if start_line == -1:
        print("Class not found")
        return
        
    open_braces = 0
    started = False
    
    for i in range(start_line, len(lines)):
        line = lines[i]
        for char in line:
            if char == '{':
                open_braces += 1
                started = True
            elif char == '}':
                open_braces -= 1
                if started and open_braces == 0:
                    print(f"Class ends at line {i + 1}")
                    return

find_class_end('/Users/tayap/project-naming/flutter-naming/lib/src/screens/naming_screen.dart', '_NamingScreenState'import sys