#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PORT="${1:-8180}"

echo "=== Launching OpenJDK Zero WebAssembly JVM Environment ==="

# Check runtime assets
if [ ! -f "$ROOT_DIR/web/javabox-direct.wasm" ] || [ ! -f "$ROOT_DIR/web/javabox-direct.data" ]; then
    echo "Runtime assets missing. Downloading..."
    "$ROOT_DIR/tools/download-runtime.sh"
fi

python3 "$SCRIPT_DIR/serve-wasm-jvm.py" "$PORT"
