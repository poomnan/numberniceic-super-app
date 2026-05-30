cd /Users/tayap/project-naming/go-naming
go build -o go-naming-app main.go
rsync -avz --exclude '.git' -e "sshpass -p 'IntelliP24.X' ssh -o StrictHostKeyChecking=no" go-naming-app tayap@43.228.85.200:/home/tayap/go-naming/go-naming-app
sshpass -p 'IntelliP24.X' ssh -o StrictHostKeyChecking=no tayap@43.228.85.200 "echo 'IntelliP24.X' | sudo -S systemctl restart go-naming"
