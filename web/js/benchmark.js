// Benchmark mode: open index.html?benchmark=SEED (tools/benchmark.js does this in a browser it starts).
//
// The launcher is skipped, the game creates a fresh world from SEED (RetroBenchmark in gameglue) and the page runs
// timed phases, recording how long every frame takes:
//   warmup  standing still while the spawn area finishes loading (not measured)
//   idle    standing still
//   walk    walking forward and weaving left and right, so new chunks load and are built
// URL options: warmup=, idle=, walk= (seconds, 0 skips a phase); scale= (render resolution, as in the launcher);
// render=far|normal|short|tiny, graphics=fancy|fast, smooth=on|off, clouds=on|off,
// difficulty=peaceful|easy|normal|hard (unset ones keep the game's defaults).
// Benchmark runs use their own saves and settings, apart from the player's. The results show on the page at the end
// and are in window.__retroBenchmark.results; tools/benchmark.js is notified of each phase through a DevTools binding.
(function () {
  'use strict';
  var params = new URLSearchParams(location.search);
  if (!params.has('benchmark')) return;

  function seconds(name, fallback) {
    var v = parseFloat(params.get(name));
    return isFinite(v) && v >= 0 ? v : fallback;
  }
  var phases = [
    { name: 'warmup', seconds: seconds('warmup', 20), walk: false, measured: false },
    { name: 'idle', seconds: seconds('idle', 30), walk: false, measured: true },
    { name: 'walk', seconds: seconds('walk', 30), walk: true, measured: true }
  ].filter(function (p) { return p.seconds > 0; });

  var config = { benchmark: params.get('benchmark') };
  if (params.has('scale')) config.resolutionScale = params.get('scale');
  ['render', 'graphics', 'smooth', 'clouds', 'difficulty'].forEach(function (k) {
    if (params.has(k)) config['benchmark.' + k] = params.get(k);
  });
  window.retroBenchmarkConfig = config;

  // Shared with the game (retro.Benchmark): it sets state, entities, chunks, x/y/z and settings, and reads walk.
  var B = window.__retroBenchmark = { state: 'loading', phase: 'loading', walk: false, results: null };

  function notify(event) {
    if (typeof window.__retroBenchmarkNotify === 'function') {
      try { window.__retroBenchmarkNotify(JSON.stringify(event)); } catch (e) { /* runner gone */ }
    }
  }

  var status = document.createElement('div');
  status.style.cssText = 'position:fixed;left:8px;bottom:8px;z-index:5;padding:4px 8px;border-radius:4px;' +
    'background:rgba(0,0,0,.6);color:#fff;font:12px/1.4 system-ui,sans-serif;pointer-events:none';
  function show(text) { status.textContent = 'Benchmark: ' + text; }
  document.body.appendChild(status);
  show('starting');

  document.getElementById('username').value = 'Benchmark';
  document.getElementById('play').click();

  var waited = setInterval(function () {
    var failure = document.getElementById('failure');
    if (!failure.classList.contains('hidden')) {
      clearInterval(waited);
      show('failed');
      notify({ type: 'error', message: document.getElementById('failure-text').textContent });
    } else if (B.state === 'ingame') {
      clearInterval(waited);
      notify({ type: 'ingame' });
      runPhase(0, {});
    } else {
      show(B.state === 'title' ? 'creating world' : 'loading');
    }
  }, 250);

  function runPhase(index, results) {
    if (index >= phases.length) {
      finish(results);
      return;
    }
    var phase = phases[index];
    B.phase = phase.name;
    B.walk = phase.walk;
    notify({ type: 'phase', phase: phase.name, seconds: phase.seconds, measured: phase.measured });
    var times = [], entities = [], startX = B.x, startZ = B.z;
    var first = null, last = null, lastSample = 0, lastShown = -1;
    function frame(t) {
      if (first === null) first = t; else times.push(t - last);
      last = t;
      if (t - lastSample >= 1000) {
        lastSample = t;
        if (B.entities != null) entities.push(B.entities);
      }
      var elapsed = Math.floor((t - first) / 1000);
      if (elapsed !== lastShown) {
        lastShown = elapsed;
        show(phase.name + ' ' + elapsed + ' / ' + phase.seconds + ' s');
      }
      if (t - first < phase.seconds * 1000) {
        requestAnimationFrame(frame);
        return;
      }
      if (phase.measured) {
        var s = frameStats(times);
        s.entities = entities.length ? Math.round(average(entities)) : null;
        s.distance = Math.round(Math.sqrt(Math.pow(B.x - startX, 2) + Math.pow(B.z - startZ, 2)));
        results[phase.name] = s;
        notify({ type: 'phaseEnd', phase: phase.name, stats: s });
      } else {
        notify({ type: 'phaseEnd', phase: phase.name });
      }
      runPhase(index + 1, results);
    }
    requestAnimationFrame(frame);
  }

  function average(values) {
    var sum = 0;
    for (var i = 0; i < values.length; i++) sum += values[i];
    return values.length ? sum / values.length : 0;
  }

  function round(v) { return Math.round(v * 100) / 100; }

  function frameStats(times) {
    var sorted = times.slice().sort(function (a, b) { return a - b; });
    var n = sorted.length;
    if (!n) return { frames: 0 };
    var total = average(sorted) * n;
    var pct = function (q) { return round(sorted[Math.min(n - 1, Math.floor(q * n))]); };
    return {
      frames: n,
      seconds: round(total / 1000),
      fps: round(n * 1000 / total),
      // FPS over the slowest 1% of frames: how bad the stutters are.
      low1: round(1000 / average(sorted.slice(n - Math.max(1, Math.round(n / 100))))),
      frameMs: { avg: round(total / n), p50: pct(0.5), p95: pct(0.95), p99: pct(0.99), max: round(sorted[n - 1]) }
    };
  }

  function gpuName() {
    var gl = window.__retroGl;
    if (!gl) return '';
    var ext = gl.getExtension('WEBGL_debug_renderer_info');
    return String(gl.getParameter(ext ? ext.UNMASKED_RENDERER_WEBGL : gl.RENDERER));
  }

  function finish(phaseResults) {
    B.walk = false;
    B.phase = 'done';
    var canvas = document.getElementById('game');
    B.results = {
      format: 1,
      date: new Date().toISOString(),
      seed: config.benchmark || 'benchmark',
      settings: B.settings,
      browser: navigator.userAgent,
      gpu: gpuName(),
      canvas: canvas.width + 'x' + canvas.height,
      devicePixelRatio: window.devicePixelRatio,
      chunks: B.chunks,
      phases: phaseResults
    };
    show('done');
    notify({ type: 'done', results: B.results });
    showResults(B.results);
  }

  function showResults(r) {
    if (document.pointerLockElement) document.exitPointerLock();
    var rows = Object.keys(r.phases).map(function (name) {
      var p = r.phases[name];
      return '<tr><td>' + name + '</td><td>' + p.fps + '</td><td>' + p.low1 + '</td><td>' + p.frameMs.p50 +
        '</td><td>' + p.frameMs.p95 + '</td><td>' + (p.entities == null ? '' : p.entities) + '</td></tr>';
    }).join('');
    var panel = document.createElement('div');
    panel.className = 'overlay';
    panel.style.background = 'rgba(0,0,0,.6)';
    panel.innerHTML = '<div class="card wide"><h1>Benchmark results</h1>' +
      '<p class="sub"></p>' +
      '<table style="width:100%;border-collapse:collapse;font-size:14px;text-align:right">' +
      '<tr style="color:var(--muted)"><td style="text-align:left">phase</td><td>avg FPS</td><td>1% low FPS</td>' +
      '<td>median ms</td><td>p95 ms</td><td>entities</td></tr>' + rows + '</table>' +
      '<p class="note"></p>' +
      '<div class="row"><button class="ghost" data-act="copy">Copy JSON</button>' +
      '<button class="ghost" data-act="close">Close</button></div></div>';
    panel.querySelector('.sub').textContent = r.gpu + ' · ' + r.canvas;
    panel.querySelector('.note').textContent = 'Seed "' + r.seed + '", ' + r.settings + '. ' + r.browser;
    Array.prototype.forEach.call(panel.querySelectorAll('td:first-child'), function (td) { td.style.textAlign = 'left'; });
    panel.addEventListener('click', function (e) {
      var act = e.target.getAttribute && e.target.getAttribute('data-act');
      if (act === 'copy' && navigator.clipboard) {
        navigator.clipboard.writeText(JSON.stringify(r, null, 2)).then(function () { e.target.textContent = 'Copied'; });
      } else if (act === 'close') {
        panel.remove();
      }
    });
    document.body.appendChild(panel);
  }
})();
