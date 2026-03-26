import paramiko

host = '43.228.85.200'
port = 22
username = 'root'
password = 'Lydh@58LTG'

local_path = '/Users/tayap/project-naming/go-naming/go_naming_server'
remote_path = '/home/tayap/go-naming/server'

try:
    print("Connecting to server...")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(host, port, username, password)

    print("Uploading new binary...")
    sftp = ssh.open_sftp()
    remote_tmp = remote_path + ".tmp"
    sftp.put(local_path, remote_tmp)
    sftp.chmod(remote_tmp, 0o755)
    sftp.close()

    print("Stopping service and replacing binary...")
    ssh.exec_command('systemctl stop go-naming')
    ssh.exec_command(f'mv {remote_tmp} {remote_path}')
    
    print("Restarting service...")
    stdin, stdout, stderr = ssh.exec_command('systemctl start go-naming')
    print("Stdout:", stdout.read().decode())
    print("Stderr:", stderr.read().decode())

    ssh.close()
    print("Deployment completed successfully.")
except Exception as e:
    print(f"Error: {e}")
