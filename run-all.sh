#!/usr/bin/env bash
# ========================================================
# Real-Time Chat System All-in-One Launcher (Linux/macOS)
# ========================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================================"
echo "  Launching Real-Time Chat Server and Both Clients      "
echo "========================================================"

echo "[1/3] Starting TLS Server on Port 5000 in background..."
if [ -f "target/realtime-chat-app-1.0-SNAPSHOT.jar" ]; then
    java -jar target/realtime-chat-app-1.0-SNAPSHOT.jar server &
else
    java -cp "target/classes:lib/*" com.realtimechat.Main server &
fi
SERVER_PID=$!

echo "Waiting for Server to bind..."
sleep 2

echo "[2/3] Launching Client 1 (Alice)..."
if [ -f "target/realtime-chat-app-1.0-SNAPSHOT.jar" ]; then
    java -jar target/realtime-chat-app-1.0-SNAPSHOT.jar client &
else
    java -cp "target/classes:lib/*" com.realtimechat.Main client &
fi

sleep 1

echo "[3/3] Launching Client 2 (Bob)..."
if [ -f "target/realtime-chat-app-1.0-SNAPSHOT.jar" ]; then
    java -jar target/realtime-chat-app-1.0-SNAPSHOT.jar client &
else
    java -cp "target/classes:lib/*" com.realtimechat.Main client &
fi

echo "========================================================"
echo "  Server and Clients running! Press Ctrl+C to terminate."
echo "========================================================"

trap "kill $SERVER_PID 2>/dev/null || true" EXIT
wait $SERVER_PID
