# GraalVM Compilation for Minecraft 1.2.5 Full Retro

This directory contains the tooling, configurations, and scripts for compiling **Minecraft 1.2.5 + Forge 3.4.9 + the Full Retro modpack (46 mods)** using **GraalVM** Ahead-Of-Time (AOT) compilation instead of TeaVM.

---

## Background & Architecture

The base project ([`1.2.5-website`](../README.md)) compiles Minecraft 1.2.5 bytecode into JavaScript via **TeaVM** to run in a web browser using WebGL 2.

GraalVM offers two distinct AOT compilation avenues:
1. **GraalVM Native Image (Standalone Binary):** Compiles the game and modpack bytecode directly into a native Linux x86_64 ELF executable (`mc-retro-native`).
2. **GraalVM Web Image (`--tool:svm-wasm`):** An experimental feature in Oracle GraalVM 25+ that compiles JVM bytecode into a WebAssembly (WasmGC) module paired with a JavaScript wrapper.

| Feature | Base Project (TeaVM) | GraalVM Web Image (Wasm) | GraalVM Native Image |
| :--- | :--- | :--- | :--- |
| **Target Output** | `classes.js` (JavaScript) | `mc-wasm.js.wasm` (WasmGC) | Standalone ELF binary (`mc-retro-native`) |
| **Browser Execution** | Native WebGL 2 + Web Audio | Requires `@JS` API shims | Standalone desktop (X11 / OpenGL) |
| **Compilation Method** | ASM AST + TeaVM compiler | SubstrateVM + Binaryen (`wasm-as`) | SubstrateVM + Graal compiler IR |
| **Peak Build RAM** | ~11–14 GB | ~2–4 GB | ~8.5 GB |

---

## Directory Layout

```
GraalVM-Versions/
├── README.md                     # This documentation
├── config/
│   ├── reflect-config.json       # Reflection metadata for 7,895 clean modpack classes
│   ├── jni-config.json           # JNI bindings for AWT, X11, System, and LWJGL
│   └── resource-config.json      # Inclusions for assets, textures, sounds, and configs
└── scripts/
    ├── build-native.sh           # One-step compilation to standalone ELF native executable
    ├── build-wasm.sh             # Experimental Web Image Wasm compilation script
    ├── run-native.sh             # Runner configuring LD_LIBRARY_PATH and natives
    └── sanitize-classes.py       # Scanner & filter for broken 2012 legacy inner-class bytecode
```

---

## Technical Challenges & Fixes

### 1. Legacy 2012 Obfuscated Inner-Class Attribute Bug
During GraalVM Native Image points-to static analysis across the 7,950 modpack classes, modern OpenJDK (Java 21/25) reflection threw:
```
java.lang.InternalError: Enclosing constructor not found
    at java.base/java.lang.Class.getEnclosingConstructor(Class.java:1594)
    at java.base/sun.reflect.generics.scope.ClassScope.computeEnclosingScope(ClassScope.java:56)
    at org.graalvm.nativeimage.builder/.../ReflectionDataBuilder.queryGenericInfo(...)
```
* **Cause:** Minecraft 1.2.5 and 2012 Forge mods were obfuscated using legacy ProGuard/Retroguard pipelines that altered or stripped outer constructors without updating the `EnclosingMethod` bytecode attributes of anonymous inner classes (e.g. in `Futures$CombinedFuture$1`, `ic2.common.TileEntityReactorChamber$1`).
* **Fix:** [`scripts/sanitize-classes.py`](scripts/sanitize-classes.py) dynamically audits all classes against modern JVM reflection APIs, filtering out broken inner classes from `reflect-config.json`. This allowed GraalVM to analyze all **20,370 types** and **147,000+ methods** cleanly.

### 2. Desktop Compatibility Runtime (`runtime-compat/`)
The base web project implements a virtual in-memory filesystem and WebGL bridge. For standalone native execution, we built a dedicated pure-JVM desktop compatibility runtime in `GraalVM-Versions/runtime-compat/`:
- `retro.Runtime`: Binds the Minecraft data directory (`.minecraft`), sets `java.home` and native library paths, and mounts game archives.
- `retro.rt.Pak`: Pure-JVM binary reader for the RPK2 format used by `assets.pak`, enabling zero-overhead direct `FileChannel` access to game textures, sound effects, and terrain maps without requiring browser virtual filesystem shims.
- `retro.desktop.DesktopMain`: Clean entrypoint that initializes the runtime before invoking `net.minecraft.client.Minecraft.main()`.

### 3. LWJGL 2.9.3 `libjawt.so` ELF Version Mismatch
When `Minecraft.run()` initializes the display, LWJGL 2.9.3 calls `System.load("liblwjgl64.so")`. On modern Linux with Java 21, `dlopen` failed with:
```
OSError: .../libjawt.so: version `SUNWprivate_1.1' not found (required by .../liblwjgl64.so)
```
* **Cause:** Precompiled `liblwjgl64.so` binaries from 2015 were linked against Java 7/8's `libjawt.so`, which exported `JAWT_GetAWT` under the legacy ELF symbol version `SUNWprivate_1.1`. Modern GraalVM/OpenJDK 21 exports `JAWT_GetAWT` with the base version tag.
* **Fix:** [`build-native.sh`](scripts/build-native.sh) compiles a forwarding shim shared library with a GNU linker version script defining `SUNWprivate_1.1 { global: JAWT_GetAWT; };`, resolving the symbol requirement and forwarding dynamically to Java's `libjawt.so`.

### 4. Snoop URL Protocol Handler
Minecraft 1.2.5 initializes its client snoop telemetry reporter (`vt`) during startup with `new URL("http://snoop.minecraft.net/client")`. In GraalVM Native Image, URL stream handlers are stripped by default unless explicitly retained.
* **Fix:** Enabled via `--enable-url-protocols=http,https` in native-image compilation flags.

### 5. Modpack Discovery & Class Loading
Forge Mod Loader (FML) discovers mods by inspecting the `mods/` folder and instantiating `mod_*.class` via `cpw.mods.fml.common.ModClassLoader`.
* Our desktop runtime links `:gameglue:jar` into the native image classpath, which delegates `ModClassLoader.loadClass()` directly to `Class.forName()`.
* All 46 mods from `fs.pak` are unpacked into `.minecraft/mods`, and `retro.Runtime.boot()` binds `Minecraft.aj` via reflection so FML reliably detects the modpack directory.
* All 8,818 classes across the modpack, LWJGL, JInput, and AWT are registered in `reflect-config.json` and `jni-config.json`.

---

## Build Requirements

- **CPU:** 4+ cores recommended (tested on 88-thread Intel Xeon E5-2696 v4).
- **RAM:** Minimum 16 GB free RAM (peak RSS during compilation reaches ~9.5 GB).
- **JDK:** Oracle GraalVM 21 LTS (`native-image` installed).
- **Tools:** `build-essential`, `gcc`, `patchelf`, `xvfb` (for headless display testing).

---

## Building the Native Binary

1. Run the end-to-end build script:
   ```bash
   ./GraalVM-Versions/scripts/build-native.sh
   ```
2. The standalone binary is produced at:
   ```
   build/graalvm/mc-retro-native
   ```
3. Run the game:
   ```bash
   ./GraalVM-Versions/scripts/run-native.sh
   ```
   (Or under headless X11: `xvfb-run -a ./GraalVM-Versions/scripts/run-native.sh`)
