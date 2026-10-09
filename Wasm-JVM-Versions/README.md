# OpenJDK Zero in WebAssembly (Method 1: Embedded C++ JVM in Wasm)

This directory explores **Method 1**: running a full, real Java Virtual Machine inside modern web browsers by compiling the **OpenJDK 21 Zero bytecode interpreter** to **WebAssembly** via **Emscripten**.

Unlike Ahead-of-Time (AOT) compilers like TeaVM or Bytecoder that attempt to translate Java bytecode into JavaScript or WasmGC prior to runtime, Method 1 embeds an actual JVM interpreter in WebAssembly memory that executes unmodified `.class` and `.jar` bytecode dynamically inside the browser tab.

---

## 1. Architecture Overview

```
Browser Tab (Chrome / Firefox with SharedArrayBuffer)
  ├── Emscripten WASM Module (javabox-direct.wasm ~3.1MB)
  │     └── OpenJDK 21 Zero Interpreter (Pure C++ JVM Engine)
  │           ├── HotSpot Runtime & Garbage Collector
  │           ├── Full OpenJDK 21 Classlib (javabox-direct.data ~72MB)
  │           └── CompileServer Daemon
  │                 ├── javax.tools.JavaCompiler (In-browser compiler)
  │                 └── URLClassLoader (Dynamic classloading)
  ├── JNI Graphics & Audio Bridge
  │     ├── Framebuffer ARGB → Shared WASM Heap → Canvas 2D / WebGL
  │     └── PCM Stereo Audio → WASM Ring Buffer → Web Audio API
  └── Keyboard / Mouse Input Queue
        └── DOM Event Listener → WASM Export → JNI Native Poll
```

---

## 2. Key Advantages Over AOT Compilers (TeaVM & Bytecoder)

| Problem Area | TeaVM / Bytecoder (AOT) | OpenJDK Zero in Wasm (Method 1) |
| :--- | :--- | :--- |
| **Missing Classlib Methods** | Compiler aborts if method missing (`String.split()`, `ByteBuffer`, etc.) | **100% Complete OpenJDK 21 Class Library**. All standard methods exist. |
| **Dynamic Mod Classloading** | Closed-world requirement. Cannot synthesize or load arbitrary classes at runtime. | **Full `URLClassLoader` Support**. Unmodified mod JARs load dynamically. |
| **Java Reflection** | Requires exhaustive reflection lists (`classes.txt`, `reflect-config.json`). | **Native JVM Reflection**. Works identically to desktop Java. |
| **Bytecode Modification (ASM)** | Fails or requires compiler plugins during build time. | **Native Execution**. In-memory bytecode manipulation works out of the box. |
| **Startup / Compile Speed** | Requires multi-minute AOT compiler runs. | **Zero AOT build step**. JVM boots in 3–5 seconds and loads `.class` directly. |

---

## 3. Working Demos Included

All assets and interactive pages are served from `web/`:

### Demo 1: Interactive In-Browser Java IDE & Terminal
- **Page**: `web/editor.html`
- **What it does**: Allows writing Java code in the browser, compiling it in-process using `javax.tools.JavaCompiler`, and executing it inside the WebAssembly JVM with full `xterm.js` terminal output.

### Demo 2: Graphical Java Engine (Mocha Doom)
- **Page**: `web/doom.html`
- **What it does**: Runs a pure-Java port of the Doom engine in WebAssembly:
  - **Video**: Renders to `BufferedImage`, copied via JNI to the shared WASM heap, drawn to `<canvas>` via `requestAnimationFrame` at 30 FPS.
  - **Audio**: 8-channel software mixer producing 16-bit PCM stereo (22,050 Hz) played via Web Audio API.
  - **Input**: WASM input queue polled each frame via JNI.

---

## 4. Minecraft 1.2.5 + Forge Integration Plan

To run Minecraft 1.2.5 retro modpack inside this WebAssembly JVM runtime:

### Step 1: Filesystem Emulation (MEMFS / IDBFS)
Emscripten provides a virtual POSIX filesystem (`FS`):
- Mount `assets.pak` and game configs under `/home/player/.minecraft`.
- Mount `/home/player/.minecraft/saves` to browser **IDBFS** (IndexedDB) for persistent world saves.

### Step 2: The LWJGL 2.9.3 Bridge
Minecraft 1.2.5 communicates with the screen via LWJGL 2.9.3 (`org.lwjgl.opengl.GL11`, `Display`, `Keyboard`, `Mouse`):
- **Approach A (C-Level GL4ES)**: Compile LWJGL's native C sources (`liblwjgl.so`) with Emscripten using **GL4ES** (translates desktop OpenGL 1.1–2.1 calls directly into WebGL 1/2 calls).
- **Approach B (JNI Framebuffer)**: Map `Display.update()` to copy the framebuffer to an HTML5 canvas (similar to how Mocha Doom renders).

### Step 3: Audio Integration
Map Paulscode SoundSystem (`org.lwjgl.openal.AL10`) to Emscripten's Web Audio ring buffer.

---

## 5. Quickstart & How to Run

### Step 1: Download Runtime Assets (One-Time)
Run the automated downloader to fetch the prebuilt OpenJDK 21 Zero Wasm binary:
```bash
cd Wasm-JVM-Versions
./tools/download-runtime.sh
```

### Step 2: Launch Development Server
Modern browsers require **Cross-Origin Isolation** (`Cross-Origin-Opener-Policy: same-origin` and `Cross-Origin-Embedder-Policy: require-corp`) for `SharedArrayBuffer` and WebAssembly threads:

```bash
cd Wasm-JVM-Versions
./scripts/launch.sh 8180
```

Open in your browser:
- **Portal**: [http://localhost:8180/index.html](http://localhost:8180/index.html)
- **Java IDE**: [http://localhost:8180/editor.html](http://localhost:8180/editor.html)
- **Graphical Game (Doom)**: [http://localhost:8180/doom.html](http://localhost:8180/doom.html)
