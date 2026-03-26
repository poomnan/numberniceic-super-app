cd /Users/tayap/project-naming/go-naming
GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -o server main.go
rsync -avz --exclude '.git' -e "sshpass -p 'IntelliP24.X' ssh -o StrictHostKeyChecking=no" server tayap@43.228.85.200:/home/tayap/go-naming/server
sshpass -p 'IntelliP24.X' ssh -o StrictHostKeyChecking=no tayap@43.228.85.200 "echo 'IntelliP24.X' | sudo -S systemctl restart go-naming"
