#!/bin/bash
# FX Trading Application Status Script

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$APP_DIR/fx-trading.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "Status: Not running (no PID file)"
    exit 1
fi

PID=$(cat "$PID_FILE")

if ps -p "$PID" > /dev/null 2>&1; then
    echo "Status: Running (PID: $PID)"

    # Check health endpoint
    if command -v curl &> /dev/null; then
        echo ""
        echo "Health check:"
        curl -s http://localhost:8080/actuator/health | python -m json.tool 2>/dev/null || echo "Health endpoint not responding"
    fi

    exit 0
else
    echo "Status: Not running (stale PID file)"
    rm "$PID_FILE"
    exit 1
fi
