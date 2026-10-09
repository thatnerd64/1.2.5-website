#!/usr/bin/env bash
# Packages the standalone GraalVM native binary and all runtime dependencies into a redistributable archive.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/graalvm"
DIST_DIR="$BUILD_DIR/mc-retro-graalvm-linux-x64"
OUT_TAR="$BUILD_DIR/mc-retro-graalvm-linux-x64.tar.gz"

echo "=== Packaging Standalone GraalVM Distribution ==="

if [ ! -f "$BUILD_DIR/mc-retro-native" ]; then
    echo "Error: Native binary $BUILD_DIR/mc-retro-native not found. Run build-native.sh first."
    exit 1
fi

rm -rf "$DIST_DIR" "$OUT_TAR"
mkdir -p "$DIST_DIR/lib"

# 1. Native Executable
echo "Copying native binary..."
cp -p "$BUILD_DIR/mc-retro-native" "$DIST_DIR/"

# 2. Shared Libraries (LWJGL, OpenAL, AWT/Java shims)
echo "Bundling native libraries..."
cp -p "$BUILD_DIR"/lib*.so "$DIST_DIR/lib/"
if [ -d "$BUILD_DIR/lwjgl/natives" ]; then
    cp -p "$BUILD_DIR/lwjgl/natives"/lib*64.so "$DIST_DIR/lib/"
fi
cd "$DIST_DIR/lib"
ln -sf libopenal64.so libopenal.so
ln -sf libopenal64.so libopenal.so.1
cd "$ROOT_DIR"

# 3. Assets & Textures (dereference symlinks)
echo "Bundling assets..."
if [ -e "$ROOT_DIR/assets.pak" ]; then
    cp -L "$ROOT_DIR/assets.pak" "$DIST_DIR/assets.pak"
elif [ -f "$ROOT_DIR/build/prepared/web/assets.pak" ]; then
    cp "$ROOT_DIR/build/prepared/web/assets.pak" "$DIST_DIR/assets.pak"
fi

if [ -e "$ROOT_DIR/assets.pak.d" ]; then
    cp -rL "$ROOT_DIR/assets.pak.d" "$DIST_DIR/assets.pak.d"
elif [ -d "$ROOT_DIR/build/prepared/web/assets.pak.d" ]; then
    cp -r "$ROOT_DIR/build/prepared/web/assets.pak.d" "$DIST_DIR/assets.pak.d"
fi

# 4. Modpack Files (.minecraft with configs & mods)
echo "Bundling modpack..."
if [ -d "$ROOT_DIR/.minecraft" ]; then
    cp -rL "$ROOT_DIR/.minecraft" "$DIST_DIR/.minecraft"
fi

# 5. Runner script
cat << 'RUNNER' > "$DIST_DIR/run.sh"
#!/usr/bin/env bash
# Launch Minecraft 1.2.5 Full Retro (GraalVM Standalone Native)
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export LD_LIBRARY_PATH="$HERE/lib:${LD_LIBRARY_PATH:-}"
echo "Launching Minecraft 1.2.5 Full Retro (GraalVM Native)..."
exec "$HERE/mc-retro-native" \
    -Dorg.lwjgl.librarypath="$HERE/lib" \
    -Dnet.java.games.input.librarypath="$HERE/lib" \
    "$@"
RUNNER
chmod +x "$DIST_DIR/run.sh"

# 6. Readme
cat << 'README' > "$DIST_DIR/README.txt"
Minecraft 1.2.5 Full Retro Modpack - GraalVM Native Edition
============================================================
This package is an Ahead-Of-Time (AOT) compiled standalone native
Linux x86_64 distribution of Minecraft 1.2.5 + Forge 3.4.9 + 46 mods.

Quick Start:
  ./run.sh

Features:
- Instant startup (no JVM bytecode warmup)
- 46 mods pre-loaded (IC2, BuildCraft, RedPower 2, EE2, NEI, etc.)
- Native desktop OpenGL with full hardware acceleration
- 3D spatial audio via OpenAL Soft
- Saves directory located at: .minecraft/saves/
README

echo "Creating compressed tarball..."
tar -czf "$OUT_TAR" -C "$BUILD_DIR" "mc-retro-graalvm-linux-x64"

echo "=== GraalVM Distribution Package Created Successfully ==="
ls -lh "$OUT_TAR"
