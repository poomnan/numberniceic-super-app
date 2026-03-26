
input_file = 'articles_data_raw.sql'
output_file = 'import_articles_data.sql'

with open(input_file, 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    # Remove 'public.' schema
    new_line = line.replace('INSERT INTO public.articles', 'INSERT INTO articles')
    
    # Replace boolean 'true' with '1', 'false' with '0'
    # Be careful not to replace 'true' inside text content, but usually SQL dump boolean values are notably outside quotes if standard dump, 
    # but Postgres dump often uses 'true' string for boolean? No, plain true.
    # Let's simple check: values are typically at the end or specific positions.
    # The dump format: VALUES (id, slug, title, ..., is_published, ...)
    # is_published is 8th column.
    
    # Simple replace for SQL standard booleans which are unquoted keywords
    new_line = new_line.replace(', true,', ', 1,').replace(', false,', ', 0,')
    
    # Postgres timestamp with timezone '2012-07-07 00:00:00+07'
    # MySQL DATETIME supports '2012-07-07 00:00:00'
    # Simple regex or replace to strip timezone if needed, but MySQL 8 might handle it. 
    # Let's try to remove '+07' at the tail of timestamps if it causes issues.
    new_line = new_line.replace('+07\'', '\'') 

    new_lines.append(new_line)

with open(output_file, 'w') as f:
    f.writelines(new_lines)

print(f"Converted {len(lines)} lines.")
