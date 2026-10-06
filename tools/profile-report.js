#!/usr/bin/env node
// Summarises a Chrome CPU profile (.cpuprofile) of the game: where the main thread's time goes, by game phase
// (entity updates, chunk building, saving...), by code layer (game code, Java class library, TeaVM runtime,
// GL emulation) and by function, with Minecraft (MCP) method names.
//
//   node tools/profile-report.js PROFILE.cpuprofile [--dist dist] [--top 25]
//
// Names need a profile build (./gradlew build -PprofileBuild=true), which writes dist/profile-symbols.json;
// other builds only show busy/idle/GC time. tools/benchmark.js records profiles and uses this to report on them.
// Node.js 18+, no dependencies.
'use strict';

const fs = require('fs');
const path = require('path');

/** Loads dist/profile-symbols.json (and classes.js, to label TeaVM's runtime helpers); null if missing. */
function loadSymbols(dist) {
  const file = path.join(dist, 'profile-symbols.json');
  if (!fs.existsSync(file)) return null;
  const s = JSON.parse(fs.readFileSync(file, 'utf8'));
  let lines = null;
  try {
    lines = fs.readFileSync(path.join(dist, 'classes.js'), 'utf8').split('\n');
  } catch (e) { /* labels without the source */ }
  return { names: s.names, ranges: s.ranges, mcp: s.mcp, lines };
}

/** The Java method compiled to the JavaScript function starting at (line, column), or null. */
function methodAt(symbols, line, column) {
  const r = symbols.ranges;
  const n = r.length / 3;
  // The last range starting at or before (line, column).
  let lo = 0;
  let hi = n - 1;
  let found = -1;
  while (lo <= hi) {
    const mid = (lo + hi) >> 1;
    if (r[mid * 3] < line || (r[mid * 3] === line && r[mid * 3 + 1] <= column)) {
      found = mid;
      lo = mid + 1;
    } else {
      hi = mid - 1;
    }
  }
  if (found >= 0 && r[found * 3 + 2] >= 0) return symbols.names[r[found * 3 + 2]];
  // A function's first characters can lie just before its method's range: then it is the next one, close by.
  for (let i = found + 1; i < n && r[i * 3] <= line + 3; i++) {
    if (r[i * 3 + 2] >= 0) return symbols.names[r[i * 3 + 2]];
  }
  return null;
}

function label(frame, symbols) {
  const fn = frame.functionName || '(anonymous)';
  if (!frame.url.endsWith('classes.js')) {
    if (fn.startsWith('(')) return fn;
    return fn + (frame.url ? ' [' + path.basename(frame.url) + ']' : ' [native]');
  }
  if (fn.startsWith('$rt_') || fn.startsWith('Long_')) return fn + ' [teavm]';
  const lead = symbols && symbols.lines && /^\s*(\$rt_[A-Za-z0-9_]+) =/.exec(symbols.lines[frame.lineNumber] || '');
  if (fn === '(anonymous)' && lead) return lead[1] + ' [teavm]';
  const m = symbols ? methodAt(symbols, frame.lineNumber, frame.columnNumber) : null;
  if (m && !fn.startsWith('$')) return m;
  return fn + ' [js]';
}

// Frames that only pass a call on (virtual-call trampolines, thread start-up); they count where they are the leaf.
const TRANSPARENT = /^(\$rt_wrapFunction\d|\$rt_threadStarter|\$rt_callWithReceiver|\(anonymous\) \[js\])/;

