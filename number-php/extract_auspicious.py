
import re

input_file = 'restore_news.sql'
output_file = 'fix_auspicious.sql'

with open(input_file, 'r', encoding='utf-8', errors='ignore') as f:
    content = f.read()

# Find the insert block
# It usually starts with "INSERT INTO `auspicious_days` ... VALUES" and ends with ";"
match = re.search(r"INSERT INTO `auspicious_days` .*? VALUES\s*(.*?);", content, re.DOTALL)
if match:
    values_str = match.group(1)
    # Split by ), ( to separate rows crudely or use regex
    # The format is (1, '2023-01-01', 1, 0, 0, NULL, NULL), ...
    
    # Matches: (id, 'date', ...)
    # We are interested in the date.
    
    # regex to match each tuple
    tuples = re.findall(r"\(([^)]+)\)", values_str)
    
    inserts = []
    for t in tuples:
        # Check date
        # t is roughly: 994, '2026-05-01', 0, 1, 0, NULL, NULL
        parts = t.split(',')
        if len(parts) >= 2:
            date_part = parts[1].strip().strip("'")
            if date_part.startswith('2026-') or date_part.startswith('2027-'):
                # Check month
                m = int(date_part.split('-')[1])
                y = int(date_part.split('-')[0])
                
                # We want 2026-05 onwards, and all 2027 if any
                if (y == 2026 and m >= 4) or y > 2026:
                     # Reconstruct valid SQL
                     # We can use INSERT IGNORE to be safe
                     safe_val = t.replace("\n", "").strip()
                     inserts.append(f"({safe_val})")

    if inserts:
        with open(output_file, 'w') as out:
            out.write("INSERT IGNORE INTO `auspicious_days` (`id`, `date`, `is_wanpra`, `is_tongchai`, `is_atipbadee`, `description`, `created_at`) VALUES \n")
            out.write(",\n".join(inserts))
            out.write(";\n")
        print(f"Extracted {len(inserts)} rows to {output_file}")
    else:
        print("No matching rows found")
else:
    print("Could not find INSERT statement")
