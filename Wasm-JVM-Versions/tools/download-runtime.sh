#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WEB_DIR="$ROOT_DIR/web"

mkdir -p "$WEB_DIR"

BASE_URL="https://javabox-demo.brian-fec.workers.dev"

echo "=== OpenJDK Zero WebAssembly Runtime Downloader ==="

FILES=(
  "javabox-direct.mjs"
  "javabox-direct.wasm"
  "javabox-direct.data"
  "xterm.js"
  "xterm.css"
)

for file in "${FILES[@]}"; do
  dest="$WEB_DIR/$file"
  if [ -s "$dest" ]; then
    echo "  [OK] $file already present"
  else
    echo "  [DOWNLOADING] $file from $BASE_URL/$file..."
    curl -fL --progress-bar -o "$dest.part" "$BASE_URL/$file"
    mv "$dest.part" "$dest"
    echo "  [SAVED] $file"
  fi
done

echo ""
echo "All runtime assets verified in $WEB_DIR:"
ls -lh "$WEB_DIR"
