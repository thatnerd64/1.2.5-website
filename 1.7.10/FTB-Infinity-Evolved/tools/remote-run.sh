#!/usr/bin/env bash
# Runs the last remote build in headless Chromium on the build machine itself (it has the memory and the output
# already; nothing large crosses the network). Same REMOTE/SSH/REMOTE_DIR/REMOTE_TOOLS as tools/remote-build.sh.
#
#   tools/remote-run.sh [seconds] [run.cjs flags...]      (prints the run log; screenshot -> dist-remote/shot.png,
#                                                          --clicks step screenshots -> dist-remote/steps/)
#   RUN_TIMEOUT=<seconds> overrides the overall limit (seconds + 300), e.g. for --waitfor runs.
#
# Remote prerequisites: playwright-core in $REMOTE_PW and its Chromium (npx playwright-core install --with-deps chromium).
set -euo pipefail
cd "$(dirname "$0")/.."
REMOTE=${REMOTE:?set REMOTE=user@host (the build machine)}
SSH=${SSH:-ssh}
REMOTE_DIR=${REMOTE_DIR:-build1710}
REMOTE_TOOLS=${REMOTE_TOOLS:-'$HOME/build1710-tools'}
REMOTE_PW=${REMOTE_PW:-'$HOME/pw/node_modules/playwright-core'}
SECS=${1:-420}; shift || true

# Ship the (small) harness and page sources so edits to them take effect without a rebuild.
tar czf - tools web | $SSH "$REMOTE" "mkdir -p $REMOTE_DIR && tar xzf - -C $REMOTE_DIR"
$SSH "$REMOTE" "cd $REMOTE_DIR && export PATH=\$(ls -d $REMOTE_TOOLS/node-v*)/bin:\$PATH PLAYWRIGHT_CORE=$REMOTE_PW; \
  rm -rf dist steps && mkdir steps && tools/assemble.sh . dist >/dev/null && \
  timeout ${RUN_TIMEOUT:-$((SECS + 300))} node tools/run.cjs dist $SECS --shot=run-shot.png --stepdir=steps $* > run.log 2>&1; echo RUN EXIT \$? >> run.log; \
  if [ -f build/teavm/classes.js.teavmdbg ]; then \
    CP=\$(find ~/.gradle/caches/modules-2 -name 'teavm-core-0.15.0.jar' -o -name 'teavm-relocated-libs-*-0.15.0.jar' | grep -v sources | tr '\n' :); \
    \$(ls -d $REMOTE_TOOLS/jdk-17*)/bin/java -cp \$CP tools/Symbolize.java build/teavm/classes.js.teavmdbg < run.log > run.sym.log 2>&1 && mv run.sym.log run.log; fi; \
  cat run.log"
mkdir -p dist-remote
$SSH "$REMOTE" "cat $REMOTE_DIR/run-shot.png" > dist-remote/shot.png 2>/dev/null || true
rm -rf dist-remote/steps && mkdir -p dist-remote/steps
$SSH "$REMOTE" "cd $REMOTE_DIR && tar cf - steps" 2>/dev/null | tar xf - -C dist-remote 2>/dev/null || true
