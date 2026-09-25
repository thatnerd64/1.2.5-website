// OpenAL (the subset used by paulscode SoundSystem / LWJGL) on top of the Web Audio API.
// Called from org.lwjgl.openal.AL10 through window.RetroAL.
(function () {
  'use strict';
  var AL_PLAYING = 0x1012, AL_PAUSED = 0x1013, AL_STOPPED = 0x1014, AL_INITIAL = 0x1011;
  var ctx = null, master = null;
  var buffers = [null];
  var sources = [null];
  var listener = { pos: [0, 0, 0], at: [0, 0, -1], up: [0, 1, 0] };

  function context() {
    if (!ctx) {
      var C = window.AudioContext || window.webkitAudioContext;
      if (!C) return null;
      ctx = new C();
      master = ctx.createGain();
      master.connect(ctx.destination);
    }
    return ctx;
  }

  window.__retroAudioResume = function () {
    var c = context();
    if (c && c.state === 'suspended') c.resume();
  };
  ['keydown', 'mousedown', 'touchstart', 'pointerdown'].forEach(function (ev) {
    window.addEventListener(ev, window.__retroAudioResume, { capture: true });
  });

  function setParam(param, value) {
    if (!param) return;
    try { param.setValueAtTime(value, ctx.currentTime); } catch (e) { param.value = value; }
  }

  function updatePosition(s) {
    var p = s.pos;
    if (s.relative) p = [p[0] + listener.pos[0], p[1] + listener.pos[1], p[2] + listener.pos[2]];
    var pn = s.panner;
    if (pn.positionX) { setParam(pn.positionX, p[0]); setParam(pn.positionY, p[1]); setParam(pn.positionZ, p[2]); }
    else pn.setPosition(p[0], p[1], p[2]);
  }

  function stopNodes(s) {
    for (var i = 0; i < s.nodes.length; i++) {
      var n = s.nodes[i];
      n.onended = null;
      try { n.stop(); } catch (e) {}
      try { n.disconnect(); } catch (e) {}
    }
    s.nodes = [];
  }

  function makeNode(s, audio, loop) {
    var n = ctx.createBufferSource();
    n.buffer = audio;
    n.loop = loop;
    n.playbackRate.value = s.pitch;
    n.connect(s.gain);
    s.nodes.push(n);
    return n;
  }

  // Schedules queued (streaming) buffers that have not been scheduled yet.
  function scheduleQueue(s) {
    var now = ctx.currentTime;
    if (s.queueEnd < now) s.queueEnd = now + 0.02;
    for (var i = 0; i < s.queue.length; i++) {
      var q = s.queue[i];
      if (q.end !== undefined) continue;
      var b = buffers[q.id];
      var audio = b && b.audio;
      if (!audio) { q.start = s.queueEnd; q.end = s.queueEnd; continue; }
      var n = makeNode(s, audio, false);
      n.start(s.queueEnd);
      q.start = s.queueEnd;
      q.end = s.queueEnd + audio.duration / s.pitch;
      s.queueEnd = q.end;
      (function (node) {
        node.onended = function () {
          var k = s.nodes.indexOf(node);
          if (k >= 0) s.nodes.splice(k, 1);
          try { node.disconnect(); } catch (e) {}
        };
      })(n);
    }
  }

  var AL = window.RetroAL = {
    available: function () { return !!context(); },

    genBuffer: function () { buffers.push({ audio: null }); return buffers.length - 1; },
    deleteBuffer: function (id) { if (id > 0) buffers[id] = null; },
    bufferData: function (id, format, bytes, freq) {
      var c = context();
      var b = buffers[id];
      if (!c || !b) return;
      var stereo = format === 0x1102 || format === 0x1103;
      var bits16 = format === 0x1101 || format === 0x1103;
      var channels = stereo ? 2 : 1;
      var frames = Math.floor(bytes.byteLength / (bits16 ? 2 : 1) / channels);
      if (frames <= 0) { b.audio = null; return; }
      var rate = Math.max(3000, Math.min(768000, freq));
      var audio = c.createBuffer(channels, frames, rate);
      var dv = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
      for (var ch = 0; ch < channels; ch++) {
        var out = audio.getChannelData(ch);
        var i;
        if (bits16) for (i = 0; i < frames; i++) out[i] = dv.getInt16((i * channels + ch) * 2, true) / 32768;
        else for (i = 0; i < frames; i++) out[i] = (bytes[i * channels + ch] - 128) / 128;
      }
      b.audio = audio;
    },

    genSource: function () {
      var c = context();
      if (!c) { sources.push(null); return sources.length - 1; }
      var gain = c.createGain();
      var panner = c.createPanner();
      panner.panningModel = 'equalpower';
      panner.distanceModel = 'inverse';
      panner.refDistance = 1;
      panner.rolloffFactor = 1;
      panner.maxDistance = 10000;
      gain.connect(panner);
      panner.connect(master);
      sources.push({ gain: gain, panner: panner, nodes: [], queue: [], queueEnd: 0, buffer: 0, state: AL_INITIAL,
        looping: false, pitch: 1, pos: [0, 0, 0], relative: false, streaming: false, startTime: 0, offset: 0 });
      return sources.length - 1;
    },
    deleteSource: function (id) {
      var s = sources[id];
      if (!s) return;
      stopNodes(s);
      try { s.panner.disconnect(); } catch (e) {}
      sources[id] = null;
    },

    play: function (id) {
      var s = sources[id];
      if (!s || !ctx) return;
      if (s.streaming) {
        if (s.state === AL_PLAYING) return;
        s.state = AL_PLAYING;
        s.queueEnd = ctx.currentTime;
        for (var i = 0; i < s.queue.length; i++) if (s.queue[i].end !== undefined && s.queue[i].end > ctx.currentTime) {
          s.queue[i].end = undefined;
        }
        scheduleQueue(s);
        return;
      }
      var b = buffers[s.buffer];
      stopNodes(s);
      if (!b || !b.audio) { s.state = AL_STOPPED; return; }
      var n = makeNode(s, b.audio, s.looping);
      var offset = s.state === AL_PAUSED ? s.offset : 0;
      n.onended = function () {
        var k = s.nodes.indexOf(n);
        if (k >= 0) s.nodes.splice(k, 1);
        if (s.nodes.length === 0 && s.state === AL_PLAYING) s.state = AL_STOPPED;
      };
      n.start(0, offset % b.audio.duration);
      s.startTime = ctx.currentTime - offset;
      s.state = AL_PLAYING;
    },
    pause: function (id) {
      var s = sources[id];
      if (!s || s.state !== AL_PLAYING) return;
      s.offset = (ctx.currentTime - s.startTime) * s.pitch;
      stopNodes(s);
      if (s.streaming) for (var i = 0; i < s.queue.length; i++) {
        if (s.queue[i].end !== undefined && s.queue[i].end > ctx.currentTime) s.queue[i].end = undefined;
      }
      s.state = AL_PAUSED;
    },
    stop: function (id) {
      var s = sources[id];
      if (!s) return;
      stopNodes(s);
      for (var i = 0; i < s.queue.length; i++) s.queue[i].end = 0;
      s.state = AL_STOPPED;
      s.offset = 0;
    },
    rewind: function (id) {
      var s = sources[id];
      if (!s) return;
      AL.stop(id);
      for (var i = 0; i < s.queue.length; i++) s.queue[i].end = undefined;
      s.state = AL_INITIAL;
    },

    queueBuffer: function (id, bufferId) {
      var s = sources[id];
      if (!s) return;
      s.streaming = true;
      s.queue.push({ id: bufferId });
      if (s.state === AL_PLAYING) scheduleQueue(s);
    },
    // Returns the id of the oldest processed buffer, or 0.
    unqueueBuffer: function (id) {
      var s = sources[id];
      if (!s || !s.queue.length) return 0;
      var q = s.queue[0];
      if (s.state === AL_PLAYING && (q.end === undefined || q.end > ctx.currentTime)) return 0;
      s.queue.shift();
      return q.id;
    },

    getState: function (id) {
      var s = sources[id];
      if (!s) return AL_STOPPED;
      if (s.streaming && s.state === AL_PLAYING) {
        var last = s.queue.length ? s.queue[s.queue.length - 1] : null;
        if (!last || (last.end !== undefined && last.end <= ctx.currentTime)) s.state = AL_STOPPED;
      }
      return s.state;
    },
    processed: function (id) {
      var s = sources[id];
      if (!s) return 0;
      var now = ctx ? ctx.currentTime : 0, n = 0;
      for (var i = 0; i < s.queue.length; i++) {
        var q = s.queue[i];
        if (s.state !== AL_PLAYING || (q.end !== undefined && q.end <= now)) n++; else break;
      }
      return n;
    },
    queued: function (id) { var s = sources[id]; return s ? s.queue.length : 0; },

    setBuffer: function (id, bufferId) {
      var s = sources[id];
      if (!s) return;
      s.buffer = bufferId;
      s.queue = [];
      s.streaming = false;
    },
    setLooping: function (id, on) {
      var s = sources[id];
      if (!s) return;
      s.looping = on;
      for (var i = 0; i < s.nodes.length; i++) if (!s.streaming) s.nodes[i].loop = on;
    },
    setRelative: function (id, on) { var s = sources[id]; if (s) { s.relative = on; updatePosition(s); } },
    setGain: function (id, v) { var s = sources[id]; if (s) setParam(s.gain.gain, Math.max(0, v)); },
    setPitch: function (id, v) {
      var s = sources[id];
      if (!s) return;
      s.pitch = Math.max(0.05, v);
      for (var i = 0; i < s.nodes.length; i++) setParam(s.nodes[i].playbackRate, s.pitch);
    },
    setRolloff: function (id, v) { var s = sources[id]; if (s) s.panner.rolloffFactor = Math.max(0, v); },
    setRefDistance: function (id, v) { var s = sources[id]; if (s) s.panner.refDistance = Math.max(0.0001, v); },
    setMaxDistance: function (id, v) { var s = sources[id]; if (s) s.panner.maxDistance = Math.max(0.0001, v); },
    setPosition: function (id, x, y, z) {
      var s = sources[id];
      if (!s) return;
      s.pos = [x, y, z];
      updatePosition(s);
    },

    listenerPosition: function (x, y, z) {
      if (!context()) return;
      listener.pos = [x, y, z];
      var l = ctx.listener;
      if (l.positionX) { setParam(l.positionX, x); setParam(l.positionY, y); setParam(l.positionZ, z); }
      else l.setPosition(x, y, z);
    },
    listenerOrientation: function (ax, ay, az, ux, uy, uz) {
      if (!context()) return;
      var l = ctx.listener;
      if (l.forwardX) {
        setParam(l.forwardX, ax); setParam(l.forwardY, ay); setParam(l.forwardZ, az);
        setParam(l.upX, ux); setParam(l.upY, uy); setParam(l.upZ, uz);
      } else l.setOrientation(ax, ay, az, ux, uy, uz);
    },
    listenerGain: function (v) { if (context()) setParam(master.gain, Math.max(0, v)); },
    distanceModel: function (model) {
      var name = model === 0xD003 || model === 0xD004 ? 'linear' : model === 0xD005 || model === 0xD006
        ? 'exponential' : 'inverse';
      for (var i = 1; i < sources.length; i++) if (sources[i]) sources[i].panner.distanceModel = name;
    }
  };
})();
