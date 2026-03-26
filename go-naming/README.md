# Go Naming Backend

This project connects to a remote PostgreSQL database.

## Prerequisites
- Go installed
- Network access to the database server (43.228.85.200)

## Setup
1. Initialize dependencies:
   ```bash
   go mod tidy
   ```

## Running
Run the main application to verify the database connection and list existing tables:
```bash
go run main.go
```

## Configuration
The database credentials are currently hardcoded in `main.go` for testing purposes.
