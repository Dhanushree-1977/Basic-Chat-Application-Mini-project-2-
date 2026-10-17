#!/usr/bin/env bash
# ========================================================
# Real-Time Chat TLS Server Launcher (Linux / macOS)
# ========================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "==================================================="
echo "  Starting TLS Encrypted Real-Time Chat Server... "
echo "==================================================="

# Check for fat JAR first, then target/classes fallback
if [ -f "target/realtime-chat-app-1.0-SNAPSHOT.jar" ]; then
    java -jar target/realtime-chat-app-1.0-SNAPSHOT.jar server
else
    java -cp "target/classes:lib/*" com.realtimechat.Main server
fi
