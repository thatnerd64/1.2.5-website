// Opens the built site in headless Chromium, presses Play and prints everything the page logs.
//
//   node tools/run.cjs <dir> [seconds] [--shot=/tmp/x.png] [--headed]
//
// <dir> is a folder holding index.html, classes.js and the packs (see tools/assemble.sh).
const fs = require('fs'), path = require('path'), http = require('http');
const pw = process.env.PLAYWRIGHT_CORE || '/home/neon/dev/llama.cpp/build/tools/ui/ui-src/node_modules/playwright-core';
const { chromium } = require(pw);
const args = process.argv.slice(2);
const dir = path.resolve(args.find(a => !a.startsWith('--')));
const secs = +(args.filter(a => !a.startsWith('--'))[1] || 60);
const opt = Object.fromEntries(args.filter(a => a.startsWith('--'))
  .map(a => { const i = a.indexOf('='); return i < 0 ? [a.slice(2), undefined] : [a.slice(2, i), a.slice(i + 1)]; }));
const mime = { '.html': 'text/html', '.js': 'text/javascript', '.json': 'application/json', '.css': 'text/css' };

function findChrome() {
  if (process.env.CHROME) return process.env.CHROME;
  const base = path.join(process.env.HOME, '.cache/ms-playwright');
  for (const d of fs.readdirSync(base).filter(d => d.startsWith('chromium-'))) {
    for (const sub of ['chrome-linux64/chrome', 'chrome-linux/chrome']) {
      const p = path.join(base, d, sub); if (fs.existsSync(p)) return p;
    }
  }
}

