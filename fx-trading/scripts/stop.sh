#!/bin/bash
# FX Trading Application Shutdown Script

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$APP_DIR/fx-trading.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "PID file not found. Application may not be running."
    exit 1
fi

PID=$(cat "$PID_FILE")

if ! ps -p "$PID" > /dev/null 2>&1; then
    echo "Application not running (stale PID file)"
    rm "$PID_FILE"
    exit 1
fi

echo "Stopping FX Trading Application (PID: $PID)..."
kill -TERM "$PID"

# Wait for graceful shutdown (max 30 seconds)
for i in {1..30}; do
    if ! ps -p "$PID" > /dev/null 2>&1; then
        echo "Application stopped successfully"
        rm "$PID_FILE"
        exit 0
    fi
    sleep 1
done

# Force kill if still running
echo "Forcing application shutdown..."
kill -9 "$PID"
rm "$PID_FILE"
echo "Application stopped (forced)"
