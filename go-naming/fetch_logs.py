import paramiko

host = '43.228.85.200'
port = 22
username = 'root'
password = 'Lydh@58LTG'

try:
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(host, port, username, password)

    print("Fetching logs...")
    stdin, stdout, stderr = ssh.exec_command('journalctl -u go-naming -n 100 --no-pager')
    print("Logs:")
    print(stdout.read().decode())
    err = stderr.read().decode()
    if err:
        print("Stderr:", err)

    ssh.close()
except Exception as e:
    print(f"Error: {e}")
