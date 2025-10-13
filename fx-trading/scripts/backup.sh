#!/bin/bash
# FX Trading Database Backup Script

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_PATH="$APP_DIR/data/trades.db"
BACKUP_DIR="$APP_DIR/backups"
DATE=$(date +%Y%m%d-%H%M%S)
BACKUP_FILE="$BACKUP_DIR/trades-$DATE.db"

# Create backup directory if doesn't exist
mkdir -p "$BACKUP_DIR"

# Check if database exists
if [ ! -f "$DB_PATH" ]; then
    echo "Error: Database file not found at $DB_PATH"
    exit 1
fi

# Check if sqlite3 is available
if ! command -v sqlite3 &> /dev/null; then
    echo "Error: sqlite3 command not found. Installing..."
    echo "On macOS: brew install sqlite"
    echo "On Ubuntu: sudo apt-get install sqlite3"
    exit 1
fi

echo "Starting backup of $DB_PATH..."

# Use SQLite's backup command for safe online backup
sqlite3 "$DB_PATH" ".backup '$BACKUP_FILE'"

if [ $? -eq 0 ]; then
    echo "Backup successful: $BACKUP_FILE"

    # Compress backup
    gzip "$BACKUP_FILE"
    echo "Backup compressed: $BACKUP_FILE.gz"

    # Keep only last 30 days of backups
    find "$BACKUP_DIR" -name "trades-*.db.gz" -mtime +30 -delete
    echo "Old backups cleaned (kept last 30 days)"

    # Show backup size
    BACKUP_SIZE=$(du -h "$BACKUP_FILE.gz" | cut -f1)
    echo "Backup size: $BACKUP_SIZE"
else
    echo "Backup failed!"
    exit 1
fi
