#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/graalvm-wasm"

echo "=== Attempting GraalVM Web Image (Wasm) Compilation ==="
echo "Note: Oracle GraalVM 25+ and Binaryen 119+ (wasm-as) are required."

if ! command -v wasm-as &>/dev/null; then
    echo "Error: 'wasm-as' (Binaryen) not found in PATH."
    exit 1
fi

cd "$ROOT_DIR"
./gradlew prepareGame :gameglue:jar :runtime:jar

mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

CP="$ROOT_DIR/runtime/build/libs/runtime.jar:$ROOT_DIR/gameglue/build/libs/gameglue.jar:$ROOT_DIR/build/prepared/game.jar"

native-image --tool:svm-wasm \
    -cp "$CP" \
    retro.glue.Main mc-wasm

echo "Wasm build output generated at $BUILD_DIR:"
ls -lh "$BUILD_DIR"