const srv = http.createServer((q, r) => {
  const p = path.join(dir, decodeURIComponent(q.url.split('?')[0]).replace(/\/$/, '/index.html'));
  fs.readFile(p, (e, d) => {
    if (e) { r.writeHead(404); r.end(); return; }
    r.writeHead(200, { 'content-type': mime[path.extname(p)] || 'application/octet-stream' }); r.end(d);
  });
}).listen(0, async () => {
  const browser = await chromium.launch({
    executablePath: findChrome(), headless: !opt.headed,
    args: ['--use-angle=' + (process.env.ANGLE || 'swiftshader'), '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'],
  });
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });
  let waitedFor = false;
  page.on('console', m => {
    console.log('[' + m.type() + ']', m.text().slice(0, +(process.env.MAXLEN || 1500)));
    if (opt.waitfor && !waitedFor && m.text().includes(opt.waitfor)) waitedFor = true;
  });
  page.on('pageerror', e => console.log('[pageerror]', String(e.stack || e.message).slice(0, +(process.env.MAXLEN || 2500))));
  async function startExceptionCapture() {
    // --exceptions=N : print up to N distinct thrown JS exceptions (message + top frames), caught or not,
    // from the time capture starts (--excafter=<click number> delays it until that click happened)
    const c = await page.context().newCDPSession(page);
    await c.send('Debugger.enable');
    await c.send('Debugger.setPauseOnExceptions', { state: 'all' });
    let n = 0;
    const seen = new Set();
    c.on('Debugger.paused', async ev => {
      const d = ev.data && (ev.data.description || ev.data.value) || '';
      const msg = String(d).split('\n')[0].slice(0, 200);
      const where = ev.callFrames.slice(0, 10).map(f => f.functionName + ':' + f.location.lineNumber).join(' < ');
      const key = msg + where.slice(0, 120);
      if (!seen.has(key) && n < +opt.exceptions) {
        seen.add(key); n++;
        let extra = '';
        if (opt.evalvars && /TypeError/.test(msg)) {
          // what the receiver of the failing call is: the compiled constructor name of each local that is an object
          try {
            const r = await c.send('Debugger.evaluateOnCallFrame', { callFrameId: ev.callFrames[0].callFrameId, returnByValue: true,
              expression: opt.evalvars });
            extra = ' EVAL=' + JSON.stringify(r.result && (r.result.value !== undefined ? r.result.value : r.result.description));
          } catch (e) { extra = ' EVAL-ERR ' + e.message; }
        }
        console.log('EXC#' + n + ' ' + msg + ' @ ' + where + extra);
      }
      await c.send('Debugger.resume');
    });
  }
  if (opt.exceptions && !opt.excafter) await startExceptionCapture();
  let breakCdp = null;
  if (opt.break) {
    // --break=fn1,fn2 : print the call stack whenever one of these compiled functions starts (dev build only)
    breakCdp = await page.context().newCDPSession(page);
    await breakCdp.send('Debugger.enable');
    const lines = fs.readFileSync(path.join(dir, 'classes.js'), 'utf8').split('\n');
    for (const fn of opt.break.split(',')) {
      const ln = lines.findIndex(l => l.startsWith(fn + ' = ') || l.startsWith('function ' + fn + '('));
      if (ln < 0) { console.log('BREAK: no function ' + fn); continue; }
      await breakCdp.send('Debugger.setBreakpointByUrl', { lineNumber: ln + 1, urlRegex: '.*classes\\.js.*' });
      console.log('BREAK set at ' + fn + ' line ' + (ln + 1));
    }
    breakCdp.on('Debugger.paused', async ev => {
      console.log('HIT ' + ev.callFrames.slice(0, 14).map(f => f.functionName + ':' + f.location.lineNumber).join(' < '));
      await breakCdp.send('Debugger.resume');
    });
  }
  if (opt.breakat) {
    // --breakat=line:col,... (from tools/Locate.java) : print the call stack when execution reaches these positions
    const cdp = await page.context().newCDPSession(page);
    await cdp.send('Debugger.enable');
    for (const pos of opt.breakat.split(',')) {
      const [line, col] = pos.split(':').map(Number);
      // --breakcond=JS : only stop when this expression holds in the function (e.g. "arguments[1] === null")
      await cdp.send('Debugger.setBreakpointByUrl', { lineNumber: line - 1, columnNumber: col, urlRegex: '.*classes\\.js.*',
        condition: opt.breakcond || '' });
    }
    console.log('BREAKAT set: ' + opt.breakat);
    cdp.on('Debugger.paused', async ev => {
      // (one frame per line: tools/Symbolize.java maps one position per line)
      console.log('HITAT\n' + ev.callFrames.slice(0, 16).map(f => '    at (classes.js:' + (f.location.lineNumber + 1) + ':'
        + (f.location.columnNumber + 1) + ')').join('\n'));
      await cdp.send('Debugger.resume');
    });
  }
  if (opt.gltrace) {
    await page.addInitScript(() => {
      const P = WebGL2RenderingContext.prototype;
      const stat = d => { if (!d || !d.length) return 'nodata'; let ff = 0, z = 0; for (let i = 0; i < d.length; i++) { if (d[i] === 255) ff++; else if (d[i] === 0) z++; }
        return 'len=' + d.length + ' ff=' + (ff / d.length).toFixed(2) + ' zero=' + (z / d.length).toFixed(2) + ' first=' + Array.from(d.slice(0, 8)).join(','); };
      window.__glcount = {};
      for (const k of Object.getOwnPropertyNames(P)) { const d = Object.getOwnPropertyDescriptor(P, k); if (!d || typeof d.value !== 'function' || k === 'constructor') continue; const o = d.value;
        P[k] = function () { window.__glcount[k] = (window.__glcount[k] || 0) + 1; return o.apply(this, arguments); }; }
      let n = 0;
      for (const f of ['texImage2D', 'texSubImage2D']) {
        const o = P[f];
        P[f] = function () { if (n++ < 60) { const a = arguments; console.log('GL ' + f + ' level=' + a[1] + ' ' + (f === 'texImage2D' ? a[3] + 'x' + a[4] : a[4] + 'x' + a[5] + '@' + a[2] + ',' + a[3]) + ' ' + stat(a[a.length - 1])); } return o.apply(this, arguments); };
      }
    });
  }
  if (opt.fps) {
    await page.addInitScript(() => { window.__frames = 0; const raf = window.requestAnimationFrame.bind(window);
      window.requestAnimationFrame = cb => raf(t => { window.__frames++; cb(t); }); });
  }
  // --query=debuglog=Railcraft : page query string (see web/index.html)
  await page.goto(`http://localhost:${srv.address().port}/` + (opt.query ? '?' + opt.query : ''));
  await page.click('#play');
  if (opt.clicks) {
    // --clicks="x,y;x,y@seconds;..." : click, wait (default 3 s), screenshot <stepdir, default /tmp>/mc-step<N>.png.
    // --waitfor=<text>: first wait (up to --waitmax seconds, default 3600) for a console line containing the text,
    // then --boot seconds (a modded game takes a varying ~40 min to reach the title screen headless)
    let i = 0;
    if (opt.waitfor) {
      const until = Date.now() + +(opt.waitmax || 3600) * 1000;
      while (!waitedFor && Date.now() < until) await page.waitForTimeout(2000);
      console.log('WAITFOR ' + (waitedFor ? 'seen' : 'timed out') + ': ' + opt.waitfor);
    }
    await page.waitForTimeout(+(opt.boot || 40) * 1000);
    for (const step of opt.clicks.split(';')) {
      const [pos, wait] = step.split('@');
      const [x, y] = pos.split(',').map(Number);
      if (opt.excafter && opt.excafter == String(i)) await startExceptionCapture();
      if (x >= 0) { await page.mouse.move(x, y); await page.waitForTimeout(300); await page.mouse.down(); await page.waitForTimeout(150); await page.mouse.up(); }
      await page.waitForTimeout(+(wait || 3) * 1000);
      if (opt.input) console.log('INPUT', JSON.stringify(await page.evaluate(() => { const S = window.__retroInput; return { active: S.active, queue: S.mouseQueue.length, grabbed: S.grabbed, x: S.x, y: S.y, frame: S.frame, buttons: S.buttons, pending: S.pending }; })));
      await page.screenshot({ path: `${opt.stepdir || '/tmp'}/mc-step${++i}.png` });
    }
  }
  if (opt.stack) {
    // sample where the page is: pause the JS engine a few times and print the top frames
    const cdp = await page.context().newCDPSession(page);
    await cdp.send('Debugger.enable');
    for (let n = 0; n < +opt.stack; n++) {
      const paused = new Promise(r => cdp.once('Debugger.paused', r));
      await cdp.send('Debugger.pause');
      const ev = await Promise.race([paused, new Promise(r => setTimeout(() => r(null), 5000))]);
      if (ev) console.log('STACK#' + n + ': ' + ev.callFrames.slice(0, 22).map(f => f.functionName + ':' + f.location.lineNumber).join(' < '));
      await cdp.send('Debugger.resume');
      await page.waitForTimeout(700);
    }
  }
  if (opt.fps) { const f0 = await page.evaluate(() => window.__frames); await page.waitForTimeout(5000); const f1 = await page.evaluate(() => window.__frames); console.log('FPS', ((f1 - f0) / 5).toFixed(1)); }
  await page.waitForTimeout(secs * 1000);
  const state = await page.evaluate(() => ({
    loading: !document.getElementById('loading').classList.contains('hidden'),
    failure: !document.getElementById('failure').classList.contains('hidden'),
    text: document.getElementById('failure-text').textContent.slice(0, 1500),
    status: document.getElementById('status').textContent + ' / ' + document.getElementById('detail').textContent,
  }));
  console.log('STATE', JSON.stringify(state));
  if (opt.gltrace) console.log('GLCOUNT', JSON.stringify(await page.evaluate(() => window.__glcount)));
  await page.screenshot({ path: opt.shot || '/tmp/mc1710.png' });
  // --dumpfile=/home/player/.minecraft/config/x.cfg[,...] : print files the game saved (IndexedDB) as text
  if (opt.dumpfile) {
    const dumped = await page.evaluate(paths => new Promise(resolve => {
      const req = indexedDB.open('minecraft-1.7.10-ftb-infinity-evolved', 1);
      req.onerror = () => resolve({ error: String(req.error) });
      req.onsuccess = () => {
        const all = req.result.transaction('files', 'readonly').objectStore('files').getAll();
        all.onsuccess = () => {
          const out = {};
          for (const r of all.result) {
            if (paths.includes(r.path)) out[r.path] = new TextDecoder().decode(new Uint8Array(r.data));
          }
          resolve(out);
        };
      };
    }), opt.dumpfile.split(','));
    for (const [p, text] of Object.entries(dumped)) console.log('=== FILE ' + p + '\n' + text + '\n=== END ' + p);
    if (!Object.keys(dumped).length) console.log('=== FILE (none saved) ' + opt.dumpfile);
  }
  await browser.close(); srv.close();
});
