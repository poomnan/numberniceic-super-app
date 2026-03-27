cd /Users/tayap/project-naming/go-naming
GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -o server main.go
rsync -avz --exclude '.git' -e "sshpass -p 'IntelliP24.X' ssh -o StrictHostKeyChecking=no" server tayap@numberniceic.online:/home/tayap/go-naming/server
sshpass -p 'IntelliP24.X' ssh -o StrictHostKeyChecking=no tayap@numberniceic.online "echo 'IntelliP24.X' | sudo -S systemctl restart go-naming"
