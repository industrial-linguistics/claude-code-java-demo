#!/bin/bash
# FX Trading Application Startup Script

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_JAR="$APP_DIR/target/fx-trading-1.0.0.jar"
PID_FILE="$APP_DIR/fx-trading.pid"
LOG_FILE="$APP_DIR/logs/fx-trading.log"

# Create logs directory if it doesn't exist
mkdir -p "$APP_DIR/logs"

# Create data directory if it doesn't exist
mkdir -p "$APP_DIR/data"

# Check if already running
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "Application already running (PID: $PID)"
        exit 1
    fi
fi

# Check if JAR exists
if [ ! -f "$APP_JAR" ]; then
    echo "Error: JAR file not found at $APP_JAR"
    echo "Please run 'mvn clean package' first"
    exit 1
fi

# Start application
echo "Starting FX Trading Application..."
nohup java -jar "$APP_JAR" \
    --spring.config.location="$APP_DIR/src/main/resources/application.yml" \
    --FX_DB_PATH="$APP_DIR/data/trades.db" \
    >> "$LOG_FILE" 2>&1 &

echo $! > "$PID_FILE"
echo "Application started (PID: $(cat $PID_FILE))"
echo "Logs: $LOG_FILE"
echo "Health check: http://localhost:8080/actuator/health"
