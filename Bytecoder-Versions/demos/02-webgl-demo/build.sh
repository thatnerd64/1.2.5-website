#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
CLI_JAR="$ROOT_DIR/tools/bytecoder-cli.jar"
API_JAR="$ROOT_DIR/lib/bytecoder.api-2024-05-10.jar"
WEB_JAR="$ROOT_DIR/lib/bytecoder.web-2024-05-10.jar"

if [ ! -f "$CLI_JAR" ]; then
    echo "Error: bytecoder-cli.jar not found at $CLI_JAR"
    exit 1
fi

echo "=== Building 02-webgl-demo with Bytecoder ==="
rm -rf "$SCRIPT_DIR/bin" "$SCRIPT_DIR/dist"
mkdir -p "$SCRIPT_DIR/bin" "$SCRIPT_DIR/dist"

echo "Compiling Java sources against Bytecoder Web API..."
javac -cp "$API_JAR:$WEB_JAR" -d "$SCRIPT_DIR/bin" "$SCRIPT_DIR/src/retro/bytecoder/WebGLDemo.java"

echo "Compiling to WebAssembly (WasmGC)..."
java -jar "$CLI_JAR" compile wasm \
    -classpath="$SCRIPT_DIR/bin,$API_JAR,$WEB_JAR" \
    -mainclass=retro.bytecoder.WebGLDemo \
    -builddirectory="$SCRIPT_DIR/dist" \
    -filenameprefix=webgl_ \
    -optimizationlevel=DISABLED

echo "Build complete. Artifacts in $SCRIPT_DIR/dist:"
ls -lh "$SCRIPT_DIR/dist"
