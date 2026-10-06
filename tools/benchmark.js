#!/usr/bin/env node
// Benchmarks the built game (dist/) in Chrome, Edge or Chromium on this computer and reports frame rates and,
// for a profile build, where the time goes.
//
//   ./gradlew build -PprofileBuild=true       # optional: function names in the report (same speed as a release build)
//   node tools/benchmark.js                   # about 3 minutes; results in benchmark-results/<date>/
//
// It serves dist/ itself, starts the browser with a fresh temporary profile and opens the page in benchmark mode
// (index.html?benchmark=SEED, see web/js/benchmark.js): the game creates a new world from the seed, waits while the
// spawn area loads, then measures standing still and walking around, recording a CPU profile of the page during each
// measured phase (the page waits for the profiler to start, so starting and stopping it is not measured).
//
// Writes report.txt (also printed), results.json, and <phase>.cpuprofile for each phase, which Chrome DevTools can
// open (Performance panel > Load profile) for a closer look. Run with --help for the options.
// Node.js 22+ (built-in WebSocket), no dependencies.
'use strict';

const fs = require('fs');
const http = require('http');
const os = require('os');
const path = require('path');
const { spawn, execFileSync } = require('child_process');
const report = require('./profile-report.js');

const HELP = `Usage: node tools/benchmark.js [options]

  --seed TEXT          world seed (default: benchmark)
  --warmup S           seconds standing still before measuring, while the world loads (default: 20)
  --idle S             seconds measured standing still (default: 30, 0 skips)
  --walk S             seconds measured walking, loading new chunks (default: 30, 0 skips)
  --render far|normal|short|tiny   --graphics fancy|fast   --smooth on|off   --clouds on|off
  --difficulty peaceful|easy|normal|hard
                       game settings (default: the game's own defaults for new players)
  --size WxH           page size in CSS pixels, rendered at 1x (default: 1280x720)
  --scale N            render resolution, as in the launcher (default: 1)
  --uncapped           don't cap the frame rate at the display's refresh rate, to see how far above it the game
                       gets (with software WebGL this floods the GPU process and stalls for seconds)
  --no-profile         only frame times, without the CPU profiler (which costs some speed)
  --interval US        CPU profile sampling interval in microseconds (default: 1000)
  --browser PATH       Chrome, Edge or Chromium to use (default: found automatically; also $CHROME_PATH)
  --headless           no window (usually software rendering on the CPU, so not representative)
  --software-gl        force software WebGL (SwiftShader), e.g. on a machine without a GPU
  --dist DIR           the built site (default: dist/ in this repository)
  --out DIR            where to write the results (default: benchmark-results/<date-time>/)
  --keep-open          leave the browser open at the end
  --timeout S          give up when the world is not loaded after this long (default: 900)`;

function parseArgs(argv) {
  const o = {
    seed: 'benchmark', warmup: 20, idle: 30, walk: 30, size: '1280x720', scale: null, uncapped: false, profile: true,
    interval: 1000, browser: process.env.CHROME_PATH || null, headless: false, softwareGl: false,
    dist: path.join(__dirname, '..', 'dist'), out: null, keepOpen: false, timeout: 900, settings: {},
  };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    const value = () => {
      if (i + 1 >= argv.length) throw new Error(a + ' needs a value');
      return argv[++i];
    };
    const number = () => {
      const v = Number(value());
      if (!isFinite(v) || v < 0) throw new Error(a + ' needs a number');
      return v;
    };
    switch (a) {
      case '--help': case '-h': console.log(HELP); process.exit(0); break;
      case '--seed': o.seed = value(); break;
      case '--warmup': o.warmup = number(); break;
      case '--idle': o.idle = number(); break;
      case '--walk': o.walk = number(); break;
      case '--render': case '--graphics': case '--smooth': case '--clouds': case '--difficulty':
        o.settings[a.slice(2)] = value(); break;
      case '--size': o.size = value(); break;
      case '--scale': o.scale = number(); break;
      case '--uncapped': o.uncapped = true; break;
      case '--no-profile': o.profile = false; break;
      case '--interval': o.interval = number(); break;
      case '--browser': o.browser = value(); break;
      case '--headless': o.headless = true; break;
      case '--software-gl': o.softwareGl = true; break;
      case '--dist': o.dist = path.resolve(value()); break;
      case '--out': o.out = path.resolve(value()); break;
      case '--keep-open': o.keepOpen = true; break;
      case '--timeout': o.timeout = number(); break;
      default: throw new Error('unknown option ' + a + ' (see --help)');
    }
  }
  const size = /^(\d+)x(\d+)$/.exec(o.size);
  if (!size) throw new Error('--size must look like 1280x720');
  o.width = Number(size[1]);
  o.height = Number(size[2]);
  return o;
}