// [name, frames]: a sample belongs to the first entry with one of its frames on the stack (an empty list: the rest).
const TICK = [
  ['Entity AI (EntityAITasks, pathfinding)', ['EntityAITasks.onUpdateTasks', 'PathFinder.createEntityPathTo',
    'EntityLiving.updateEntityActionState', 'EntityLiving.updateAITasks']],
  ['Entity movement & collision (moveEntity)', ['Entity.moveEntity', 'EntityLiving.moveEntityWithHeading']],
  ['Entity updates: other', ['World.updateEntities']],
  ['Animated textures (water, lava, fire, mod FX)', ['RenderEngine.updateDynamicTextures']],
  ['Random block ticks (leaf decay, crops...)', ['World.tickBlocksAndAmbiance']],
  ['Mob spawning', ['SpawnerAnimals.findChunksForSpawning']],
  ['Lighting updates', ['World.updatingLighting', 'World.updateLightByType', 'World.updateAllLightTypes']],
  ['World tick: other (autosave, weather...)', ['World.tick']],
  ['Mod tick handlers (FML)', ['cpw.mods.fml.common.FMLCommonHandler.tickStart',
    'cpw.mods.fml.common.FMLCommonHandler.tickEnd']],
  ['Particle updates', ['EffectRenderer.updateEffects']],
  ['Tick: other', []],
];
const RENDER = [
  ['Chunk mesh building (updateRenderers)', ['RenderGlobal.updateRenderers']],
  ['Terrain drawing (sortAndRender)', ['RenderGlobal.sortAndRender']],
  ['Entity & tile entity rendering', ['RenderGlobal.renderEntities']],
  ['HUD / GUI overlay', ['GuiIngame.renderGameOverlay']],
  ['Hand / held item', ['EntityRenderer.renderHand']],
  ['Particles', ['EffectRenderer.renderParticles', 'EffectRenderer.renderLitParticles']],
  ['Sky / clouds', ['RenderGlobal.renderSky', 'RenderGlobal.renderClouds']],
  ['Render: other', []],
];
const TOP = [
  ['Idle (waiting for the next frame)', ['(idle)']],
  ['Garbage collection', ['(garbage collector)']],
  ['Browser internals (program)', ['(program)']],
  ['Chunk saving thread (NBT + zlib)', ['ThreadedFileIOBase.run']],
  ['Game tick (Minecraft.runTick)', ['Minecraft.runTick'], TICK],
  ['World rendering (updateCameraAndRender)', ['EntityRenderer.updateCameraAndRender'], RENDER],
  ['Mod render-tick handlers (minimap...)', ['cpw.mods.fml.client.FMLClientHandler.onRenderTickEnd',
    'cpw.mods.fml.client.FMLClientHandler.onRenderTickStart']],
  ['World loading (startWorld)', ['Minecraft.startWorld']],
  ['Frame loop: other', ['Minecraft.runGameLoop']],
  ['Other', []],
];

function pick(rules, onStack) {
  for (const rule of rules) {
    if (!rule[1].length || rule[1].some((f) => onStack.has(f))) return rule;
  }
  return null;
}

function layer(leaf) {
  if (leaf.startsWith('(')) return null;
  if (leaf.endsWith('[teavm]') || leaf.endsWith('[js]') || leaf.startsWith('org.teavm.')) {
    return 'TeaVM runtime (virtual calls, threads, arrays, longs)';
  }
  if (leaf.startsWith('retro.gl.') || leaf.endsWith('[native]')) return 'GL emulation + WebGL / browser calls';
  if (leaf.startsWith('com.jcraft.jzlib') || leaf.startsWith('java.util.zip')) return 'zlib compression (jzlib)';
  if (leaf.startsWith('java.') || leaf.startsWith('javax.')) return 'Java class library (collections, Random...)';
  if (leaf.startsWith('retro.')) return 'Browser runtime (retro.*)';
  return 'Game + mod code';
}

function runtimeGroup(leaf) {
  if (/^\$rt_wrapFunction\d/.test(leaf)) return 'Virtual-call trampolines ($rt_wrapFunctionN)';
  if (/^\$rt_(suspending|resuming|nativeThread|invalidPointer)|^TeaVMThread|\$_asyncCall_\$|^org\.teavm\.platform/.test(leaf)) {
    return 'Thread emulation (suspend/resume checks)';
  }
  if (/^\$rt_(createArray|createByteArray|createIntArray|createFloatArray|createDoubleArray|createLongArray|createShortArray|createCharArray|createBooleanArray|createMultiArray|arraycls)|^JavaArray/.test(leaf)) {
    return 'Array allocation and copies';
  }
  if (/^Long_/.test(leaf)) return 'long arithmetic emulation';
  if (/fillNativeException|^JavaError/.test(leaf)) return 'Exception stack capture';
  return 'Other runtime helpers';
}

