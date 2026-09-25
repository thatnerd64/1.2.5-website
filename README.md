# Minecraft 1.2.5 + Forge in the browser (Full Retro)

Minecraft 1.2.5 with Minecraft Forge and the **Full Retro** modpack's mods and configs, compiled from Java
bytecode to JavaScript with [TeaVM](https://teavm.org) and running in any modern browser with WebGL 2. The
game has no Java plugin, no JVM emulator and no server: it is a static web page.

> Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
> **This repository contains no Minecraft code or assets.** Each person builds the game from their own
> `minecraft.jar`; see [`input/README.md`](input/README.md).

## How it works

```
input/minecraft.jar  ─┐                        ┌─ runtime/   LWJGL → WebGL 2 / Web Audio, AWT, ImageIO,
(your Forge jar)      │   buildtools/Prepare   │             file system (IndexedDB), sockets (WebSocket)
modpack/mods/*        ├─► merge + patch ───────┤
modpack/config/*      │   bytecode             ├─ gameglue/  FML class-loader replacement, entry point
                      ┘                        │
                                               └─► TeaVM ─► dist/classes.js + assets.pak + fs.pak
```

* **Prepare** (`buildtools/`) merges your jar and every mod in the order FML's class loader would see them,
  redirects the handful of JDK calls TeaVM lacks, applies runtime class patches ahead of time (e.g.
  LumySkinPatch), drops classes that could never load on a JVM (missing supertypes), and packs resources and
  the initial `.minecraft` folder (configs, mod list, sounds).
* **Runtime** (`runtime/`) implements the platform the game expects:
  * `org.lwjgl.opengl.*`: an OpenGL 1.x fixed-function emulator on WebGL 2 (matrix stacks, lighting, fog,
    alpha test, texgen, display lists compiled to VBOs/VAOs, client-side vertex arrays, FBOs).
  * `org.lwjgl.input.*`: keyboard and mouse with pointer lock. `org.lwjgl.openal.*`: OpenAL on Web Audio.
  * `java.awt.*`, `javax.imageio.*`: the imaging subset Minecraft and mods use (PNG codec included).
  * `java.io.File`: an in-memory file system; everything the game writes (worlds, options, edited configs)
    is saved to IndexedDB.
  * `java.util.logging`, `java.net.Socket` (over a WebSocket proxy), `MessageDigest`, charsets and more.
* **FML** runs unmodified apart from its class loader: it scans `.minecraft/mods` (stub jars listing each mod's
  entries) and instantiates mods through `Class.forName`, so load order, configs and logging behave as on
  desktop.
* **teavm-plugin/** configures TeaVM (reflection for game classes, `Class.forName` for mod classes,
  JVM-style lazy linking for optional cross-mod references) and carries fixes for four TeaVM 0.15 compiler bugs
  hit by this code base (each marked "Modified for Minecraft Web").

## Building

Requirements: JDK 17+ (21 recommended), about 12 GB of free RAM for the TeaVM step, and your own jar.

```sh
cp /path/to/.minecraft/bin/minecraft.jar input/minecraft.jar     # Forge-patched 1.2.5 client
cp -r /path/to/.minecraft/resources input/resources             # optional: sounds and music
./gradlew build                                                  # -> dist/
```

`./gradlew build -PdevBuild=true` produces readable (unminified) JavaScript for debugging.
`-PexcludeMods=CraftPresence-Forge-1.2.5-Release-1.9.6.jar,...` leaves mods out.

Serve `dist/` with any static web server and open it:

```sh
cd dist && python3 -m http.server 8080      # then open http://localhost:8080
```

`dist/` contains code compiled from your `minecraft.jar`, so it is yours to use; don't publish it.

## Playing

* Enter a username and press **Play**. Worlds and settings persist in the browser (IndexedDB); the launcher
  can export them as a zip or delete them.
* Click the game to capture the mouse; **Esc** releases it and opens the menu. **F11** toggles full screen.
  **F2** screenshots are also downloaded by the browser.
* Multiplayer: run [`tools/ws-proxy.js`](tools/ws-proxy.js) (Node.js + `ws`), enter its `ws://` / `wss://`
  address in the launcher, then use Minecraft's normal Multiplayer screen.

## The modpack

[`modpack/`](modpack/) is the Full Retro `.minecraft` content: `mods/`, `config/`, per-mod folders and
`options.txt`. See [`modpack/README.md`](modpack/README.md) for the mod list, status and how to add mods.

## Repository layout

| Path | Purpose |
|------|---------|
| `buildtools/` | Prepare step (merge, patch, pack) and TeaVM driver |
| `teavm-plugin/` | TeaVM policies/plugins and patched TeaVM classes |
| `runtime/` | Browser platform: LWJGL, AWT, ImageIO, file system, audio, input, JDK gaps |
| `gameglue/` | Code compiled against the game: entry point, FML class loader |
| `web/` | Page, launcher, Web Audio OpenAL (`js/al.js`) |
| `modpack/` | Full Retro mods and configs |
| `input/` | Your `minecraft.jar` and resources (git-ignored) |
| `tools/` | WebSocket → TCP proxy for multiplayer |
