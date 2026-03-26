
input_file = '../ananya-go/articles_dump.sql'
output_file = 'import_articles_data.sql'

import re

statements = []
current_statement = []
in_statement = False

with open(input_file, 'r') as f:
    for line in f:
        # Check start of INSERT
        if line.strip().startswith("INSERT INTO public.articles"):
            in_statement = True
            current_statement = [line]
        elif in_statement:
            current_statement.append(line)
        
        # Check end of statement ');' or ');\n' 
        # (Be careful about '); inside strings, but for SQL dump usually it's at the end of line)
        if in_statement and line.strip().endswith(");"):
            # End of statement
            full_stmt = "".join(current_statement)
            
            # Transformation
            # 1. Remove Schema
            full_stmt = full_stmt.replace("INSERT INTO public.articles", "INSERT INTO articles")
            
            # 2. Boolean true/false -> 1/0
            # Use strict replace to avoid replacing text in content
            # Pattern: , true, -> , 1,
            full_stmt = full_stmt.replace(", true,", ", 1,")
            full_stmt = full_stmt.replace(", false,", ", 0,")
            # If at end of values ... , true); -> ... , 1);
            full_stmt = full_stmt.replace(", true);", ", 1);")
            full_stmt = full_stmt.replace(", false);", ", 0);")
            
            # 3. Timestamp +Timezone
            # Postgres: '2012-07-07 00:00:00+07'
            # MySQL: '2012-07-07 00:00:00'
            # Regex to remove +XX at end of timestamp string
            # It matches 'YYYY-MM-DD HH:MM:SS(.fraction)?+XX'
            full_stmt = re.sub(r"(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})(\.\d+)?(\+\d+)'", r"\1\2'", full_stmt)
            # Handle decimals in timestamp if any '2025...15:18:21.01' MySQL supports it usually
            
            statements.append(full_stmt)
            in_statement = False
            current_statement = []

with open(output_file, 'w') as f:
    for stmt in statements:
        f.write(stmt) # newline is already in line

print(f"Extracted and converted {len(statements)} statements.")