/** Analyses one profile; percentages are of the main thread's busy (non-idle) time. */
function analyze(profile, symbols) {
  const nodes = new Map(profile.nodes.map((n) => [n.id, n]));
  const parent = new Map();
  for (const n of profile.nodes) for (const c of n.children || []) parent.set(c, n.id);
  const labels = new Map();
  for (const n of profile.nodes) labels.set(n.id, label(n.callFrame, symbols));
  const stacks = new Map();
  const stackOf = (id) => {
    let s = stacks.get(id);
    if (s) return s;
    const frames = [];
    for (let at = id; parent.has(at); at = parent.get(at)) frames.push(labels.get(at));
    frames.reverse();
    s = frames.filter((f, i) => i === frames.length - 1 || !TRANSPARENT.test(f));
    stacks.set(id, s);
    return s;
  };

  const top = new Map();
  const sub = new Map();
  const layers = new Map();
  const runtime = new Map();
  const self = new Map();
  const add = (map, key, t) => map.set(key, (map.get(key) || 0) + t);
  let total = 0;
  let idle = 0;
  const { samples, timeDeltas } = profile;
  for (let i = 0; i < samples.length; i++) {
    const t = i + 1 < timeDeltas.length ? timeDeltas[i + 1] : 0;
    total += t;
    const st = stackOf(samples[i]);
    const leaf = st.length ? st[st.length - 1] : '(root)';
    if (leaf === '(idle)') { idle += t; continue; }
    const onStack = new Set(st);
    const rule = pick(TOP, onStack);
    add(top, rule[0], t);
    if (rule[2]) add(sub, rule[0] + '\u0000' + pick(rule[2], onStack)[0], t);
    add(self, leaf, t);
    const l = layer(leaf);
    if (l) add(layers, l, t);
    if (l && l.startsWith('TeaVM')) add(runtime, runtimeGroup(leaf), t);
  }
  const busy = total - idle;
  const pct = (t) => (busy ? Math.round((1000 * t) / busy) / 10 : 0);
  const sorted = (map) => [...map].sort((a, b) => b[1] - a[1]);
  return {
    seconds: Math.round(total / 1e4) / 100,
    busyPercent: total ? Math.round((1000 * busy) / total) / 10 : 0,
    named: !!symbols,
    phases: sorted(top).map(([name, t]) => ({
      name,
      percent: pct(t),
      parts: sorted(new Map([...sub].filter(([k]) => k.startsWith(name + '\u0000'))))
        .map(([k, v]) => ({ name: k.split('\u0000')[1], percent: pct(v) })),
    })),
    layers: sorted(layers).map(([name, t]) => ({ name, percent: pct(t) })),
    teavmRuntime: sorted(runtime).map(([name, t]) => ({ name, percent: pct(t) })),
    functions: sorted(self).slice(0, 60).map(([name, t]) => ({ name, percent: pct(t) })),
  };
}

function format(a, top = 25) {
  const out = [];
  const row = (indent, name, value) => out.push((' '.repeat(indent) + name).padEnd(58) + (value.toFixed(1) + '%').padStart(7));
  out.push(`Main thread busy ${a.busyPercent}% of ${a.seconds} s (the rest is waiting for the next frame).`);
  if (!a.named) {
    out.push('No function names: this is not a profile build (./gradlew build -PprofileBuild=true).');
  }
  out.push('');
  out.push('Where the busy time goes'.padEnd(58) + '% busy'.padStart(7));
  for (const p of a.phases) {
    row(2, p.name, p.percent);
    for (const s of p.parts) if (s.percent >= 0.1) row(6, s.name, s.percent);
  }
  out.push('');
  out.push('By code layer (self time)'.padEnd(58) + '% busy'.padStart(7));
  for (const l of a.layers) row(2, l.name, l.percent);
  if (a.teavmRuntime.length) {
    out.push('');
    out.push('TeaVM runtime overhead (self time)'.padEnd(58) + '% busy'.padStart(7));
    for (const l of a.teavmRuntime) row(2, l.name, l.percent);
  }
  out.push('');
  out.push('Hottest functions (self time)'.padEnd(58) + '% busy'.padStart(7));
  for (const f of a.functions.slice(0, top)) row(2, f.name.length > 54 ? f.name.slice(0, 53) + '…' : f.name, f.percent);
  return out.join('\n');
}

module.exports = { loadSymbols, analyze, format };

if (require.main === module) {
  const args = process.argv.slice(2);
  const opt = (name, fallback) => (args.includes(name) ? args[args.indexOf(name) + 1] : fallback);
  const file = args.find((a, i) => !a.startsWith('--') && !(i > 0 && args[i - 1].startsWith('--')));
  if (!file) {
    console.error('usage: node tools/profile-report.js PROFILE.cpuprofile [--dist dist] [--top 25]');
    process.exit(2);
  }
  const dist = opt('--dist', path.join(__dirname, '..', 'dist'));
  const symbols = loadSymbols(dist);
  console.log(format(analyze(JSON.parse(fs.readFileSync(file, 'utf8')), symbols), Number(opt('--top', 25))));
}
