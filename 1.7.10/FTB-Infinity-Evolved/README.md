# FTB Infinity Evolved (Minecraft 1.7.10 + Forge) in the browser

A port of [FTB Infinity Evolved 1.7](https://www.feed-the-beast.com/) (pack version 3.1.0: Minecraft 1.7.10, Forge
10.13.4.1614, 140 mods) to the browser with [TeaVM](https://teavm.org), built on the same runtime as the Minecraft
1.2.5 build in the repository root. The game is compiled from Java bytecode to JavaScript; there is no JVM, and the
result is a static web page that needs WebGL 2. Multiplayer goes through a WebSocket relay (see [Multiplayer](#multiplayer)).

> Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
> **This folder contains no Minecraft code, assets or mods.** `tools/fetch.mjs` downloads everything from the
> official sources into `input/` (git-ignored) when you build.

**Status: work in progress.** See [Status](#status) for what runs today.

## How it works

Forge 1.7.10 is a bytecode-transforming platform: LaunchWrapper's class loader runs a chain of ASM transformers over
every class as it is loaded (deobfuscation to SRG names, access transformers, Forge's binary patches to Minecraft,
`@SideOnly` stripping, `@Optional` stripping, event-subscription hooks, plus every coremod's own transformers). A
browser has no class loader, so that chain runs at build time instead:

```
tools/fetch.mjs ──► input/  (Mojang jar + libraries + asset index, Forge universal, the pack's 140 mods + configs)
                      │
tools/aot.sh (JDK 8) ─┤  runs the real Forge launch chain headlessly (aot/Dump.java), brings FML up to mod
                      │  construction, then pushes every class of every jar through the live transformer chain
                      ▼
build/aot/classes.jar     all classes, transformed          build/aot/discovery.json   what mod discovery found
                      │
tools/capture.sh ─────┤  optional: runs the real client with the pack (JDK 8, Xvfb + Mesa), opens a world, records
  (JDK 8)             │  every class the game generates at run time and the class bytecode mods read
                      ▼
build/capture/generated.jar + classbytes.txt
                      │
buildtools/Prepare ───┤  patches JDK calls TeaVM lacks, drops classes a JVM could not load, packs resources
                      ▼
build/prepared/game.jar + web/assets.pak (classpath resources) + web/fs.pak (initial .minecraft)
                      │
buildtools/Compile ───┤  TeaVM (runtime/, teavm-plugin/, gameglue/ are compiled in)
                      ▼
classes.js  +  assets.pak  +  fs.pak  +  index.html        (dist/)
```

* **aot/** (JDK 8, build time only). `DumpTweaker` makes `Dump` the launch target; `Discovery` records the result of
  FML's mod scan (classes, `@Mod` containers, annotation data, `mcmod.info`) because the browser cannot unzip and
  ASM-parse 140 jars at start-up.
* **Classes generated at run time.** About ten mods build classes with ASM while the game runs and define them
  through `ClassLoader.defineClass` (ForgeMultipart's trait mixins, AE2's part layers, LogisticsPipes' proxy wrappers,
  ExtraUtilities, FunkyLocomotion, OpenMods, Immersive Engineering's NEI handlers, and MineTweaker, which compiles the
  pack's scripts to bytecode). A browser cannot load bytecode, so `tools/capture.sh` runs the real game once with a
  Java agent (`aot/.../CaptureAgent.java`) that records those classes; the build compiles them in, and the browser's
  `defineClass` returns the compiled class with the same bytes (or name). It also records which classes mods read
  through `LaunchClassLoader.getClassBytes` (ForgeMultipart, ExtraUtilities' Ender Quarry), whose bytecode the build
  packs. Multipart combinations nobody built during the capture run cannot be created in the browser.
* **runtime/** the browser platform: LWJGL 2 on WebGL 2 (a fixed-function OpenGL emulator), OpenAL on Web Audio, AWT
  imaging, an in-memory file system persisted to IndexedDB, plus the JDK pieces TeaVM 0.15 lacks (notably
  `java.util.concurrent` executors/futures/latches, `java.security` key types, `UUID`, `Proxy`).
* **gameglue/** replacements for classes that cannot work as-is: LaunchWrapper's `Launch`/`LaunchClassLoader`, FML's
  `ModClassLoader`, `JarDiscoverer` (replays `discovery.json`), `ASMEventHandler` (Forge generates a class per
  `@SubscribeEvent` method at run time; this calls the method reflectively), log4j's `LogManager`, and the entry point.
* **teavm-plugin/** TeaVM configuration: reflection policy, `Class.forName` for mod classes, JVM-style lazy linking.
* **buildtools/** `Prepare` (bytecode patches, class lists, packs), `Compile`, and two static checkers:
  `ApiCheck` (LWJGL calls the shims lack) and `JdkCheck` (JDK calls TeaVM and the shims lack).

## Building

Needs Node 18+, a JDK 8 (`JAVA8_HOME`, for the AOT step), a JDK 17 for Gradle, and for the TeaVM step roughly
10-14 GB of free RAM.

```sh
node tools/fetch.mjs                     # ~600 MB: Mojang jar/libraries/assets, Forge, the pack (idempotent, sha1-checked)
./gradlew aot && tools/capture.sh        # optional but needed by the mods that generate classes (needs xvfb-run, Mesa)
./gradlew compileWeb -PdevBuild=true     # aot -> prepare -> TeaVM; -PnoMods=true builds vanilla + Forge only
tools/assemble.sh build dist             # (if built locally) put page + packs together
```

`tools/remote-build.sh` runs the whole thing on another machine from source alone (it fetches its own inputs);
`PULL_OUTPUT=` (empty) brings back only the logs, and `tools/remote-run.sh` then runs the result in headless Chromium on
that machine (the full build needs more memory in the browser than a small desktop has). `tools/iter.sh` is one
edit-build-run loop that pulls the output and runs it locally.

## Multiplayer

Browsers cannot open TCP sockets, so the game reaches servers through a WebSocket-to-TCP relay, the same one the 1.2.5
build uses (`tools/ws-proxy.js` in the repository root; it forwards bytes unchanged, so it serves 1.7.10 as is).
Netty's `NioSocketChannel`, which Minecraft hands to its `Bootstrap` for every outgoing connection (joining and the
server list ping), is replaced by a channel over `retro.net.WebSocketConnection`
(`gameglue/.../io/netty/channel/socket/nio/NioSocketChannel.java`). A server address is either `host[:port]`, reached
through the relay set in `retroConfig.proxy`, or a `ws://` / `wss://` URL of a relay in front of one server, e.g.
`wss://relay.example/?host=127.0.0.1&port=25565` (Minecraft's `ServerAddress` keeps such URLs whole; see the scoped
redirect in `Patches`). The page query `?server=<address>` joins a server as soon as the game has loaded.

A matching server (the pack minus its client-only mods and minus the mods the browser build leaves out, so Forge's
mod list check passes): `node tools/fetch-server.mjs <dir>` downloads it (Minecraft server, Forge, libraries, mods,
configs); then `java -Xmx4G -jar forge-1.7.10-10.13.4.1614-1.7.10-universal.jar nogui` in that directory with Java 8,
`online-mode=false` (browser players have no Mojang session), `level-type=BIOMESOP` as the pack recommends, and the
relay in front of it: `node tools/ws-proxy.js --listen 0.0.0.0:25566 --target 127.0.0.1:25565`.

## Status

Work in progress; this is what has been verified (headless Chromium, `tools/run.cjs`):

| Stage | State |
|---|---|
| Fetch Mojang jar/libraries/assets, Forge, the pack's 140 mods and configs | works, sha1-checked |
| AOT dump through Forge's real transformer chain, **without mods** | works: 10.4k classes |
| AOT dump **with all 140 mods and their coremods** | works: 53k classes, mod discovery recorded; the few classes that fail are other-side classes and optional-dependency compat classes (as on a JVM) |
| TeaVM compile + run, **vanilla 1.7.10 + Forge** (`-PnoMods=true`) | **works**: title screen, create world, the integrated server generates the world, Netty local channel and FML handshake, rendering with HUD |
| TeaVM compile + run **with the mods** | **title screen**: all 181 mods load without errors ("181 mods loaded, 181 mods active"); headless SwiftShader runs need ~40 min to get there. Creating and playing a world is next |
| Classes mods generate at run time | compiled in from a capture run of the real game (`tools/capture.sh`); multipart combinations not seen in that run cannot be created |
| Sound | not working yet (the SoundSystem falls back to "No Sound") |
| Multiplayer | Netty channel over the WebSocket relay; servers in offline mode (no Mojang session in the browser). Online-mode encryption is not supported |

Known gaps and decisions:

* Mojang's sounds are packed as lazy files (`fs.pak.d/`); mod assets share the classpath pack (`assets.pak`).
* `fastcraft`, `Patcher` and `CustomMainMenu` are left out of the AOT dump and the pack (performance/menu mods that
  patch the game in ways that make no sense here).
* A coremod patch that cannot be applied (NEI's `GuiContainer.mouseReleased` hook does not find its target) is skipped
  with a note in `build/aot/dump.log` instead of dropping the class.
* The TeaVM step for the full pack needs far more memory than vanilla (see `tools/remote-build.sh`: `TEAVM_HEAP`).

### Working on it

`tools/iter.sh <seconds> [-PnoMods=true]` is the edit-build-run loop (remote dev build, assemble, headless run);
`tools/remote-run.sh <seconds> [run.cjs flags]` runs the last remote build on the build machine and prints the log with
Java stack frames (a `-Pdebuginfo=true` build). Page switches: `?debuglog=<logger,...>` logs everything from those
loggers, `?debugreflect=<class>` logs the field types reflection reports for a class. `tools/Locate.java` turns Java
methods into positions for `run.cjs --breakat=` (with `--breakcond=` for a JavaScript condition); `--dumpfile=` prints
files the game saved.
`tools/run.cjs` can click through the menus (`--clicks`), sample stacks (`--stack`), break on compiled functions
(`--break`), print thrown exceptions (`--exceptions`) and trace WebGL (`--gltrace`). TeaVM 0.15's own class library
sources (useful to copy and fix a `T...` shim) are at
`https://repo1.maven.org/maven2/org/teavm/teavm-classlib/0.15.0/teavm-classlib-0.15.0-sources.jar`.
