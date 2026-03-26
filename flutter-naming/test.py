import sys

def check_file():
    with open('lib/src/screens/naming_screen.dart', 'r') as f:
        lines = f.readlines()
        
    stack = []
    
    for i, line in enumerate(lines):
        for char in line:
            if char == '{':
                stack.append(i)
            elif char == '}':
                if stack:
                    start = stack.pop()
                    if start == 30: # _NamingScreenState starts at line 31 (index 30)
                        print(f"Class _NamingScreenState ends at line {i + 1}")
                        return
                else:
                    print(f"Unmatched closing brace at line {i + 1}")

check_file()
