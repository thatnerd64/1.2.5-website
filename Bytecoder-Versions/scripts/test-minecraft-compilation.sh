#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
CLI_JAR="$ROOT_DIR/Bytecoder-Versions/tools/bytecoder-cli.jar"
GAME_JAR="$ROOT_DIR/build/prepared/game.jar"
GAMEGLUE_JAR="$ROOT_DIR/gameglue/build/libs/gameglue.jar"
RUNTIME_JAR="$ROOT_DIR/runtime/build/libs/runtime.jar"
OUT_DIR="$ROOT_DIR/build/bytecoder-test"

mkdir -p "$OUT_DIR"

echo "=== Bytecoder Minecraft 1.2.5 Compilation Diagnostic Test ==="
echo "Testing compilation of game classpath against Bytecoder Wasm backend..."

if [ ! -f "$GAME_JAR" ]; then
    echo "Warning: $GAME_JAR not found. Running ./gradlew prepareGame..."
    (cd "$ROOT_DIR" && ./gradlew prepareGame :gameglue:jar :runtime:jar)
fi

echo "Attempting compilation of net.minecraft.client.Minecraft..."
set -o pipefail
set +e
java -Xmx32g -jar "$CLI_JAR" compile wasm \
    -classpath="$GAME_JAR" \
    -mainclass=net.minecraft.client.Minecraft \
    -builddirectory="$OUT_DIR" \
    -filenameprefix="mc_" \
    -optimizationlevel=DISABLED 2>&1 | tee "$OUT_DIR/mc-compile.log"
EXIT_CODE="${PIPESTATUS[0]}"
set -e

echo ""
echo "=== Diagnostic Result ==="
WASM_OUTPUT="$OUT_DIR/mc_wasmclasses.wasm"
if [ -f "$WASM_OUTPUT" ] && ! grep -q "AnalysisException" "$OUT_DIR/mc-compile.log"; then
    echo "SUCCESS: Wasm module generated at $WASM_OUTPUT"
else
    echo "FAILURE: Bytecoder could not compile Minecraft."
    echo "Key exceptions found in log:"
    grep -E "AnalysisException|IllegalStateException|ClassNotFoundException|NoSuchMethodError|No such method|Unsupported class file" "$OUT_DIR/mc-compile.log" | head -n 10 || true
fi
