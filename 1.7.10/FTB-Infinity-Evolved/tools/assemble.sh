#!/usr/bin/env bash
# Puts a remote build's output (dist-remote/) together with the page into a servable folder.
#   tools/assemble.sh [src=dist-remote] [out=dist]
set -euo pipefail
cd "$(dirname "$0")/.."
SRC=${1:-dist-remote}; OUT=${2:-dist}
mkdir -p "$OUT"
cp -r web/. "$OUT"/
cp "$SRC"/build/teavm/classes.js "$OUT"/
cp -r "$SRC"/build/prepared/web/. "$OUT"/
echo "assembled $OUT: $(du -sh "$OUT" | cut -f1)"
