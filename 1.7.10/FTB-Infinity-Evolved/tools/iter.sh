#!/usr/bin/env bash
# One edit-build-run iteration: remote dev build, assemble, run headless and print the log.
#   tools/iter.sh [seconds-to-run=40] [gradle args...]
set -uo pipefail
cd "$(dirname "$0")/.."
SECS=${1:-40}; shift || true
TEAVM_HEAP=${TEAVM_HEAP:-14g} tools/remote-build.sh -PdevBuild=true "$@" > build-remote.log 2>&1
grep -E "BUILD|EXIT|FAILED|OutOfMemory|Classes compiled|Methods compiled" dist-remote/build.log | head
if ! grep -q "^EXIT 0" dist-remote/build.log; then
  echo "=== BUILD FAILED (not running the old output) ==="
  grep -B1 -A4 "error:" dist-remote/build.log | head -60
  exit 1
fi
tools/assemble.sh >/dev/null
timeout $((SECS + ${EXTRA_TIMEOUT:-420})) node tools/run.cjs dist "$SECS" ${RUN_ARGS:-} > run.log 2>&1
grep -v 'WebGL: INVALID\|WebGL-0x\|Forge Version\|XMLHttp\|Failed to load resource\|STDOUT\] *$\|TeaVMThread\|^    at \$rt\|^    at jl_\|Preparing spawn' run.log \
  | cut -c1-${WIDTH:-300} | head -${LINES_MAX:-150}
