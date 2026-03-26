
import re

input_file = 'restore_news.sql'
output_file = 'fix_auspicious_no_ids.sql'

with open(input_file, 'r', encoding='utf-8', errors='ignore') as f:
    content = f.read()

match = re.search(r"INSERT INTO `auspicious_days` .*? VALUES\s*(.*?);", content, re.DOTALL)
if match:
    values_str = match.group(1)
    # Regex to grab content inside parens, expecting: (id, 'date', ...)
    tuples = re.findall(r"\(([^)]+)\)", values_str)
    
    inserts = []
    for t in tuples:
        parts = t.split(',')
        if len(parts) >= 2:
            date_part = parts[1].strip().strip("'")
            if date_part.startswith('2026-') or date_part.startswith('2027-'):
                m = int(date_part.split('-')[1])
                y = int(date_part.split('-')[0])
                
                # Filter for April 2026 onwards
                if (y == 2026 and m >= 4) or y > 2026:
                     # Remove the first element (ID)
                     # t is "91, '2026-04-01', ..."
                     # We want "'2026-04-01', ..."
                     # Find first comma
                     first_comma = t.find(',')
                     if first_comma != -1:
                         val_no_id = t[first_comma+1:].strip()
                         inserts.append(f"({val_no_id})")

    if inserts:
        with open(output_file, 'w') as out:
            # Note: Removed `id` from column list
            out.write("INSERT IGNORE INTO `auspicious_days` (`date`, `is_wanpra`, `is_tongchai`, `is_atipbadee`, `description`, `created_at`) VALUES \n")
            out.write(",\n".join(inserts))
            out.write(";\n")
        print(f"Extracted {len(inserts)} rows (without IDs) to {output_file}")
    else:
        print("No matching rows found")
else:
    print("Could not find INSERT statement")
