#!/usr/bin/env node
// WebSocket -> TCP proxy for playing on Minecraft 1.2.5 servers from the browser build.
//
// Browsers cannot open raw TCP sockets, so the game connects to this proxy over a WebSocket and names the
// server it wants: ws://proxy-host:8080/?host=mc.example.com&port=25565. Bytes are forwarded unchanged.
//
//   npm install ws
//   node tools/ws-proxy.js [--port 8080] [--allow mc.example.com:25565,other.host:25565]
//
// Then enter ws://localhost:8080 (or wss://... behind TLS) as the multiplayer proxy in the launcher.
// Use --allow to restrict which servers can be reached; without it any host is allowed, so do not expose an
// unrestricted proxy to the internet.
'use strict';

const net = require('net');
const { WebSocketServer } = require('ws');

const args = process.argv.slice(2);
function option(name, fallback) {
  const i = args.indexOf('--' + name);
  return i >= 0 && i + 1 < args.length ? args[i + 1] : fallback;
}
const port = parseInt(option('port', process.env.PORT || '8080'), 10);
const allow = (option('allow', process.env.ALLOW || '') || '').split(',').map(s => s.trim()).filter(Boolean);

const wss = new WebSocketServer({ port });
console.log(`Minecraft WebSocket proxy listening on :${port}` + (allow.length ? ` (allowed: ${allow.join(', ')})` : ''));

wss.on('connection', (ws, req) => {
  const url = new URL(req.url, 'http://localhost');
  const host = url.searchParams.get('host');
  const targetPort = parseInt(url.searchParams.get('port') || '25565', 10);
  if (!host || !(targetPort > 0 && targetPort < 65536)) {
    ws.close(1008, 'host and port required');
    return;
  }
  if (allow.length && !allow.includes(`${host}:${targetPort}`)) {
    ws.close(1008, 'server not allowed by proxy');
    return;
  }
  const socket = net.connect({ host, port: targetPort });
  const pending = [];
  let open = false;
  socket.on('connect', () => {
    open = true;
    for (const chunk of pending) socket.write(chunk);
    pending.length = 0;
  });
  socket.on('data', data => { if (ws.readyState === ws.OPEN) ws.send(data); });
  socket.on('error', err => ws.close(1011, String(err.message).slice(0, 120)));
  socket.on('close', () => ws.close());
  ws.on('message', data => {
    const buf = Buffer.isBuffer(data) ? data : Buffer.from(data);
    if (open) socket.write(buf); else pending.push(buf);
  });
  ws.on('close', () => socket.destroy());
  ws.on('error', () => socket.destroy());
});