// ---- Static server for dist/ ---------------------------------------------------------------------------------

const TYPES = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.json': 'application/json',
  '.css': 'text/css', '.png': 'image/png', '.ogg': 'audio/ogg', '.wav': 'audio/wav', '.mp3': 'audio/mpeg',
};

function serve(root) {
  const server = http.createServer((req, res) => {
    let rel;
    try {
      rel = decodeURIComponent(new URL(req.url, 'http://x').pathname);
    } catch (e) {
      res.writeHead(400).end();
      return;
    }
    const file = path.join(root, rel.endsWith('/') ? rel + 'index.html' : rel);
    if (!file.startsWith(root + path.sep) && file !== root) {
      res.writeHead(403).end();
      return;
    }
    fs.stat(file, (err, st) => {
      if (err || !st.isFile()) {
        res.writeHead(404).end();
        return;
      }
      res.writeHead(200, {
        'Content-Type': TYPES[path.extname(file).toLowerCase()] || 'application/octet-stream',
        'Content-Length': st.size, 'Cache-Control': 'no-store',
      });
      fs.createReadStream(file).pipe(res);
    });
  });
  return new Promise((resolve) => server.listen(0, '127.0.0.1', () => resolve(server)));
}

// ---- Browser ----------------------------------------------------------------------------------------------------

function findBrowser() {
  const candidates = [];
  if (process.platform === 'win32') {
    for (const base of [process.env.PROGRAMFILES, process.env['PROGRAMFILES(X86)'], process.env.LOCALAPPDATA]) {
      if (!base) continue;
      candidates.push(path.join(base, 'Google', 'Chrome', 'Application', 'chrome.exe'),
        path.join(base, 'Microsoft', 'Edge', 'Application', 'msedge.exe'),
        path.join(base, 'Chromium', 'Application', 'chrome.exe'),
        path.join(base, 'BraveSoftware', 'Brave-Browser', 'Application', 'brave.exe'));
    }
  } else if (process.platform === 'darwin') {
    for (const app of ['Google Chrome', 'Chromium', 'Microsoft Edge', 'Brave Browser']) {
      candidates.push(`/Applications/${app}.app/Contents/MacOS/${app}`,
        path.join(os.homedir(), `Applications/${app}.app/Contents/MacOS/${app}`));
    }
  } else {
    for (const name of ['google-chrome', 'google-chrome-stable', 'chromium', 'chromium-browser', 'microsoft-edge',
      'microsoft-edge-stable', 'brave-browser']) {
      try {
        candidates.push(execFileSync('which', [name], { stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim());
      } catch (e) { /* not installed */ }
    }
  }
  return candidates.find((c) => c && fs.existsSync(c)) || null;
}

async function launch(o, url) {
  const exe = o.browser || findBrowser();
  if (!exe) throw new Error('no Chrome, Edge or Chromium found; pass --browser PATH');
  const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'mc-benchmark-'));
  const args = [
    '--remote-debugging-port=0', `--user-data-dir=${profile}`, '--no-first-run', '--no-default-browser-check',
    '--disable-background-timer-throttling', '--disable-renderer-backgrounding',
    '--disable-backgrounding-occluded-windows', '--disable-features=CalculateNativeWinOcclusion',
    // Room for the page at its emulated size plus the browser's toolbar.
    `--window-size=${o.width + (o.headless ? 0 : 40)},${o.height + (o.headless ? 0 : 160)}`,
  ];
  if (o.uncapped) args.push('--disable-gpu-vsync', '--disable-frame-rate-limit');
  if (o.headless) args.push('--headless=new');
  if (o.softwareGl) args.push('--use-angle=swiftshader', '--enable-unsafe-swiftshader');
  if (process.platform === 'linux' && process.getuid && process.getuid() === 0) args.push('--no-sandbox');
  args.push(url);
  const child = spawn(exe, args, { stdio: 'ignore' });
  child.on('error', (e) => console.error('could not start ' + exe + ': ' + e.message));
  // With port 0 the browser picks a free port and writes it to DevToolsActivePort in its profile.
  const portFile = path.join(profile, 'DevToolsActivePort');
  for (let i = 0; i < 300 && !fs.existsSync(portFile); i++) await sleep(100);
  if (!fs.existsSync(portFile)) throw new Error(exe + ' did not open a DevTools port');
  await sleep(100);
  const [port, wsPath] = fs.readFileSync(portFile, 'utf8').split('\n');
  return { exe, child, profile, ws: `ws://127.0.0.1:${port.trim()}${wsPath.trim()}` };
}

