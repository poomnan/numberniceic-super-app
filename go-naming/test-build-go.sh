cd /Users/tayap/project-naming/go-naming
mkdir -p /tmp/go-bin /tmp/go-naming-cache /tmp/go-naming-tmp
export GOCACHE=/tmp/go-naming-cache
export GOTMPDIR=/tmp/go-naming-tmp
GOOS=linux GOARCH=amd64 go build -o /tmp/go-bin/go-naming-app main.go
sshpass -p 'IntelliP24.X' rsync -avz -e "ssh -o StrictHostKeyChecking=no" /tmp/go-bin/go-naming-app tayap@43.228.85.200:/home/tayap/go-naming/server
sshpass -p 'IntelliP24.X' ssh -T -o StrictHostKeyChecking=no tayap@43.228.85.200 "echo 'IntelliP24.X' | sudo -S systemctl restart go-naming"
sshpass -p 'IntelliP24.X' ssh -T -o StrictHostKeyChecking=no tayap@43.228.85.200 "ps aux | grep -E '/server' | grep -v grep"
