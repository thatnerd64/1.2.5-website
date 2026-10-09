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
* **Fix:** [`scripts/sanitize-classes.py`](scripts/sanitize-classes.py) dynamically audits all classes against modern JVM reflection APIs, filtering out the 61 malformed inner classes from `reflect-config.json`. This allowed GraalVM to analyze all **18,425 types** and **105,069 methods** cleanly.

### 2. AWT & System JNI Binding
Desktop Minecraft 1.2.5 relies on AWT/Swing (`java.awt.Canvas`, `java.awt.Toolkit`) for the display container before handing off rendering to LWJGL. GraalVM Native Image requires explicit JNI access declarations in [`config/jni-config.json`](config/jni-config.json) for:
- `java.lang.System.load` / `loadLibrary`
- `java.awt.GraphicsEnvironment` and `sun.awt.X11GraphicsEnvironment`
- `org.lwjgl.opengl.Display`, `GL11`, and `Sys`

### 3. Web Image vs TeaVM Shims
The base project's browser compatibility layer (`runtime/`) relies heavily on TeaVM's `@JSBody` annotation:
```java
@JSBody(params = { "message" }, script = "console.error(message);")
public static native void fatal(String message);
```
GraalVM does not interpret TeaVM's `@JSBody` bytecode intrinsic and expects standard JNI linkage. To run fully in the browser via GraalVM Web Image, these shims would need to be re-implemented against GraalVM's `@org.graalvm.webimage.api.JS` interface.

---

## Build Requirements

- **CPU:** 4+ cores recommended (tested on 88-thread Intel Xeon E5-2696 v4).
- **RAM:** Minimum 16 GB free RAM (peak RSS during 105k method compilation reaches ~8.5 GB).
- **JDK:** Oracle GraalVM 21 LTS or Oracle GraalVM 25.
- **Tools:** `build-essential`, `zlib1g-dev`, and `binaryen` (v119+ for Web Image).

---

## Building the Native Binary

1. Run the build script:
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
