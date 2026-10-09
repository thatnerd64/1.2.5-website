#!/usr/bin/env bash
# Runs the already assembled dist/ headless: boot, singleplayer -> create world, wait, screenshots in /tmp/mc-step*.png
#   tools/world.sh [seconds in world, default 60]
cd "$(dirname "$0")/.."
ANGLE=${ANGLE:-gl} MAXLEN=${MAXLEN:-2500} node tools/run.cjs dist 3 --boot=30 \
  --clicks="640,353@5;875,593@5;400,665@${1:-60};-1,-1@30;-1,-1@30" 2>&1 \
  | grep -v 'WebGL: INVALID\|TEXDBG\|Forge Version\|XMLHttp\|Failed to load\|STDOUT\] *$\|TeaVMThread\|DIAG' | cut -c1-300
