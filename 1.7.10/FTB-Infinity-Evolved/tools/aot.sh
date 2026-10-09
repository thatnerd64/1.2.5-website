#!/usr/bin/env bash
# Build-time AOT step: runs the real Forge 1.7.10 launch chain on a JDK 8 and dumps every class after all of
# Forge's and the coremods' transformers have run (see aot/src/main/java/retro/aot/Dump.java).
#
#   JAVA8_HOME=/path/to/jdk8 tools/aot.sh [--no-mods]
#
# Output: build/aot/classes.jar (+ dump.log, renamed.txt)
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT=$PWD
JAVA8_HOME=${JAVA8_HOME:-$(ls -d "$HOME"/dev/jdk8/jdk8u*-b* 2>/dev/null | head -1)}
[ -x "$JAVA8_HOME/bin/java" ] || { echo "JAVA8_HOME must point at a JDK 8 (LaunchWrapper and Forge 1.7.10 do not run on newer JVMs)"; exit 2; }
IN=input
[ -f $IN/minecraft/1.7.10.jar ] || { echo "run: node tools/fetch.mjs"; exit 2; }

OUT=build/aot
rm -rf $OUT/tool $OUT/game; mkdir -p $OUT/tool $OUT/game

# The game directory FML sees: the pack's configs (copied, mods rewrite them) and mods (linked).
cp -r $IN/pack/config $OUT/game/config 2>/dev/null || mkdir $OUT/game/config
if [ "${1:-}" != "--no-mods" ]; then
  mkdir -p $OUT/game/mods
  for f in $IN/pack/mods/*; do ln -s "$ROOT/$f" "$OUT/game/mods/$(basename "$f")"; done
  for f in $(ls -d $OUT/game/mods/* | grep -E '/(fastcraft|Patcher|CustomMainMenu)' || true); do rm -f "$f"; done
  # MOD_FILTER: keep only the mods whose file name matches this regex (a subset of the pack, to test with)
  if [ -n "${MOD_FILTER:-}" ]; then
    for f in $OUT/game/mods/*; do
      basename "$f" | grep -Eiq "$MOD_FILTER" || rm -f "$f"
    done
  fi
else
  mkdir -p $OUT/game/mods
fi

L=$IN/libraries
CP="$ROOT/$OUT/tool"
for j in \
  $L/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar \
  $L/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar \
  $L/com/google/guava/guava/17.0/guava-17.0.jar \
  $L/org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar \
  $L/net/sf/jopt-simple/jopt-simple/4.5/jopt-simple-4.5.jar \
  $L/lzma/lzma/0.0.1/lzma-0.0.1.jar \
  $L/org/apache/logging/log4j/log4j-api/2.0-beta9/log4j-api-2.0-beta9.jar \
  $L/org/apache/logging/log4j/log4j-core/2.0-beta9/log4j-core-2.0-beta9.jar \
  $IN/forge/forge-1.7.10-10.13.4.1614-1.7.10-universal.jar \
  $IN/minecraft/1.7.10.jar \
  $L/com/google/code/gson/gson/2.2.4/gson-2.2.4.jar \
  $L/io/netty/netty-all/4.0.10.Final/netty-all-4.0.10.Final.jar \
  $L/com/mojang/authlib/1.5.21/authlib-1.5.21.jar \
  $L/com/mojang/realms/1.3.5/realms-1.3.5.jar \
  $L/com/mojang/netty/1.8.8/netty-1.8.8.jar \
  $L/com/ibm/icu/icu4j-core-mojang/51.2/icu4j-core-mojang-51.2.jar \
  $L/net/sf/trove4j/trove4j/3.0.3/trove4j-3.0.3.jar \
  $L/java3d/vecmath/1.3.1/vecmath-1.3.1.jar \
  $L/net/java/jutils/jutils/1.0.0/jutils-1.0.0.jar \
  $L/net/java/jinput/jinput/2.0.5/jinput-2.0.5.jar \
  $L/org/lwjgl/lwjgl/lwjgl/2.9.1/lwjgl-2.9.1.jar \
  $L/org/lwjgl/lwjgl/lwjgl_util/2.9.1/lwjgl_util-2.9.1.jar \
  $L/com/paulscode/*/*/*.jar \
  $L/commons-codec/commons-codec/1.9/commons-codec-1.9.jar \
  $L/commons-io/commons-io/2.4/commons-io-2.4.jar \
  $L/commons-logging/commons-logging/1.1.3/commons-logging-1.1.3.jar \
  $L/org/apache/commons/commons-compress/1.8.1/commons-compress-1.8.1.jar \
  $L/org/apache/httpcomponents/httpclient/4.3.3/httpclient-4.3.3.jar \
  $L/org/apache/httpcomponents/httpcore/4.3.2/httpcore-4.3.2.jar \
  $L/com/typesafe/akka/akka-actor_2.11/2.3.3/akka-actor_2.11-2.3.3.jar \
  $L/com/typesafe/config/1.2.1/config-1.2.1.jar \
  $L/org/scala-lang/scala-library/2.11.1/scala-library-2.11.1.jar \
  $L/org/scala-lang/scala-reflect/2.11.1/scala-reflect-2.11.1.jar \
  $L/org/scala-lang/scala-actors-migration_2.11/1.1.0/scala-actors-migration_2.11-1.1.0.jar \
  $L/org/scala-lang/modules/*/*/*.jar ; do
  CP="$CP:$ROOT/$j"
done
echo "$CP" > $OUT/classpath.txt

"$JAVA8_HOME/bin/javac" -nowarn -d $OUT/tool -cp "$CP" $(find aot/src/main/java -name "*.java")

SKIP="org.lwjgl.,scala.swing.,scala.tools.,scala.xml.,scala.actors.,scala.concurrent.forkjoin.,akka.,com.typesafe.,cpw.mods.fml.relauncher.FMLSecurityManager,retro.aot."
cd $OUT
"$JAVA8_HOME/bin/java" -Xmx3g -Daot.out="$PWD" -Daot.skip="$SKIP" -Dfml.ignoreInvalidMinecraftCertificates=true \
  -cp "$CP" net.minecraft.launchwrapper.Launch \
  --tweakClass retro.aot.DumpTweaker --tweakClass cpw.mods.fml.common.launcher.FMLTweaker \
  --gameDir "$PWD/game" --assetsDir "$PWD/game/assets" --version 1.7.10 --username dump --accessToken 0 2>&1 | tee run.log | tail -40
