#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/graalvm"
CONFIG_DIR="$ROOT_DIR/GraalVM-Versions/config"
LWJGL_DIR="$ROOT_DIR/build/graalvm/lwjgl"

echo "=== Building Minecraft 1.2.5 Full Retro with GraalVM Native Image ==="

if ! command -v native-image &>/dev/null; then
    echo "Error: 'native-image' not found in PATH. Please install Oracle GraalVM 21+ and set JAVA_HOME/PATH."
    exit 1
fi

echo "Step 1: Preparing game bytecode via gradle..."
cd "$ROOT_DIR"
./gradlew prepareGame

GAME_JAR="$ROOT_DIR/build/prepared/game.jar"
if [ ! -f "$GAME_JAR" ]; then
    echo "Error: $GAME_JAR was not generated."
    exit 1
fi

echo "Step 2: Ensuring LWJGL 2.9.3 libraries and natives are present..."
mkdir -p "$LWJGL_DIR/natives"
cd "$LWJGL_DIR"

if [ ! -f "lwjgl-2.9.3.jar" ]; then
    curl -sSLO https://repo1.maven.org/maven2/org/lwjgl/lwjgl/lwjgl/2.9.3/lwjgl-2.9.3.jar
fi
if [ ! -f "lwjgl_util-2.9.3.jar" ]; then
    curl -sSLO https://repo1.maven.org/maven2/org/lwjgl/lwjgl/lwjgl_util/2.9.3/lwjgl_util-2.9.3.jar
fi
if [ ! -f "jinput-2.0.5.jar" ]; then
    curl -sSLO https://repo1.maven.org/maven2/net/java/jinput/jinput/2.0.5/jinput-2.0.5.jar
fi
if [ ! -f "natives/liblwjgl64.so" ]; then
    curl -sSLO https://repo1.maven.org/maven2/org/lwjgl/lwjgl/lwjgl-platform/2.9.3/lwjgl-platform-2.9.3-natives-linux.jar
    (cd natives && jar xf ../lwjgl-platform-2.9.3-natives-linux.jar)
fi

echo "Step 3: Compiling AOT native binary with GraalVM..."
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

CP="$GAME_JAR:$LWJGL_DIR/lwjgl-2.9.3.jar:$LWJGL_DIR/lwjgl_util-2.9.3.jar:$LWJGL_DIR/jinput-2.0.5.jar"

native-image -cp "$CP" \
    -H:ConfigurationFileDirectories="$CONFIG_DIR" \
    -H:+UnlockExperimentalVMOptions \
    --no-fallback \
    net.minecraft.client.Minecraft mc-retro-native

echo "=== Build Complete! Executable produced at: $BUILD_DIR/mc-retro-native ==="
echo "To run:"
echo "  LD_LIBRARY_PATH=\"$BUILD_DIR:$LWJGL_DIR/natives\" $BUILD_DIR/mc-retro-native"
