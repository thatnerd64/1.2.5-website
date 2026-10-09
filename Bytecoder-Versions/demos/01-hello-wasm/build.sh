#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
CLI_JAR="$ROOT_DIR/tools/bytecoder-cli.jar"

if [ ! -f "$CLI_JAR" ]; then
    echo "Error: bytecoder-cli.jar not found at $CLI_JAR"
    exit 1
fi

echo "=== Building 01-hello-wasm with Bytecoder ==="
rm -rf "$SCRIPT_DIR/bin" "$SCRIPT_DIR/dist"
mkdir -p "$SCRIPT_DIR/bin" "$SCRIPT_DIR/dist"

echo "Compiling Java sources..."
javac -d "$SCRIPT_DIR/bin" "$SCRIPT_DIR/src/retro/bytecoder/HelloWasm.java"

echo "Compiling to WebAssembly (WasmGC)..."
java -jar "$CLI_JAR" compile wasm \
    -classpath="$SCRIPT_DIR/bin" \
    -mainclass=retro.bytecoder.HelloWasm \
    -builddirectory="$SCRIPT_DIR/dist" \
    -filenameprefix=hello_ \
    -optimizationlevel=DISABLED

echo "Build complete. Artifacts in $SCRIPT_DIR/dist:"
ls -lh "$SCRIPT_DIR/dist"
