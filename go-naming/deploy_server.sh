cd /Users/tayap/project-naming/go-naming
export GOOS=linux
export GOARCH=amd64
go build -o server main.go
sshpass -p 'IntelliP24.X' ssh -o StrictHostKeyChecking=no tayap@43.228.85.200 "echo 'IntelliP24.X' | sudo -S systemctl stop go-naming"
rsync -avz --exclude '.git' -e "sshpass -p 'IntelliP24.X' ssh -o StrictHostKeyChecking=no" server tayap@43.228.85.200:/home/tayap/go-naming/server
sshpass -p 'IntelliP24.X' ssh -o StrictHostKeyChecking=no tayap@43.228.85.200 "echo 'IntelliP24.X' | sudo -S systemctl start go-naming"