// ---- Chrome DevTools Protocol (one WebSocket, flattened target sessions) ------------------------------------------

class Cdp {
  static connect(url) {
    if (typeof WebSocket !== 'function') throw new Error('Node.js 22 or newer is needed (built-in WebSocket)');
    const cdp = new Cdp();
    cdp.socket = new WebSocket(url);
    cdp.nextId = 1;
    cdp.pending = new Map();
    cdp.listeners = [];
    cdp.socket.onmessage = (e) => {
      const msg = JSON.parse(e.data);
      if (msg.id && cdp.pending.has(msg.id)) {
        const { resolve, reject } = cdp.pending.get(msg.id);
        cdp.pending.delete(msg.id);
        if (msg.error) reject(new Error(msg.error.message)); else resolve(msg.result);
      } else if (msg.method) {
        for (const l of cdp.listeners) l(msg);
      }
    };
    cdp.socket.onclose = () => {
      for (const { reject } of cdp.pending.values()) reject(new Error('browser closed'));
      cdp.pending.clear();
      for (const l of cdp.listeners) l({ method: 'closed' });
    };
    return new Promise((resolve, reject) => {
      cdp.socket.onopen = () => resolve(cdp);
      cdp.socket.onerror = () => reject(new Error('could not connect to the browser'));
    });
  }

  send(method, params = {}, sessionId) {
    const id = this.nextId++;
    this.socket.send(JSON.stringify(sessionId ? { id, method, params, sessionId } : { id, method, params }));
    return new Promise((resolve, reject) => this.pending.set(id, { resolve, reject }));
  }

  on(listener) {
    this.listeners.push(listener);
    return () => { this.listeners = this.listeners.filter((l) => l !== listener); };
  }
}

