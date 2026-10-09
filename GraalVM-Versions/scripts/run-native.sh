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

cd "$ROOT_DIR"

# Ensure assets.pak is accessible in working directory
if [ ! -e "assets.pak" ] && [ -f "build/prepared/web/assets.pak" ]; then
    ln -sf "build/prepared/web/assets.pak" "assets.pak"
fi
if [ ! -e "assets.pak.d" ] && [ -d "build/prepared/web/assets.pak.d" ]; then
    ln -sf "build/prepared/web/assets.pak.d" "assets.pak.d"
fi

# Ensure .minecraft folder is extracted from fs.pak if absent
if [ ! -d ".minecraft" ] && [ -f "build/prepared/web/fs.pak" ]; then
    echo "Unpacking modpack configs from fs.pak into .minecraft..."
    python3 -c "
import struct, os
pak_path = 'build/prepared/web/fs.pak'
out_dir = '.minecraft'
with open(pak_path, 'rb') as f:
    f.read(4)
    count, = struct.unpack('>I', f.read(4))
    entries = []
    for _ in range(count):
        nlen, = struct.unpack('>I', f.read(4))
        name = f.read(nlen).decode('utf-8')
        off, length = struct.unpack('>ii', f.read(8))
        entries.append((name, off, length))
    for name, off, length in entries:
        dest = os.path.join(out_dir, name)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        if off >= 0:
            f.seek(off)
            with open(dest, 'wb') as out:
                out.write(f.read(length))
"
fi

export LD_LIBRARY_PATH="${BUILD_DIR}:${LWJGL_NATIVES}:${LD_LIBRARY_PATH:-}"

echo "Starting Minecraft 1.2.5 Full Retro (GraalVM Native)..."
exec "$BIN" "$@"

