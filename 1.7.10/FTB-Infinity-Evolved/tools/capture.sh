#!/usr/bin/env bash
# Capture run: starts the real 1.7.10 client with the whole pack on a JDK 8 (headless: Xvfb + Mesa's software GL),
# opens a singleplayer world, plays a while and quits. A Java agent (aot/.../CaptureAgent.java) records every class
# the game generates at run time; the build compiles them in (see Prepare: build/capture/generated.jar).
#
#   JAVA8_HOME=/path/to/jdk8 tools/capture.sh [play-seconds=120]
#
# Needs xvfb-run and Mesa (libGL) on the machine. Run after tools/aot.sh (reuses its classpath list).
# Output: build/capture/generated.jar, build/capture/client.log
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT=$PWD
JAVA8_HOME=${JAVA8_HOME:-$(ls -d "$HOME"/dev/jdk8/jdk8u*-b* 2>/dev/null | head -1)}
PLAY=${1:-120}
IN=input
OUT=build/capture
[ -f build/aot/classpath.txt ] || { echo "run tools/aot.sh first"; exit 2; }
mkdir -p $OUT/natives $OUT/agent

# LWJGL and JInput natives (the AOT step never opens a window, so fetch.mjs does not get them)
for u in org/lwjgl/lwjgl/lwjgl-platform/2.9.1/lwjgl-platform-2.9.1-natives-linux.jar \
         net/java/jinput/jinput-platform/2.0.5/jinput-platform-2.0.5-natives-linux.jar; do
  f=$OUT/natives/$(basename $u)
  [ -s $f ] || curl -sSfL -o $f https://libraries.minecraft.net/$u
  (cd $OUT/natives && "$JAVA8_HOME/bin/jar" xf $(basename $u) && rm -rf META-INF)
done

# the agent jar
"$JAVA8_HOME/bin/javac" -nowarn -cp $IN/libraries/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar -d $OUT/agent \
  aot/src/main/java/retro/aot/CaptureAgent.java
printf 'Premain-Class: retro.aot.CaptureAgent\n' > $OUT/agent.mf
"$JAVA8_HOME/bin/jar" cfm $OUT/agent.jar $OUT/agent.mf -C $OUT/agent .

# the game directory: the pack's configs and mods, as the AOT step sees them
rm -rf $OUT/game; mkdir -p $OUT/game/mods
cp -r $IN/pack/config $OUT/game/config
[ -d $IN/pack/scripts ] && cp -r $IN/pack/scripts $OUT/game/scripts
[ -d $IN/pack/resources ] && cp -r $IN/pack/resources $OUT/game/resources
[ -d $IN/pack/modpack ] && cp -r $IN/pack/modpack $OUT/game/modpack
# FML's splash screen draws from a second GL context on its own thread; under Xvfb + llvmpipe it only gets in the way
sed -i 's/^enabled=.*/enabled=false/' $OUT/game/config/splash.properties 2>/dev/null || true
grep -q '^enabled=' $OUT/game/config/splash.properties 2>/dev/null || echo 'enabled=false' >> $OUT/game/config/splash.properties
for f in $IN/pack/mods/*; do ln -s "$ROOT/$f" "$OUT/game/mods/$(basename "$f")"; done
for f in $(ls -d $OUT/game/mods/* | grep -E '/(fastcraft|Patcher|CustomMainMenu)' || true); do rm -f "$f"; done
printf 'lang:en_US\nfboEnable:true\nrenderDistance:4\nfancyGraphics:false\n' > $OUT/game/options.txt

# the client's classpath: the AOT step's (all of the game's libraries) without its own tool directory, plus twitch
CP=$(cut -d: -f2- build/aot/classpath.txt)
for j in $(find $IN/libraries/tv -name '*.jar' ! -name '*natives*'); do CP="$CP:$ROOT/$j"; done

cd $OUT
xvfb-run -a -s "-screen 0 1280x720x24" "$JAVA8_HOME/bin/java" -Xmx6g -Xms2g \
  -javaagent:agent.jar=generated.jar -Dretro.capture.play=$PLAY \
  -Djava.library.path=natives -Dfml.ignoreInvalidMinecraftCertificates=true -Dfml.ignorePatchDiscrepancies=true \
  -cp "$CP" net.minecraft.launchwrapper.Launch \
  --tweakClass cpw.mods.fml.common.launcher.FMLTweaker --version 1.7.10 --gameDir "$ROOT/$OUT/game" \
  --assetsDir "$ROOT/$IN/assets" --assetIndex 1.7.10 --username Capture --accessToken 0 --userProperties '{}' \
  --uuid 00000000000000000000000000000000 --userType legacy > client.log 2>&1 || true
grep '\[capture\]' client.log || true
ls -la generated.jar 2>/dev/null || echo "no generated.jar (see $OUT/client.log)"
