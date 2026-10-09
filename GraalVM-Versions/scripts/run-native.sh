#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/graalvm"
LWJGL_NATIVES="$BUILD_DIR/lwjgl/natives"
BIN="$BUILD_DIR/mc-retro-native"

if [ ! -f "$BIN" ]; then
    echo "Error: Native executable not found at $BIN. Run build-native.sh first."
    exit 1
fi

export LD_LIBRARY_PATH="${BUILD_DIR}:${LWJGL_NATIVES}:${LD_LIBRARY_PATH:-}"

echo "Starting Minecraft 1.2.5 Full Retro (GraalVM Native)..."
exec "$BIN" "$@"
