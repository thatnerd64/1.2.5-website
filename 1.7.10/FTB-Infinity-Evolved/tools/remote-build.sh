#!/usr/bin/env bash
# Runs the whole build on another machine (the TeaVM step needs 10+ GB of RAM) from source alone: the machine
# downloads its own inputs (tools/fetch.mjs) and runs fetch -> AOT dump -> prepare -> TeaVM there.
#
#   REMOTE=user@host REMOTE_DIR=build1710 tools/remote-build.sh [gradle args, e.g. -PnoMods=true]
#
# Remote prerequisites (all unpacked under $REMOTE_TOOLS, nothing installed system-wide):
#   node 22, a JDK 8 (JAVA8_HOME), a JDK 17 (REMOTE_JDK).
# Brings back build/teavm (classes.js), build/prepared/web (packs), build/unresolved.txt and build.log into dist-remote/.
set -euo pipefail
cd "$(dirname "$0")/.."
REMOTE=${REMOTE:?set REMOTE=user@host (the build machine)}
# SSH command (e.g. SSH="sshpass -e ssh -o StrictHostKeyChecking=no" with SSHPASS set, for a host without key auth)
SSH=${SSH:-ssh}
REMOTE_DIR=${REMOTE_DIR:-build1710}
REMOTE_TOOLS=${REMOTE_TOOLS:-'$HOME/build1710-tools'}
REMOTE_JDK=${REMOTE_JDK:-'$HOME/build125/jdk'}
HEAP=${TEAVM_HEAP:-11g}

# Sources only: no inputs, no build output.
{ tar czf - --no-wildcards-match-slash --exclude=./input --exclude=./.gradle --exclude=./build --exclude=./dist --exclude=./dist-remote \
    --exclude=./node_modules --exclude='./*/build' . || [ $? = 1 ]; } \
  | $SSH "$REMOTE" "mkdir -p $REMOTE_DIR && for d in aot buildtools gameglue runtime teavm-plugin; do rm -rf $REMOTE_DIR/\$d/src; done \
      && tar xzf - -C $REMOTE_DIR"   # (sources replaced, not merged: a deleted file must not linger there)

$SSH "$REMOTE" "cd $REMOTE_DIR && echo 1000 > /proc/self/oom_score_adj; \
  export JAVA_HOME=$REMOTE_JDK JAVA8_HOME=\$(ls -d $REMOTE_TOOLS/jdk8u*) PATH=\$(ls -d $REMOTE_TOOLS/node-v*)/bin:\$PATH; \
  { { node tools/fetch.mjs || echo 'fetch failed; continuing with the inputs already there'; } && nice -n 19 ./gradlew ${TASK:-compileWeb} ${GRADLE_OFFLINE---offline} -PteavmHeap=$HEAP -Dorg.gradle.jvmargs=-Xmx512m --no-daemon --console=plain $*; } > build.log 2>&1; \
  echo EXIT \$? >> build.log; tail -40 build.log"

mkdir -p dist-remote
# PULL_OUTPUT= (empty) brings back the logs only (use tools/remote-run.sh to test on the build machine).
OUTPUTS=$([ -z "${PULL_OUTPUT-1}" ] || echo build/teavm build/prepared/web)
$SSH "$REMOTE" "cd $REMOTE_DIR && tar czf - ${FULL_PULL:+} $([ -n "${FULL_PULL:-}" ] || echo --exclude=fs.pak.d) $OUTPUTS build/prepared/prepare.log build/aot/dump.log build/aot/renamed.txt build/unresolved.txt build.log 2>/dev/null" \
  | tar xzf - -C dist-remote || true
