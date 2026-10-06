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
  * `java.util.logging`, `java.net.Socket` (over a WebSocket relay, see [Multiplayer](#multiplayer)), `MessageDigest`, charsets and more.
* **FML** runs unmodified apart from its class loader: it scans `.minecraft/mods` (stub jars listing each mod's
  entries) and instantiates mods through `Class.forName`, so load order, configs and logging behave as on
  desktop.
* **teavm-plugin/** configures TeaVM (reflection for game classes, `Class.forName` for mod classes,
  JVM-style lazy linking for optional cross-mod references) and carries fixes for five TeaVM 0.15 compiler bugs
  hit by this code base (each marked "Modified for Minecraft Web").

## Building

Requirements: JDK 17+ (21 recommended), about 14 GB of free RAM for the TeaVM step (`-PteavmHeap=11g` sets its heap), and your own jar.

```sh
cp /path/to/.minecraft/bin/minecraft.jar input/minecraft.jar     # Forge-patched 1.2.5 client
cp -r /path/to/.minecraft/resources input/resources             # optional: sounds and music
./gradlew build                                                  # -> dist/
```

`./gradlew build -PdevBuild=true` produces readable (unminified) JavaScript for debugging.
`./gradlew build -PprofileBuild=true` is a release build that names its functions, for [benchmarking](#benchmarking).
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
* Multiplayer uses Minecraft's normal Multiplayer screen; see [Multiplayer](#multiplayer) below.

## Multiplayer

The browser client joins ordinary Minecraft 1.2.5 servers running the original Java server code (vanilla, or
Forge with Full Retro's server mods). Browsers can't open TCP connections, so a small relay,
[`tools/ws-proxy.js`](tools/ws-proxy.js), carries the game's connection over a WebSocket. It needs Node.js 16+
and nothing else. The packets themselves are the game's own, so the server needs no plugin or mod.

**Quick setup:** [`tools/setup-proxy.sh`](tools/setup-proxy.sh) asks whether you're a player or a server owner,
sets up TLS for server owners (your certificate, a free Cloudflare tunnel, or your own reverse proxy), starts the
relay and prints the address to use. It downloads `ws-proxy.js` if it isn't next to it, so it also works on its
own (Linux, macOS, or Git Bash/WSL on Windows):

```sh
curl -fsSLO https://raw.githubusercontent.com/thatnerd64/1.2.5-website/main/tools/setup-proxy.sh
bash setup-proxy.sh            # or: bash setup-proxy.sh --player / --server --tunnel
```

The details, for doing it by hand:

**The server** must have `online-mode=false` in `server.properties`: a browser has no Minecraft session (and
Mojang's 1.2.5 login servers are long gone, so live 1.2.5 servers already run this way). The game says so
("Failed to login: the server must set online-mode=false") if it isn't. For a modded server, use the same mod
versions and configs as the client, i.e. Full Retro's server pack.

**Server owners** run the relay next to the server, and players add `wss://your.host:25566` as the server
address in Minecraft's Multiplayer screen, like any other address:

```sh
node tools/ws-proxy.js --listen 0.0.0.0:25566 --target 127.0.0.1:25565 --cert fullchain.pem --key privkey.pem
```

The game's page is served over HTTPS, so the relay has to be reachable as `wss://` (TLS): give it a
certificate as above, put it behind a TLS reverse proxy (Caddy, nginx), or use a tunnel such as
`cloudflared tunnel --url http://localhost:25566`, which prints an `https://….trycloudflare.com` address that
players enter as `wss://….trycloudflare.com`. Any other WebSocket-to-TCP bridge (e.g. websockify) works too.

**Players** can also reach any 1.2.5 server themselves by running the relay on their own computer:

```sh
node tools/ws-proxy.js          # listens on ws://localhost:25566
```

Enter `ws://localhost:25566` as **Multiplayer relay** in the launcher, then add servers by their normal
address (`play.example.com:25565`). The browser may ask for permission to connect to devices on the local
network. This relay only listens on localhost and only answers this site's pages and pages served from
localhost (`--origin` changes that), so other websites can't use it to reach your network.

How it behaves:

* Addresses starting with `ws://` or `wss://` connect straight to that relay; anything else (`host:port`)
  goes through the launcher's relay, which connects to that server.
* Connection problems read as on desktop: the server list shows "Can't resolve hostname" / "Can't reach
  server", a crashed server "Connection reset", a kick its reason.
* The game keeps running in a background tab (a small Web Worker keeps time where the browser would throttle
  timers), so the server doesn't time the player out. Switching away opens the game menu, as on desktop.
* Full Retro's client mods send Forge's own packets (e.g. packet 131) after logging in, which a vanilla server
  rejects ("Bad packet id"); that happens with the desktop client too. Use a Forge server with the pack's mods.
* Direct Connect's Enter key does nothing in 1.2.5 (a vanilla bug); click **Join Server**.

Tested with the game in headless Chromium against a stand-in 1.2.5 server built from the client's own packet
classes (Mojang's server jar isn't available in the test environment): server list ping (MOTD, player count),
login, terrain, chat, two players seeing each other and joining/leaving, breaking blocks, kicks, a server
going down, an online-mode server, a missing or stopped relay, direct `ws://` addresses, and 75 seconds in a
hidden tab. A real Forge server with the full mod set has not been tried yet.

## Benchmarking

[`tools/benchmark.js`](tools/benchmark.js) measures the game on your computer, in Chrome, Edge or Chromium: frame
rates, and where the time goes. It needs Node.js 22+ and nothing else.

```sh
./gradlew build -PprofileBuild=true     # as fast as a normal build, but the report can name functions
node tools/benchmark.js                 # about 3 minutes
```

It serves `dist/`, starts the browser with a fresh temporary profile (your saves and settings are not touched) and
opens the game in benchmark mode: the game creates a new world from a fixed seed, stands still for 20 s while the spawn
area loads, then measures 30 s standing still and 30 s walking forward while weaving left and right (new chunks load
and are built). Keep its window visible until it closes. The report is printed and saved, with the raw data, in
`benchmark-results/<date>/`:

* For each phase: average FPS, 1% low, median/95th/99th percentile frame times, the number of entities, how busy the
  page's main thread (where the game runs) was and how much of that was inside WebGL calls. A main thread near 100%
  busy means the game's code limits the frame rate; well below 100% at the display's refresh rate means headroom, and
  at a lower rate that the GPU limits it.
* With a profile build, where the main thread's time goes: the game tick (entity AI, movement, block ticks...), chunk
  saving, rendering (chunk building, terrain, entities...), mods and garbage collection; by code layer (game code, Java
  class library, TeaVM's runtime, GL emulation); and the hottest functions, by their Minecraft (MCP) names.

Options (`--help` lists them all): `--seed TEXT`; phase lengths `--warmup`, `--idle`, `--walk` (seconds); game
settings `--render far|normal|short|tiny`, `--graphics fancy|fast`, `--smooth on|off`, `--clouds on|off`,
`--difficulty peaceful|easy|normal|hard` (the default is the game's settings for new players); `--size 1920x1080`;
`--uncapped` to let the frame rate go above the display's refresh rate (not with software WebGL, which then stalls);
`--no-profile` for frame rates without the profiler's overhead; `--browser PATH`. Compare runs with the same seed,
size and settings.

The `<phase>.cpuprofile` files open in Chrome DevTools (Performance panel, *Load profile*), and
`node tools/profile-report.js <file>.cpuprofile` summarises one again. Without Node.js, open the page yourself with
`?benchmark` (for example `http://localhost:8080/?benchmark=myseed&render=far`): it runs the same phases and shows the
frame rates at the end.

A profile build is a release build with readable JavaScript names plus `dist/profile-symbols.json`, which maps them
to Minecraft's method names with MCP 6.2's mappings from Forge's 1.2.5 source zip (downloaded from
maven.minecraftforge.net when needed; `-PmcpZip=path` uses a local copy). Its `classes.js` is about three times
larger, so play with a normal build.

## Status

Tested in headless Chromium (software WebGL) with all 46 FML mods of the pack loaded, plus its jar mods: the
title screen, world creation, terrain generation and rendering, movement, mouse look, mining, inventory, menus,
the F3 debug screen, NotEnoughItems (item panel, cheat mode, plugins for BuildCraft, Forestry, Railcraft,
RedPower, EE2 and Wireless Redstone), Inventory Tweaks, Rei's Minimap, Block Helper, Single Player Commands
with WorldEdit (`/give`, `//pos1`, `//wand`), and saving a world, reloading the page and continuing it.

* **Performance** depends heavily on the machine. New players start with browser-friendly video settings
  (Short render distance, Fast graphics, no smooth lighting or clouds, fewer particles); raise them in Options
  if your machine keeps up. The GL emulation merges the many tiny draws of text, HUD icons and mob models into a
  few (about 350 draw calls a frame down to about 120 in a test world). On the CPU-only test machine (software
  WebGL) a fresh world runs at about 11 FPS; a desktop browser with a real GPU does considerably better.
* **Networked extras don't work in a browser**: update checks, Mojang skin lookups (LumySkinPatch) and
  CraftPresence's Discord status fail quietly (CORS / no local IPC).
* Keys and clicks: a press is always seen by the game for at least a frame, so quick taps register even in
  mods that poll key state once per tick.
* Mods that probe for optional integrations behave as on a desktop JVM: the build rejects classes the JVM
  bytecode verifier would reject (e.g. IC2's NEI support when NEI is absent).
* `java.util.Random` is JDK-exact, so a seed generates the same world as desktop Minecraft 1.2.5.

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
| `tools/` | `ws-proxy.js`: WebSocket → TCP relay for multiplayer; `benchmark.js`, `profile-report.js`: benchmarks |
