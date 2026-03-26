import subprocess
import os

DB_USER = "zoqlszwh_ananyadb"
DB_PASS = "IntelliP24.X"
DB_NAME = "zoqlszwh_ananyadb"
SQL_FILE = "zoqlszwh_ananyadb_260122.sql"

def run_query(query):
    # Using -N to remove headers
    cmd = ["mysql", "-u", DB_USER, f"-p{DB_PASS}", "-N", "-e", query, DB_NAME]
    return subprocess.check_output(cmd, stderr=subprocess.STDOUT).decode()

def main():
    # 1. Backup fcm_token
    print("Backing up fcm_token...")
    backup = {}
    try:
        # We also backup realname/surname just in case we need to verify mapping
        output = run_query("SELECT memberid, fcm_token FROM membertb WHERE fcm_token IS NOT NULL AND fcm_token != '';")
        for line in output.strip().split('\n'):
            line = line.strip()
            if not line or "Warning" in line: continue
            parts = line.split('\t')
            if len(parts) == 2:
                backup[parts[0]] = parts[1]
        print(f"Backed up {len(backup)} tokens.")
    except Exception as e:
        print(f"Warning: Could not backup tokens: {e}")

    # 2. Modify SQL to include DROP TABLE
    print("Modifying SQL file to include DROP TABLE IF EXISTS...")
    if not os.path.exists(SQL_FILE):
        print(f"Error: {SQL_FILE} not found!")
        return

    with open(SQL_FILE, "r") as f:
        content = f.read()

    # Find all table names that have CREATE TABLE
    table_names = re.findall(r"CREATE TABLE `([^`]+)`", content)
    
    # Prepend DROP TABLE for each table
    modified_content = content
    for table in set(table_names):
        modified_content = f"DROP TABLE IF EXISTS `{table}`;\n" + modified_content

    MOD_SQL = "zoqlszwh_ananyadb_mod.sql"
    with open(MOD_SQL, "w") as f:
        f.write(modified_content)

    # 3. Import SQL
    print("Importing modified SQL to localhost...")
    try:
        # Use shell for redirection
        cmd = f"mysql -u {DB_USER} -p'{DB_PASS}' {DB_NAME} < {MOD_SQL}"
        subprocess.check_call(cmd, shell=True)
        print("✅ Import Successful.")
    except subprocess.CalledProcessError as e:
        print(f"❌ Import Failed: {e}")
        return

    # 4. Restore Structure & Data
    print("Restoring fcm_token column and data...")
    try:
        run_query("ALTER TABLE membertb ADD COLUMN fcm_token VARCHAR(255) DEFAULT NULL AFTER ageday;")
        print("Added fcm_token column.")
    except Exception as e:
        print(f"Info: {e}")

    count = 0
    for mid, token in backup.items():
        try:
            run_query(f"UPDATE membertb SET fcm_token = '{token}' WHERE memberid = {mid};")
            count += 1
        except:
            pass
    print(f"Restored {count} tokens.")

    # 5. Backup current DB to verified dump (optional but recommended)
    print("Cleanup...")
    if os.path.exists(MOD_SQL):
        os.remove(MOD_SQL)

    print("🎉 Smart Update for LOCALHOST Completed!")

import re
if __name__ == "__main__":
    main()
