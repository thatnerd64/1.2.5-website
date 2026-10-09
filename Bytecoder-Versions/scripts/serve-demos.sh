#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEMOS_DIR="$SCRIPT_DIR/../demos"
PORT="${1:-8088}"

echo "=== Serving Bytecoder Demos on http://localhost:$PORT ==="
echo "  - Hello World (WasmGC): http://localhost:$PORT/01-hello-wasm/index.html"
echo "  - WebGL Canvas Demo:   http://localhost:$PORT/02-webgl-demo/index.html"
echo ""
python3 -m http.server "$PORT" --directory "$DEMOS_DIR"
