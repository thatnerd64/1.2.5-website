# Bytecoder WebAssembly (WasmGC) Experimentation

This directory contains research, tooling, working prototypes, and diagnostic benchmarks evaluating **Bytecoder** as an alternative compilation engine to **TeaVM** and **GraalVM** for Minecraft 1.2.5 + Forge retro modpack.

---

## 1. Engine Overview: Bytecoder

**Bytecoder** (by Mirko Sertic) is an Ahead-of-Time (AOT) compiler for JVM bytecode targeting:
- **WebAssembly GC (`wasm`)**: Emits modern Wasm with Garbage Collection (`WasmGC`), struct references, and host JS imports.
- **JavaScript (`js`)**: Emits readable ES6 JavaScript.

### Core Architecture Differences

| Feature | TeaVM | GraalVM Native Image | Bytecoder |
| :--- | :--- | :--- | :--- |
| **Primary Target** | JavaScript / WebAssembly | Native ELF x86_64 Binary | WebAssembly GC / JS |
| **Execution Environment** | Web Browser / Discord Activity | Desktop / Linux OS | Web Browser (WasmGC) / Node.js 22+ |
| **Standard Class Library** | Custom Classlib (`org.teavm.classlib.*`) | Full OpenJDK 21 Runtime | Incomplete Port (`java.base` + `T*` shims) |
| **Native / JS Interop** | `@JSBody`, `@JSO` interfaces | JNI / GraalVM Substrate C Entrypoints | `@Import`, `@Export`, `bytecoder.web` |
| **LWJGL 2.9.3 Support** | Emulated in `runtime/` over WebGL | Real C Shared Objects (`liblwjgl64.so`) | None (Direct WebGL 1.0 API only) |
| **Dynamic Classloading** | Static tree with TeaVM plugin hacks | Build-time metadata (`reflect-config.json`) | Whole-program static points-to only |

---

## 2. Working Prototypes in This Repository

We implemented and verified two working WebAssembly applications under `Bytecoder-Versions/demos/`:

### Demo 1: `01-hello-wasm` (Verified Working)
- **Path**: `Bytecoder-Versions/demos/01-hello-wasm/`
- **Features**:
  - Compiles Java bytecode directly into a WasmGC binary (`.wasm`) and JS bootstrap (`.js`).
  - Verifies host imports: `System.currentTimeMillis()`, string manipulation, math, and `System.out.println`.
  - Runs on **Node.js v22+** via WasmGC GC types and directly in modern web browsers.
- **Run in Node.js**:
  ```bash
  cd Bytecoder-Versions/demos/01-hello-wasm
  ./build.sh
  ./run-node.js
  ```

### Demo 2: `02-webgl-demo` (Verified Working)
- **Path**: `Bytecoder-Versions/demos/02-webgl-demo/`
- **Features**:
  - Direct canvas WebGL rendering using `bytecoder.web` (`HTMLWebGLCanvasElement`, `WebGLRenderingContext`).
  - Implements `AnimationFrameCallback` to drive a 60 FPS requestAnimationFrame rendering loop inside WebAssembly.
- **Run in Browser**:
  ```bash
  cd Bytecoder-Versions/demos/02-webgl-demo
  ./build.sh
  # Start local web server:
  ../../scripts/serve-demos.sh 8088
  # Open http://localhost:8088/02-webgl-demo/index.html
  ```

---

## 3. Minecraft 1.2.5 & Forge Compilation Analysis

When compiling `build/prepared/game.jar` (Minecraft 1.2.5 + Forge 3.4.9 + 46 mods) with Bytecoder:

```bash
java -jar tools/bytecoder-cli.jar compile wasm \
    -classpath=build/prepared/game.jar \
    -mainclass=net.minecraft.client.Minecraft \
    -builddirectory=build/bytecoder-test \
    -optimizationlevel=DISABLED
```

The compilation encounters fundamental architectural hurdles:

### 1. `TString` Class Library Incompleteness
Bytecoder replaces `java.lang.String` with its own `de.mirkosertic.bytecoder.classlib.java.lang.TString` annotated with `@SubstitutesInClass(completeReplace = true)`.
Because `completeReplace = true` wipes all OpenJDK methods and `TString` only implements ~30 basic methods, calling standard Java methods causes the compiler to abort:
```
AnalysisException: java.lang.IllegalStateException: No such method : java/lang/String.split(Ljava/lang/String;)[Ljava/lang/String;
  at de.mirkosertic.bytecoder.core.ir.ResolvedClass.resolveMethod(ResolvedClass.java:113)
  at de.mirkosertic.bytecoder.core.parser.GraphParser.parse_INVOKEVIRTUAL(GraphParser.java:707)
```
Methods missing in `TString` include:
- `String.split(String)`
- `String.contains(CharSequence)`
- `String.replace(CharSequence, CharSequence)`
- `String.format(...)` (only dummy stub)
- `String.getBytes(Charset)`

