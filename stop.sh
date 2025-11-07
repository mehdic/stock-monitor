#!/bin/bash

# Stop Script for StockMonitor
# Kills any processes running on ports 3000 and 8080

echo "======================================"
echo "StockMonitor - Stop"
echo "======================================"

# Function to kill process on a specific port
kill_port() {
    local port=$1
    local service=$2
    echo ""
    echo "Stopping $service on port $port..."

    # Find PID using lsof
    local pid=$(lsof -ti :$port)

    if [ -n "$pid" ]; then
        echo "Found process $pid. Killing..."
        kill -9 $pid 2>/dev/null || true
        sleep 1
        echo "$service stopped."
    else
        echo "No $service process found on port $port."
    fi
}

# Stop services
kill_port 8080 "Backend"
kill_port 3000 "Frontend"

echo ""
echo "======================================"
echo "All StockMonitor services stopped."
echo "======================================"
echo ""