// ---- Run --------------------------------------------------------------------------------------------------------

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function main() {
  const o = parseArgs(process.argv.slice(2));
  if (!fs.existsSync(path.join(o.dist, 'classes.js'))) {
    throw new Error(`no game in ${o.dist}: build it first (./gradlew build -PprofileBuild=true)`);
  }
  const symbols = report.loadSymbols(o.dist);
  const stamp = new Date().toISOString().replace(/[:T]/g, '-').slice(0, 19);
  const outDir = o.out || path.join(__dirname, '..', 'benchmark-results', stamp);
  fs.mkdirSync(outDir, { recursive: true });

  const server = await serve(o.dist);
  const query = new URLSearchParams({ benchmark: o.seed, warmup: o.warmup, idle: o.idle, walk: o.walk });
  if (o.scale != null) query.set('scale', o.scale);
  for (const [k, v] of Object.entries(o.settings)) query.set(k, v);
  const pageUrl = `http://127.0.0.1:${server.address().port}/index.html?${query}`;

  const browser = await launch(o, 'about:blank');
  console.log(`Browser: ${browser.exe}`);
  console.log(symbols ? 'Profile build: the report names functions.'
    : 'Release build: frame rates and busy time only (./gradlew build -PprofileBuild=true for the breakdown).');
  const cdp = await Cdp.connect(browser.ws);
  let finished = false;
  const cleanup = async () => {
    if (o.keepOpen) {
      console.log('The browser stays open; press Ctrl+C to stop serving the game.');
      return;
    }
    await cdp.send('Browser.close').catch(() => {});
    await new Promise((r) => {
      if (browser.child.exitCode !== null) r(); else browser.child.once('exit', r);
      setTimeout(r, 5000).unref();
    });
    browser.child.kill();
    for (let i = 0; i < 10; i++) {
      try { fs.rmSync(browser.profile, { recursive: true, force: true }); break; } catch (e) { await sleep(500); }
    }
    server.close();
  };
  process.on('SIGINT', () => {
    o.keepOpen = false;
    cleanup().then(() => process.exit(130));
  });

  try {
    const { targetInfos } = await cdp.send('Target.getTargets');
    let page = targetInfos.find((t) => t.type === 'page');
    if (!page) page = { targetId: (await cdp.send('Target.createTarget', { url: 'about:blank' })).targetId };
    const { sessionId } = await cdp.send('Target.attachToTarget', { targetId: page.targetId, flatten: true });
    const send = (method, params) => cdp.send(method, params, sessionId);

    const events = [];
    let wake = null;
    cdp.on((m) => {
      if (m.method === 'closed') {
        events.push({ type: 'error', message: 'the browser was closed' });
      } else if (m.sessionId === sessionId && m.method === 'Runtime.bindingCalled' && m.params.name === '__retroBenchmarkNotify') {
        events.push(JSON.parse(m.params.payload));
      } else if (m.sessionId === sessionId && m.method === 'Runtime.exceptionThrown') {
        const d = m.params.exceptionDetails;
        console.error('page error: ' + ((d.exception && d.exception.description) || d.text));
      } else {
        return;
      }
      if (wake) wake();
    });
    const nextEvent = async (timeoutMs) => {
      const end = Date.now() + timeoutMs;
      while (!events.length) {
        const left = end - Date.now();
        if (left <= 0) return null;
        await new Promise((r) => { wake = r; setTimeout(r, Math.min(left, 2000)); });
        wake = null;
        if (!events.length) await progress();
      }
      return events.shift();
    };
    let lastState = '';
    const progress = async () => {
      const r = await send('Runtime.evaluate', {
        expression: 'window.__retroBenchmark ? __retroBenchmark.state + "|" + (document.getElementById("detail") || {}).textContent : ""',
        returnByValue: true,
      }).catch(() => null);
      const state = r && r.result && r.result.value;
      if (state && state !== lastState) {
        lastState = state;
        const [s, detail] = state.split('|');
        if (s === 'title') console.log('  creating the world');
        else if (s === 'loading' && detail) console.log('  loading: ' + detail);
      }
    };

    await send('Runtime.enable');
    await send('Runtime.addBinding', { name: '__retroBenchmarkNotify' });
    await send('Emulation.setDeviceMetricsOverride', { width: o.width, height: o.height, deviceScaleFactor: 1, mobile: false });
    if (o.profile) {
      await send('Profiler.enable');
      await send('Profiler.setSamplingInterval', { interval: o.interval });
    }
    console.log(`Loading the game (${o.width}x${o.height}, seed "${o.seed}")...`);
    await send('Page.navigate', { url: pageUrl });

    const results = { phases: {}, profiles: {} };
    const go = (phase) => send('Runtime.evaluate', { expression: `window.__retroBenchmark.go = ${JSON.stringify(phase)}` });
    let inGame = false;
    // Loading may take up to --timeout; once in game, the phases take as long as they were asked to.
    let deadline = Date.now() + o.timeout * 1000;
    for (;;) {
      const ev = await nextEvent(Math.max(1000, deadline - Date.now()));
      if (!ev) throw new Error(inGame ? 'the page stopped reporting' : `the world did not load within ${o.timeout} s`);
      if (ev.type === 'error') throw new Error('the game failed: ' + ev.message);
      if (ev.type === 'ingame') {
        console.log('  in game');
        inGame = true;
        deadline = Date.now() + 2 * 1000 * (o.warmup + o.idle + o.walk) + 120000;
      }
      if (ev.type === 'phase') {
        console.log(`  ${ev.phase}: ${ev.seconds} s` + (ev.measured ? '' : ' (not measured)'));
        if (ev.measured && o.profile) await send('Profiler.start');
        await go(ev.phase);
      }
      if (ev.type === 'phaseEnd' && ev.stats) {
        const phase = { ...ev.stats };
        if (o.profile) {
          const { profile } = await send('Profiler.stop');
          const file = path.join(outDir, ev.phase + '.cpuprofile');
          fs.writeFileSync(file, JSON.stringify(profile));
          const analysis = report.analyze(profile, symbols);
          results.profiles[ev.phase] = analysis;
          phase.mainThreadBusy = analysis.busyPercent;
          phase.inWebGL = analysis.webglPercent;
        }
        results.phases[ev.phase] = phase;
        console.log(`  ${ev.phase}: ${phase.fps} FPS`);
      }
      if (ev.type === 'done') {
        results.page = ev.results;
        break;
      }
    }
    finished = true;

    const text = formatReport(o, browser, symbols, results);
    fs.writeFileSync(path.join(outDir, 'report.txt'), text + '\n');
    fs.writeFileSync(path.join(outDir, 'results.json'), JSON.stringify({
      options: { seed: o.seed, warmup: o.warmup, idle: o.idle, walk: o.walk, size: o.size, scale: o.scale,
        uncapped: o.uncapped, profile: o.profile, interval: o.interval, headless: o.headless, softwareGl: o.softwareGl,
        settings: o.settings },
      environment: { ...results.page, phases: undefined, os: `${os.type()} ${os.release()} ${os.arch()}`,
        cpu: os.cpus()[0] && os.cpus()[0].model, cores: os.cpus().length, memoryGB: Math.round(os.totalmem() / 2 ** 30) },
      phases: results.phases,
      profiles: results.profiles,
    }, null, 2) + '\n');
    console.log('\n' + text + '\n');
    console.log('Results: ' + outDir);
  } finally {
    if (!finished) console.error('Benchmark did not finish.');
    await cleanup();
  }
}