### 2. AWT and Desktop Graphics Dependency Tree
Minecraft 1.2.5's entry point (`Minecraft.main`) touches `java.awt.Frame`, `Window`, `Component`, and `GraphicsEnvironment`.
Bytecoder has **no AWT class library implementation**. When it attempts to trace AWT classes, it falls back to OpenJDK's `sun.awt.PlatformGraphicsInfo`, which immediately calls missing string and system methods.
In contrast, TeaVM succeeded here because the `1.2.5-website` project includes custom `org.teavm.classlib.java.awt.*` shims written specifically for the browser.

### 3. LWJGL 2.9.3 to WebGL Shims
Minecraft 1.2.5 uses the legacy fixed-function OpenGL 1.1–1.5 pipeline:
- `GL11.glBegin()`, `glEnd()`, `glVertex3f()`, `glTexCoord2f()`
- Display lists (`glGenLists`, `glCallList`)
- Matrix stacks (`glPushMatrix`, `glPopMatrix`, `glMatrixMode`)
- `org.lwjgl.opengl.Display`, `Keyboard`, `Mouse`
- Paulscode 3D SoundSystem (`org.lwjgl.openal.AL10`)

Bytecoder has no emulation layer for LWJGL 2.x. It only exposes raw WebGL 1.0 shaders and buffers. Porting Minecraft's OpenGL 1.x pipeline to Bytecoder would require rewriting or adapting the hundreds of methods in `runtime/src/main/java/retro/gl/` to Bytecoder's `@Import` and `bytecoder.web` APIs.

### 4. Dynamic Mod Classloading & Reflection
Forge 3.4.9 uses `ModClassLoader`, reflection (`Method.invoke`, `Field.get`), and runtime class scanning to discover and load mod classes (`@Mod`).
Bytecoder requires all reachable classes to be known at compile time and does not support dynamic bytecode generation or custom classloaders in WebAssembly.

---

## 4. Summary & Recommendation

| Engine | Suitability for Minecraft 1.2.5 Full Retro | Notes |
| :--- | :--- | :--- |
| **TeaVM** | **Production (Best for Browser)** | Fully integrated with custom LWJGL shims, IndexedDB filesystem, and live Discord Activity support. |
| **GraalVM** | **Production (Best for Desktop / Native)** | Fully working native Linux x86_64 binary with C/C++ LWJGL drivers and high FPS. |
| **Bytecoder** | **Not Feasible for Legacy Minecraft** | Excellent for modern greenfield Java games and WebGL applications, but its incomplete classlib (`TString`), lack of AWT/LWJGL shims, and absence of dynamic classloading make compiling legacy Minecraft 1.2.5 infeasible without major multi-month classlib development. |

---

## 5. Directory Structure

```
Bytecoder-Versions/
├── README.md                      # Comprehensive technical analysis and benchmark report
├── tools/
│   └── bytecoder-cli.jar          # Bytecoder CLI compiler (2024-05-10 release)
├── lib/
│   ├── bytecoder.api-2024-05-10.jar
│   ├── bytecoder.web-2024-05-10.jar
│   └── bytecoder-core-2024-05-10.jar
├── demos/
│   ├── 01-hello-wasm/             # Working Hello World demo in WebAssembly GC
│   │   ├── src/retro/bytecoder/HelloWasm.java
│   │   ├── build.sh
│   │   ├── run-node.js
│   │   ├── index.html
│   │   └── dist/
│   └── 02-webgl-demo/             # Working WebGL canvas rendering demo
│       ├── src/retro/bytecoder/WebGLDemo.java
│       ├── build.sh
│       ├── index.html
│       └── dist/
└── scripts/
    ├── compile-bytecoder-wasm.sh   # General-purpose WasmGC compiler script
    ├── compile-bytecoder-js.sh     # General-purpose JS compiler script
    ├── test-minecraft-compilation.sh # Automated diagnostic test against game.jar
    └── serve-demos.sh              # Local HTTP server to preview demos in browser
```
