#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CLI_JAR="$ROOT_DIR/tools/bytecoder-cli.jar"

if [ $# -lt 3 ]; then
    echo "Usage: $0 <main-class> <output-dir> <classpath> [optimization-level: DISABLED|DEFAULT|ALL]"
    echo "Example: $0 retro.bytecoder.HelloWasm dist/ bin/,lib/some.jar DISABLED"
    exit 1
fi

MAIN_CLASS="$1"
OUT_DIR="$2"
CP="$3"
OPT="${4:-DISABLED}"

mkdir -p "$OUT_DIR"

echo "=== Compiling $MAIN_CLASS to WebAssembly (WasmGC) via Bytecoder ==="
java -Xmx32g -jar "$CLI_JAR" compile wasm \
    -classpath="$CP" \
    -mainclass="$MAIN_CLASS" \
    -builddirectory="$OUT_DIR" \
    -filenameprefix="bytecoder_" \
    -optimizationlevel="$OPT"

echo "Wasm build output generated in $OUT_DIR"
