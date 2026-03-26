import re
import subprocess

def get_db_columns(table):
    try:
        cmd = ["mysql", "-u", "zoqlszwh_ananyadb", "-pIntelliP24.X", "-e", f"DESCRIBE `{table}`;", "zoqlszwh_ananyadb"]
        output = subprocess.check_output(cmd, stderr=subprocess.STDOUT).decode()
        columns = []
        for line in output.strip().split('\n'):
            parts = line.split('\t')
            if parts[0] != 'Field' and parts[0] != 'Field ':
                columns.append(parts[0].strip())
        return [c for c in columns if c and c != 'Field']
    except:
        return []

with open("zoqlszwh_ananyadb_260122.sql", "r") as f:
    sql_content = f.read()

create_tables = re.findall(r"CREATE TABLE `([^`]+)` \((.*?)\) ENGINE", sql_content, re.DOTALL)

print("Comparison Report:")
for table_name, columns_str in create_tables:
    sql_cols = re.findall(r"  `([^`]+)`", columns_str)
    db_cols = get_db_columns(table_name)
    
    if not db_cols:
        print(f"Table {table_name}: Missing in DB")
        continue
        
    extra_in_db = set(db_cols) - set(sql_cols)
    extra_in_sql = set(sql_cols) - set(db_cols)
    
    if extra_in_db or extra_in_sql:
        print(f"Table {table_name}:")
        if extra_in_db:
            print(f"  - Extra in DB (KEEP THESE): {extra_in_db}")
        if extra_in_sql:
            print(f"  - Extra in SQL (NEED TO ADD): {extra_in_sql}")
    else:
        # print(f"Table {table_name}: Match")
        pass
