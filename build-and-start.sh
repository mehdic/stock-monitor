#!/bin/bash

# Build and Start Script for StockMonitor
# Kills any processes on ports 3000 and 8080, builds both backend and frontend, then starts them

set -e  # Exit on error

echo "======================================"
echo "StockMonitor - Build and Start"
echo "======================================"

# Function to kill process on a specific port
kill_port() {
    local port=$1
    echo ""
    echo "Checking for processes on port $port..."

    # Find PID using lsof
    local pid=$(lsof -ti :$port)

    if [ -n "$pid" ]; then
        echo "Found process $pid on port $port. Killing..."
        kill -9 $pid 2>/dev/null || true
        sleep 1
        echo "Process on port $port killed."
    else
        echo "No process found on port $port."
    fi
}

# Kill existing processes
echo ""
echo "Step 1: Cleaning up existing processes"
echo "========================================"
kill_port 8080  # Backend
kill_port 3000  # Frontend

# Build Backend
echo ""
echo "Step 2: Building Backend"
echo "========================================"
cd backend
echo "Running Maven clean package (offline mode)..."
mvn -o clean package -DskipTests
echo "Backend build complete!"

# Build Frontend
echo ""
echo "Step 3: Building Frontend"
echo "========================================"
cd ../frontend
echo "Running npm build..."
npm run build
echo "Frontend build complete!"

# Start Backend
echo ""
echo "Step 4: Starting Backend"
echo "========================================"
cd ../backend
echo "Starting Spring Boot application on port 8080..."
mvn -o spring-boot:run > ../logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "Backend started with PID: $BACKEND_PID"

# Wait for backend to be ready
echo "Waiting for backend to be ready..."
for i in {1..30}; do
    if lsof -ti :8080 > /dev/null 2>&1; then
        echo "Backend is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "Warning: Backend may not have started properly. Check logs/backend.log"
    fi
    sleep 1
done

# Start Frontend
echo ""
echo "Step 5: Starting Frontend"
echo "========================================"
cd ../frontend
echo "Starting Vite dev server on port 3000..."
npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "Frontend started with PID: $FRONTEND_PID"

# Wait for frontend to be ready
echo "Waiting for frontend to be ready..."
for i in {1..10}; do
    if lsof -ti :3000 > /dev/null 2>&1; then
        echo "Frontend is ready!"
        break
    fi
    if [ $i -eq 10 ]; then
        echo "Warning: Frontend may not have started properly. Check logs/frontend.log"
    fi
    sleep 1
done

# Summary
echo ""
echo "======================================"
echo "StockMonitor Started Successfully!"
echo "======================================"
echo ""
echo "Backend:  http://localhost:8080"
echo "Frontend: http://localhost:3000"
echo "API Docs: http://localhost:8080/swagger-ui.html"
echo ""
echo "Backend PID:  $BACKEND_PID (logs: logs/backend.log)"
echo "Frontend PID: $FRONTEND_PID (logs: logs/frontend.log)"
echo ""
echo "To stop services:"
echo "  kill $BACKEND_PID $FRONTEND_PID"
echo ""
echo "Or use: ./stop.sh"
echo ""
