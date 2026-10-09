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
./gradlew prepareGame :gameglue:jar

GAME_JAR="$ROOT_DIR/build/prepared/game.jar"
GAMEGLUE_JAR="$ROOT_DIR/gameglue/build/libs/gameglue.jar"
if [ ! -f "$GAME_JAR" ]; then
    echo "Error: $GAME_JAR was not generated."
    exit 1
fi
if [ ! -f "$GAMEGLUE_JAR" ]; then
    echo "Error: $GAMEGLUE_JAR was not generated."
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
if [ ! -f "jutils-1.0.0.jar" ]; then
    curl -sSLO https://repo1.maven.org/maven2/net/java/jutils/jutils/1.0.0/jutils-1.0.0.jar
fi
if [ ! -f "natives/liblwjgl64.so" ]; then
    curl -sSLO https://repo1.maven.org/maven2/org/lwjgl/lwjgl/lwjgl-platform/2.9.3/lwjgl-platform-2.9.3-natives-linux.jar
    (cd natives && jar xf ../lwjgl-platform-2.9.3-natives-linux.jar)
fi
if [ ! -f "natives/libjinput-linux64.so" ]; then
    curl -sSLO https://repo1.maven.org/maven2/net/java/jinput/jinput-platform/2.0.5/jinput-platform-2.0.5-natives-linux.jar
    (cd natives && jar xf ../jinput-platform-2.0.5-natives-linux.jar)
fi

echo "Step 3: Compiling desktop compatibility runtime (retro-rt.jar)..."
mkdir -p "$BUILD_DIR/compat-classes"
javac -cp "$GAME_JAR:$GAMEGLUE_JAR" -d "$BUILD_DIR/compat-classes" $(find "$ROOT_DIR/GraalVM-Versions/runtime-compat/src/main/java" -name "*.java")
jar cf "$BUILD_DIR/retro-rt.jar" -C "$BUILD_DIR/compat-classes" .

echo "Step 4: Compiling AOT native binary with GraalVM..."
cd "$BUILD_DIR"

CP="$GAME_JAR:$GAMEGLUE_JAR:$BUILD_DIR/retro-rt.jar:$LWJGL_DIR/lwjgl-2.9.3.jar:$LWJGL_DIR/lwjgl_util-2.9.3.jar:$LWJGL_DIR/jinput-2.0.5.jar:$LWJGL_DIR/jutils-1.0.0.jar"

native-image -cp "$CP" \
    -H:ConfigurationFileDirectories="$CONFIG_DIR" \
    -H:+UnlockExperimentalVMOptions \
    --enable-url-protocols=http,https \
    --report-unsupported-elements-at-runtime \
    --no-fallback \
    retro.desktop.DesktopMain mc-retro-native

echo "Step 5: Applying libjawt.so compatibility shim for LWJGL 2.9.3..."
if [ -f "$BUILD_DIR/libjawt.so" ]; then
    cp "$BUILD_DIR/libjawt.so" "$BUILD_DIR/libjawt_real.so"
fi
mkdir -p "$BUILD_DIR/jawt_shim"
cat << 'EOF' > "$BUILD_DIR/jawt_shim/shim.c"
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>

typedef int (*jawt_fn)(void*, void*);
static jawt_fn real_fn = 0;

int JAWT_GetAWT(void* env, void* awt) {
    if (!real_fn) {
        void* h = dlopen("libjawt_real.so", RTLD_LAZY | RTLD_GLOBAL);
        if (!h && getenv("JAVA_HOME")) {
            char buf[512];
            snprintf(buf, sizeof(buf), "%s/lib/libjawt.so", getenv("JAVA_HOME"));
            h = dlopen(buf, RTLD_LAZY | RTLD_GLOBAL);
        }
        if (!h) h = dlopen("/opt/graalvm/lib/libjawt.so", RTLD_LAZY | RTLD_GLOBAL);
        if (!h) h = dlopen("libjawt.so", RTLD_LAZY | RTLD_GLOBAL);
        if (h) real_fn = (jawt_fn)dlsym(h, "JAWT_GetAWT");
    }
    if (real_fn) return real_fn(env, awt);
    return 0;
}
EOF
cat << 'EOF' > "$BUILD_DIR/jawt_shim/jawt.map"
SUNWprivate_1.1 {
    global:
        JAWT_GetAWT;
    local:
        *;
};
EOF
gcc -shared -fPIC -O2 "$BUILD_DIR/jawt_shim/shim.c" -Wl,--version-script="$BUILD_DIR/jawt_shim/jawt.map" -ldl -o "$BUILD_DIR/libjawt.so"

echo "=== Build Complete! Executable produced at: $BUILD_DIR/mc-retro-native ==="
echo "To run:"
echo "  $ROOT_DIR/GraalVM-Versions/scripts/run-native.sh"