function formatReport(o, browser, symbols, r) {
  const p = r.page;
  const lines = [];
  const version = /(Chrome|Edg|Chromium)\/[\d.]+/.exec(p.browser || '');
  lines.push(`Minecraft 1.2.5 (Full Retro) browser benchmark, ${p.date.slice(0, 16).replace('T', ' ')} UTC`);
  lines.push(`GPU:       ${p.gpu}`);
  lines.push(`Computer:  ${os.cpus()[0] ? os.cpus()[0].model.trim() : '?'}, ${os.cpus().length} threads, ` +
    `${Math.round(os.totalmem() / 2 ** 30)} GB, ${os.type()} ${os.release()}`);
  lines.push(`Browser:   ${version ? version[0] : path.basename(browser.exe)}` +
    `${o.headless ? ', headless' : ''}${o.softwareGl ? ', software WebGL' : ''}` +
    `, frame rate ${o.uncapped ? 'uncapped' : 'capped at the display refresh rate'}`);
  lines.push(`Canvas:    ${p.canvas} (page ${o.width}x${o.height} at 1x${o.scale != null ? ', render resolution ' + o.scale : ''})`);
  lines.push(`World:     seed "${p.seed}", ${p.settings}`);
  lines.push(`Build:     ${symbols ? 'profile build (function names)' : 'release build (no function names)'}` +
    (o.profile ? `; CPU profiler sampling every ${o.interval} µs during the measured phases` : ''));
  lines.push('');
  const header = ['phase', 'avg FPS', '1% low', 'median ms', 'p95 ms', 'p99 ms', 'entities', 'walked m'];
  if (o.profile) header.push('main busy', 'in WebGL');
  const rows = [header];
  for (const [name, s] of Object.entries(r.phases)) {
    const row = [name, s.fps, s.low1, s.frameMs.p50, s.frameMs.p95, s.frameMs.p99, s.entities ?? '', s.distance ?? ''];
    if (o.profile) row.push(s.mainThreadBusy + '%', s.inWebGL + '%');
    rows.push(row.map(String));
  }
  const widths = header.map((_, i) => Math.max(...rows.map((row) => row[i].length)));
  for (const row of rows) lines.push(row.map((c, i) => (i ? c.padStart(widths[i] + 2) : c.padEnd(widths[i]))).join(''));
  if (o.profile) {
    lines.push('');
    lines.push('main busy: share of the time the page\'s main thread (where the game runs) was working rather than waiting');
    lines.push('for the next frame. Near 100%: the game\'s code limits the frame rate. Well below 100% at the display\'s');
    lines.push('refresh rate: headroom. Well below it at a lower rate: the GPU limits it. in WebGL: share of the busy');
    lines.push('time inside WebGL calls, which includes waiting for the GPU.');
  }
  for (const [name, a] of Object.entries(r.profiles)) {
    lines.push('');
    lines.push(`==== ${name} ====`);
    lines.push(report.format(a, 20));
  }
  return lines.join('\n');
}

main().catch((e) => {
  console.error('benchmark: ' + e.message);
  process.exit(1);
});
