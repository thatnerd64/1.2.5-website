#!/usr/bin/env node
// WebSocket -> TCP relay for playing on Minecraft 1.2.5 servers from the browser build.
//
// Browsers cannot open TCP sockets, so the game opens a WebSocket to this relay and the relay opens the TCP
// connection to the server. Bytes are forwarded unchanged in both directions. Needs Node.js 16 or later and
// nothing else (no npm install).
//
// Player, joining any server:
//   node tools/ws-proxy.js
//   Set the launcher's relay to ws://localhost:25566, then use normal server addresses (play.example.com:25565).
//
// Server owner, relaying to one server:
//   node tools/ws-proxy.js --listen 0.0.0.0:25566 --target 127.0.0.1:25565 [--cert fullchain.pem --key privkey.pem]
//   Players add wss://your.host:25566 as the server address. Instead of --cert/--key, any TLS terminator or
//   tunnel in front of the relay works too, e.g. `cloudflared tunnel --url http://localhost:25566`.
//   The server needs online-mode=false in server.properties: browser players have no Mojang session (and the
//   login servers 1.2.5 used are gone anyway).
//
// Options (or the environment variable in brackets):
//   --listen [host:]port     where to listen, default 127.0.0.1:25566; IPv6 as [::]:25566 [LISTEN]
//   --target host[:port]     always connect to this server, whatever the client asks for [TARGET]
//   --allow host[:port],...  the servers clients may choose; any other is refused [ALLOW]
//   --origin origin,...|*    the pages (browser Origin header) that may use the relay; an entry ending in :*
//                            allows any port, e.g. http://localhost:*. Default: * with --target, otherwise
//                            https://thatnerd64.github.io and pages on localhost [ORIGIN]
//   --cert file --key file   serve wss:// with this PEM certificate chain and private key [TLS_CERT, TLS_KEY]
//   --connect-timeout ms     how long to wait for the server to accept, default 10000 [CONNECT_TIMEOUT]
//   --open                   allow a relay to any server on a non-loopback address
//   --quiet                  do not log each connection
//   --help                   show this text
// Server ports default to 25565. A relay without --target or --allow can reach any host, so it only listens on
// a loopback address unless --open is given, and only serves the default pages unless --origin says otherwise.
//
// Protocol: clients connect to ws[s]://relay/?host=<host>&port=<port> (with --target no parameters are needed;
// any are ignored). A client offering the subprotocol "mc-relay.v1" gets the text message "connected" once the
// TCP connection is up, or a close with code 4001 unknown host, 4002 connection refused, 4003 timed out,
// 4004 unreachable, 4005 refused by this relay or 4000 other error (the reason says more). Clients offering
// only "binary" (websockify style) or no subprotocol get the plain byte stream. Binary messages carry the data;
// the relay closes with 1000 when the server closes the connection and with 4000 when it fails.
'use strict';

const crypto = require('crypto');
const fs = require('fs');
const http = require('http');
const https = require('https');
const net = require('net');

const RELAY_PORT = 25566;
const DEFAULT_LISTEN = `127.0.0.1:${RELAY_PORT}`;
const MINECRAFT_PORT = 25565;

// The pages that may use a relay without --target. Browsers let any website open WebSockets to localhost, so
// an open relay on a player's machine must not answer arbitrary sites: they could use it to reach the player's
// LAN (routers, printers, NAS admin pages). Clients that send no Origin (not browsers) are always allowed.
const DEFAULT_ORIGINS = [
  'https://thatnerd64.github.io',
  'http://localhost:*', 'http://127.0.0.1:*', 'https://localhost:*', 'https://127.0.0.1:*', 'http://[::1]:*',
];

const RELAY_PROTOCOL = 'mc-relay.v1';
const WEBSOCKET_GUID = '258EAFA5-E914-47DA-95CA-C5AB0DC85B11';
const MAX_MESSAGE = 16 * 1024 * 1024; // larger client messages are refused (close 1009)
const MAX_REASON = 120;               // close reasons are limited to 123 bytes of UTF-8
const PING_INTERVAL = 30000;          // keeps idle connections open through proxies and tunnels
const CLOSE_TIMEOUT = 2000;           // how long a peer gets to close its side once we have closed ours
const CLOSE_REPLY_TIMEOUT = 30000;    // how long a client gets to answer our close frame (may have data to read)

const CLOSE = {
  NORMAL: 1000, PROTOCOL_ERROR: 1002, TOO_BIG: 1009, // RFC 6455
  ERROR: 4000, UNKNOWN_HOST: 4001, REFUSED: 4002, TIMED_OUT: 4003, UNREACHABLE: 4004, POLICY: 4005,
};

// How TCP connect errors are reported: [close code, reason].
const CONNECT_ERRORS = {
  ENOTFOUND: [CLOSE.UNKNOWN_HOST, 'Unknown host'],
  EAI_AGAIN: [CLOSE.UNKNOWN_HOST, 'Unknown host (DNS lookup failed)'],
  EAI_NONAME: [CLOSE.UNKNOWN_HOST, 'Unknown host'],
  EAI_NODATA: [CLOSE.UNKNOWN_HOST, 'Unknown host'],
  ECONNREFUSED: [CLOSE.REFUSED, 'Connection refused'],
  ETIMEDOUT: [CLOSE.TIMED_OUT, 'Connection timed out'],
  EHOSTUNREACH: [CLOSE.UNREACHABLE, 'No route to host'],
  ENETUNREACH: [CLOSE.UNREACHABLE, 'Network is unreachable'],
};

// Reasons for errors on an established connection (sent with CLOSE.ERROR).
const STREAM_ERRORS = {
  ECONNRESET: 'Connection reset',
  EPIPE: 'Broken pipe',
  ETIMEDOUT: 'Connection timed out',
  ECONNABORTED: 'Connection aborted',
};

let config; // set by main()


// ---- Options ------------------------------------------------------------------------------------------------

/** The option values from the command line, falling back to environment variables. Throws on bad input. */
function parseOptions(argv, env) {
  const flags = ['open', 'quiet', 'help'];
  const valued = ['listen', 'target', 'allow', 'origin', 'cert', 'key', 'connect-timeout'];
  const given = {};
  for (let i = 0; i < argv.length; i++) {
    const match = /^--([a-z-]+)(?:=([\s\S]*))?$/.exec(argv[i]);
    const name = argv[i] === '-h' ? 'help' : match && match[1];
    if (flags.includes(name)) {
      if (match && match[2] !== undefined) throw new Error(`--${name} takes no value`);
      given[name] = true;
    } else if (valued.includes(name)) {
      const value = match[2] !== undefined ? match[2] : argv[++i];
      if (value === undefined) throw new Error(`--${name} needs a value`);
      given[name] = value;
    } else {
      throw new Error(`unknown option ${argv[i]}`);
    }
  }
  const get = (name, envName, fallback) =>
    given[name] !== undefined ? given[name] : env[envName] ? env[envName] : fallback;

  const options = { help: !!given.help, open: !!given.open, quiet: !!given.quiet };
  options.listen = parseAddress(get('listen', 'LISTEN', DEFAULT_LISTEN), 'listen');
  const target = get('target', 'TARGET', '');
  options.target = target ? parseAddress(target, 'target') : null;
  const allow = splitList(get('allow', 'ALLOW', ''));
  options.allow = allow.length ? allow.map(entry => parseAddress(entry, 'allow')) : null;
  const origin = get('origin', 'ORIGIN', '');
  options.origins = parseOrigins(origin ? splitList(origin) : options.target ? ['*'] : DEFAULT_ORIGINS);

  const timeout = String(get('connect-timeout', 'CONNECT_TIMEOUT', '10000'));
  if (!/^\d+$/.test(timeout) || Number(timeout) < 1) throw new Error(`bad --connect-timeout ${timeout}`);
  options.connectTimeout = Number(timeout);

  const cert = get('cert', 'TLS_CERT', ''), key = get('key', 'TLS_KEY', '');
  if (!cert !== !key) throw new Error('--cert and --key go together');
  options.tls = cert ? { cert: readFile(cert, 'cert'), key: readFile(key, 'key') } : null;
  return options;
}

/**
 * Parses host:port, [ipv6]:port, a bare host or IPv6 address (port 25565) or, for --listen, a bare port
 * (host 127.0.0.1, and port 0 picks a free one).
 */
function parseAddress(text, option) {
  const s = String(text).trim();
  let host = s, port;
  const bracketed = /^\[([^\]]*)\](?::(.*))?$/.exec(s);
  if (bracketed) {
    host = bracketed[1];
    port = bracketed[2];
  } else if (option === 'listen' && /^\d+$/.test(s)) {
    host = '';
    port = s;
  } else if (s.indexOf(':') >= 0 && s.indexOf(':') === s.lastIndexOf(':')) {
    host = s.slice(0, s.indexOf(':'));
    port = s.slice(s.indexOf(':') + 1);
  }
  const listening = option === 'listen';
  if (listening && !host) host = '127.0.0.1';
  let number = listening ? RELAY_PORT : MINECRAFT_PORT;
  if (port !== undefined) number = /^\d{1,5}$/.test(port) ? Number(port) : -1;
  if (!host || number < (listening ? 0 : 1) || number > 65535) throw new Error(`bad --${option} address ${s}`);
  return { host, port: number };
}

function splitList(text) {
  return String(text).split(',').map(s => s.trim()).filter(Boolean);
}

/** Origin rules: '*', or a list of {base, anyPort} (an entry like http://localhost:* allows any port). */
function parseOrigins(list) {
  if (list.includes('*')) return '*';
  return list.map(entry => {
    const origin = entry.toLowerCase().replace(/\/+$/, '');
    const anyPort = origin.endsWith(':*');
    return { base: anyPort ? origin.slice(0, -2) : origin, anyPort };
  });
}

function readFile(file, option) {
  try {
    return fs.readFileSync(file);
  } catch (err) {
    throw new Error(`cannot read --${option} file: ${err.message}`);
  }
}

function isLoopback(host) {
  const h = host.toLowerCase();
  return h === 'localhost' || h === '::1' || /^127\./.test(h) || /^::ffff:127\./.test(h);
}

/** The header comment of this file, which is the usage text. */
function usage() {
  const lines = fs.readFileSync(__filename, 'utf8').split('\n');
  const text = [];
  for (const line of lines.slice(1)) {
    if (!line.startsWith('//')) break;
    text.push(line.replace(/^\/\/ ?/, ''));
  }
  return text.join('\n') + '\n';
}


// ---- Policy -------------------------------------------------------------------------------------------------

function originAllowed(origin) {
  if (origin === undefined || config.origins === '*') return true;
  const o = origin.toLowerCase();
  return config.origins.some(rule => o === rule.base
      || (rule.anyPort && o.startsWith(rule.base + ':') && /^\d+$/.test(o.slice(rule.base.length + 1))));
}

/** Where a new connection goes: {host, port}, or {refuse: reason} (closed with CLOSE.POLICY). */
function chooseServer(origin, params) {
  if (!originAllowed(origin)) return { refuse: `This relay does not accept connections from ${printable(origin)}` };
  if (config.target) return config.target;
  const host = (params.get('host') || '').trim().replace(/^\[(.*)\]$/, '$1');
  const port = /^\d{1,5}$/.test(params.get('port') || '') ? Number(params.get('port')) : 0;
  if (!host || host.length > 255 || port < 1 || port > 65535) return { refuse: 'host and port required' };
  const key = serverKey({ host, port });
  if (config.allow && !config.allow.some(allowed => serverKey(allowed) === key)) {
    return { refuse: `${printable(formatAddress({ host, port }))} is not allowed by this relay` };
  }
  return { host, port };
}

function serverKey(address) {
  return `${address.host.toLowerCase()}:${address.port}`;
}


// ---- WebSocket (RFC 6455, server side) ----------------------------------------------------------------------

const openSockets = new Set(); // for the keep-alive pings
const EMPTY = Buffer.alloc(0);

/**
 * One WebSocket connection over an upgraded HTTP socket. Parses the client's (masked) frames and writes
 * unmasked ones. Binary data is handed to onData frame by frame as it arrives, fragments included, so large
 * messages are not collected first; text messages and pongs are ignored and pings answered.
 * onClose(code, reason) runs once the socket is closed, with the client's close code or 1006 if it sent none.
 */
class WebSocket {
  constructor(socket) {
    this.socket = socket;
    this.state = 'open';     // 'open' -> 'closing' (a close frame was sent) -> 'closed' (socket closed)
    this.onData = () => {};
    this.onClose = () => {};
    this.chunks = [];        // received bytes not parsed yet
    this.buffered = 0;
    this.frame = null;       // {opcode, length, mask} of the frame whose payload is being received
    this.message = 0;        // opcode of the fragmented message in progress, 0 if none
    this.messageSize = 0;
    this.peerClose = null;   // {code, reason} from the client's close frame
    this.failure = null;     // why we failed the connection (a protocol error by the client)
    this.error = null;       // socket error, if any
    this.closeTimer = null;
    socket.setNoDelay(true); // game packets are small and latency matters
    socket.on('data', chunk => this.receive(chunk));
    socket.on('end', () => socket.end()); // the client closed its side: close ours (HTTP sockets allow half-open)
    socket.on('error', err => { this.error = this.error || err; }); // 'close' follows
    socket.on('close', () => this.closed());
    openSockets.add(this);
  }

  /** Sends binary data; false when the socket's buffer is full (wait for the socket's 'drain'). */
  sendBinary(data) {
    return this.state === 'open' ? this.write(0x2, data) : true;
  }

  sendText(text) {
    if (this.state === 'open') this.write(0x1, Buffer.from(text));
  }

  ping() {
    if (this.state === 'open') this.write(0x9, EMPTY);
  }

  /**
   * Starts the closing handshake. The client answers with its own close frame once it has read everything
   * sent before, then the socket is closed; a client that does not answer is disconnected.
   */
  close(code, reason) {
    if (this.state !== 'open') return;
    this.state = 'closing';
    this.write(0x8, closePayload(code, reason));
    this.socket.resume(); // in case the relay paused it: the client's answer has to be read
    this.closeAfter(CLOSE_REPLY_TIMEOUT);
  }

  receive(chunk) {
    if (this.peerClose || this.failure) return; // nothing after a close frame matters; keep reading to drain
    this.chunks.push(chunk);
    this.buffered += chunk.length;
    while (this.parseFrame()) {
      // one frame per iteration
    }
  }

  /** Handles the next frame if it is complete; false when more bytes are needed or reading has stopped. */
  parseFrame() {
    if (this.peerClose || this.failure) return false;
    if (!this.frame) {
      if (this.buffered < 2) return false;
      const head = this.peek(Math.min(this.buffered, 14));
      const fin = (head[0] & 0x80) !== 0;
      const opcode = head[0] & 0x0f;
      const size7 = head[1] & 0x7f;
      if (head[0] & 0x70) return this.fail(CLOSE.PROTOCOL_ERROR, 'Reserved bits set'); // no extensions
      if (!(head[1] & 0x80)) return this.fail(CLOSE.PROTOCOL_ERROR, 'Client frames must be masked');
      if (opcode >= 0x8) { // control frame (close, ping, pong): may come between the fragments of a message
        if (opcode > 0xA) return this.fail(CLOSE.PROTOCOL_ERROR, 'Unknown opcode');
        if (!fin || size7 > 125) return this.fail(CLOSE.PROTOCOL_ERROR, 'Invalid control frame');
      } else if (opcode > 0x2) {
        return this.fail(CLOSE.PROTOCOL_ERROR, 'Unknown opcode');
      } else if (opcode === 0x0 && !this.message) {
        return this.fail(CLOSE.PROTOCOL_ERROR, 'Unexpected continuation frame');
      } else if (opcode !== 0x0 && this.message) {
        return this.fail(CLOSE.PROTOCOL_ERROR, 'Expected a continuation frame');
      }

      const headerSize = 2 + (size7 === 126 ? 2 : size7 === 127 ? 8 : 0) + 4;
      if (this.buffered < headerSize) return false;
      let length = size7;
      if (size7 === 126) length = head.readUInt16BE(2);
      if (size7 === 127) {
        const high = head.readUInt32BE(2);
        if (high >= 0x80000000) return this.fail(CLOSE.PROTOCOL_ERROR, 'Invalid frame length');
        length = high * 2 ** 32 + head.readUInt32BE(6);
      }
      let type = opcode;
      if (opcode < 0x8) { // text (1), binary (2) or a continuation (0) of the message in progress
        type = opcode || this.message;
        const size = (opcode ? 0 : this.messageSize) + length;
        if (size > MAX_MESSAGE) return this.fail(CLOSE.TOO_BIG, 'Message too big');
        this.message = fin ? 0 : type;
        this.messageSize = size;
      }
      const header = this.take(headerSize);
      this.frame = { opcode: type, length, mask: header.subarray(headerSize - 4) };
    }

    const { opcode, length, mask } = this.frame;
    if (this.buffered < length) return false;
    this.frame = null;
    const payload = this.take(length);
    for (let i = 0; i < payload.length; i++) payload[i] ^= mask[i & 3];
    if (opcode === 0x2) {
      if (payload.length) this.onData(payload);
    } else if (opcode === 0x8) {
      this.receivedClose(payload);
    } else if (opcode === 0x9) {
      if (this.state === 'open') this.write(0xA, payload); // pong with the ping's payload
    }
    return true;
  }

  receivedClose(payload) {
    let code = 1005, reason = ''; // 1005: the frame has no status code
    if (payload.length === 1) return this.fail(CLOSE.PROTOCOL_ERROR, 'Invalid close frame');
    if (payload.length >= 2) {
      code = payload.readUInt16BE(0);
      reason = payload.toString('utf8', 2);
      if (!validCloseCode(code)) return this.fail(CLOSE.PROTOCOL_ERROR, 'Invalid close code');
    }
    this.peerClose = { code, reason };
    this.discardInput();
    if (this.state === 'open') { // the client started closing: echo its code, then close the TCP connection
      this.state = 'closing';
      this.write(0x8, code === 1005 ? EMPTY : closePayload(code, ''));
    }
    this.socket.end();
    this.closeAfter(CLOSE_TIMEOUT);
  }

  /** Fails the connection on a protocol error: tells the client why and closes without waiting for it. */
  fail(code, reason) {
    this.failure = reason;
    this.discardInput();
    if (this.state === 'open') {
      this.state = 'closing';
      this.write(0x8, closePayload(code, reason));
    }
    this.socket.resume();
    this.socket.end();
    this.closeAfter(CLOSE_TIMEOUT);
    return false;
  }

  closed() {
    clearTimeout(this.closeTimer);
    this.state = 'closed';
    this.discardInput();
    openSockets.delete(this);
    const onClose = this.onClose;
    this.onClose = () => {};
    onClose(this.peerClose ? this.peerClose.code : 1006, this.peerClose ? this.peerClose.reason : '');
  }

  /** Writes one unmasked frame; false when the socket's buffer is full. */
  write(opcode, payload) {
    if (!this.socket.writable) return true;
    const length = payload.length;
    const header = Buffer.allocUnsafe(length < 126 ? 2 : length < 65536 ? 4 : 10);
    header[0] = 0x80 | opcode; // FIN: frames are never fragmented
    if (length < 126) {
      header[1] = length;
    } else if (length < 65536) {
      header[1] = 126;
      header.writeUInt16BE(length, 2);
    } else {
      header[1] = 127;
      header.writeUInt32BE(Math.floor(length / 2 ** 32), 2);
      header.writeUInt32BE(length >>> 0, 6);
    }
    if (!length) return this.socket.write(header);
    this.socket.cork(); // header and payload go out in one write, without copying the payload
    this.socket.write(header);
    const ok = this.socket.write(payload);
    this.socket.uncork();
    return ok;
  }

  closeAfter(ms) {
    clearTimeout(this.closeTimer);
    this.closeTimer = setTimeout(() => this.socket.destroy(), ms);
  }

  /** At least the first n buffered bytes, without consuming them; n must not exceed this.buffered. */
  peek(n) {
    if (this.chunks[0].length >= n) return this.chunks[0];
    const out = Buffer.allocUnsafe(n);
    let offset = 0;
    for (const chunk of this.chunks) {
      offset += chunk.copy(out, offset, 0, Math.min(chunk.length, n - offset));
      if (offset === n) break;
    }
    return out;
  }

  /** Removes and returns the first n buffered bytes; n must not exceed this.buffered. */
  take(n) {
    if (n === 0) return EMPTY;
    this.buffered -= n;
    const first = this.chunks[0];
    if (first.length === n) return this.chunks.shift();
    if (first.length > n) {
      this.chunks[0] = first.subarray(n);
      return first.subarray(0, n);
    }
    const out = Buffer.allocUnsafe(n);
    let offset = 0;
    while (offset < n) {
      const chunk = this.chunks[0];
      const count = Math.min(chunk.length, n - offset);
      chunk.copy(out, offset, 0, count);
      offset += count;
      if (count === chunk.length) this.chunks.shift();
      else this.chunks[0] = chunk.subarray(count);
    }
    return out;
  }

  discardInput() {
    this.chunks = [];
    this.buffered = 0;
    this.frame = null;
  }
}

function closePayload(code, reason) {
  const text = Buffer.from(clip(reason || '', MAX_REASON));
  const payload = Buffer.allocUnsafe(2 + text.length);
  payload.writeUInt16BE(code, 0);
  text.copy(payload, 2);
  return payload;
}

function validCloseCode(code) {
  return (code >= 1000 && code <= 1014 && code !== 1004 && code !== 1005 && code !== 1006)
      || (code >= 3000 && code <= 4999);
}

/** Shortens text to at most max bytes of UTF-8 without splitting a character. */
function clip(text, max) {
  if (Buffer.byteLength(text) <= max) return text;
  let out = '';
  for (const ch of text) {
    if (Buffer.byteLength(out + ch) > max - 3) break;
    out += ch;
  }
  return out + '...';
}


// ---- Relay --------------------------------------------------------------------------------------------------

/** Answers plain HTTP requests (useful as a health check). */
function respond(req, res) {
  const body = `Minecraft 1.2.5 WebSocket relay${config.target ? ` for ${formatAddress(config.target)}` : ''}\n`;
  res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8', 'Content-Length': Buffer.byteLength(body) });
  res.end(body);
}

/** The WebSocket handshake. Policy refusals complete it and then close with 4005, so the page sees why. */
function upgrade(req, socket, head) {
  socket.on('error', () => {}); // until WebSocket takes over; a failed handshake is just dropped
  const key = req.headers['sec-websocket-key'] || '';
  if (req.method !== 'GET' || String(req.headers.upgrade).toLowerCase() !== 'websocket'
      || !/^[A-Za-z0-9+/]{22}==$/.test(key)) {
    return badRequest(socket, 'Not a valid WebSocket request');
  }
  if (req.headers['sec-websocket-version'] !== '13') {
    return badRequest(socket, 'Unsupported WebSocket version', 'Sec-WebSocket-Version: 13\r\n');
  }
  const offered = String(req.headers['sec-websocket-protocol'] || '').split(',').map(s => s.trim());
  const protocol = offered.includes(RELAY_PROTOCOL) ? RELAY_PROTOCOL : offered.includes('binary') ? 'binary' : null;
  const accept = crypto.createHash('sha1').update(key + WEBSOCKET_GUID).digest('base64');
  socket.write('HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n'
      + `Sec-WebSocket-Accept: ${accept}\r\n`
      + (protocol ? `Sec-WebSocket-Protocol: ${protocol}\r\n` : '') + '\r\n');
  const ws = new WebSocket(socket);
  relay(ws, req, protocol === RELAY_PROTOCOL);
  if (head.length) ws.receive(head);
}

function badRequest(socket, message, extraHeaders) {
  const body = message + '\n';
  socket.end('HTTP/1.1 400 Bad Request\r\nConnection: close\r\nContent-Type: text/plain\r\n'
      + `Content-Length: ${Buffer.byteLength(body)}\r\n${extraHeaders || ''}\r\n${body}`, () => socket.destroy());
}

/**
 * Connects one WebSocket to its server and pipes bytes both ways, with backpressure: when one side's buffer
 * fills up, reading from the other side pauses until it drains. With statusMessages (subprotocol mc-relay.v1)
 * the client is told when the connection is up, and why it failed if it did not come up.
 */
function relay(ws, req, statusMessages) {
  const started = Date.now();
  const client = clientAddress(req);
  let params;
  try {
    params = new URL(req.url, 'http://relay').searchParams;
  } catch (err) {
    params = new URLSearchParams();
  }
  const server = chooseServer(req.headers.origin, params);
  const label = `${client} -> ${server.refuse ? requestedServer(params) : printable(formatAddress(server))}`;
  let outcome = null;       // how the connection ended, for the log (the first reason wins)
  let up = 0, down = 0;     // bytes client -> server and server -> client
  let tcp = null, connected = false, connectTimer = null;
  let tcpPaused = false, wsPaused = false;
  const end = text => { outcome = outcome || text; };

  ws.onClose = code => {
    clearTimeout(connectTimer);
    end(ws.peerClose ? `closed by client (${code})` : ws.failure ? `protocol error: ${ws.failure}`
        : ws.error ? `client error: ${streamError(ws.error)}` : 'client disconnected');
    if (tcp && connected) { // let queued data reach the server, then close
      tcp.resume(); // (if paused) to see the server's side close
      tcp.end();
      setTimeout(() => tcp.destroy(), CLOSE_TIMEOUT);
    } else if (tcp) {
      tcp.destroy();
    }
    const seconds = ((Date.now() - started) / 1000).toFixed(1);
    log(`${label} ${outcome}; up ${formatBytes(up)}, down ${formatBytes(down)}, ${seconds}s`);
  };

  if (server.refuse) {
    end(`refused: ${server.refuse}`);
    ws.close(CLOSE.POLICY, server.refuse);
    return;
  }

  ws.onData = data => {
    if (!tcp || !tcp.writable) return;
    up += data.length;
    if (!tcp.write(data) && !wsPaused) { // writes before the connection is up are queued by net.Socket
      wsPaused = true;
      ws.socket.pause();
      tcp.once('drain', () => { wsPaused = false; ws.socket.resume(); });
    }
  };

  log(`${label} connecting`);
  try {
    tcp = net.connect({ host: server.host, port: server.port });
  } catch (err) { // not expected for any string, but a bad host must not bring the relay down
    end(`connect failed: ${err.message}`);
    ws.close(CLOSE.ERROR, err.message);
    return;
  }
  tcp.setNoDelay(true);
  connectTimer = setTimeout(() => {
    end('connect failed: timed out');
    ws.close(CLOSE.TIMED_OUT, 'Connection timed out');
    tcp.destroy();
  }, config.connectTimeout);

  tcp.on('connect', () => {
    clearTimeout(connectTimer);
    connected = true;
    if (ws.state !== 'open') return tcp.destroy();
    if (statusMessages) ws.sendText('connected');
  });
  tcp.on('data', data => {
    if (ws.state !== 'open') return;
    down += data.length;
    if (!ws.sendBinary(data) && !tcpPaused) {
      tcpPaused = true;
      tcp.pause();
      ws.socket.once('drain', () => { tcpPaused = false; tcp.resume(); });
    }
  });
  tcp.on('end', () => { // the server closed the connection: everything it sent has been passed on
    end('closed by server');
    ws.close(CLOSE.NORMAL, '');
  });
  tcp.on('error', err => {
    clearTimeout(connectTimer);
    if (!connected) {
      const failure = connectError(err);
      end(`connect failed: ${failure[1]}`);
      ws.close(failure[0], failure[1]);
    } else {
      end(`server error: ${streamError(err)}`);
      ws.close(CLOSE.ERROR, streamError(err));
    }
  });
  tcp.on('close', () => {
    clearTimeout(connectTimer);
    end('closed by server');
    ws.close(CLOSE.NORMAL, '');
  });
}

/** [close code, reason] for a failed TCP connect. */
function connectError(err) {
  // With several addresses for a host (IPv6 and IPv4), Node reports an AggregateError of all attempts; if any
  // was refused, the host is up and nothing listens on the port, which is the most useful thing to say.
  const codes = [err.code].concat((err.errors || []).map(e => e.code));
  const code = codes.includes('ECONNREFUSED') ? 'ECONNREFUSED' : codes.find(Boolean);
  return CONNECT_ERRORS[code] || [CLOSE.ERROR, err.message || String(code || err)];
}

function streamError(err) {
  return STREAM_ERRORS[err.code] || err.message || String(err);
}

/**
 * The client's address for logs: the first X-Forwarded-For / CF-Connecting-IP entry when a proxy or tunnel
 * forwards the connection. These headers can be forged, so they are only ever logged.
 */
function clientAddress(req) {
  const forwarded = req.headers['cf-connecting-ip'] || req.headers['x-forwarded-for'];
  const address = forwarded ? String(forwarded).split(',')[0].trim() : req.socket.remoteAddress || '?';
  return printable(address.replace(/^::ffff:/i, ''));
}

/** The server a refused client asked for, for the log. */
function requestedServer(params) {
  if (config.target) return formatAddress(config.target);
  return params.has('host') ? printable(`${params.get('host')}:${params.get('port')}`) : '(no server)';
}

function formatAddress(address) {
  return (address.host.indexOf(':') >= 0 ? `[${address.host}]` : address.host) + ':' + address.port;
}

/** Client-supplied text made safe for a log line. */
function printable(text) {
  return clip(String(text).replace(/[^\x21-\x7e]/g, '?'), 100);
}

function formatBytes(n) {
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(1)} MB`;
}

function log(line) {
  if (!config.quiet) console.log(`${new Date().toISOString()} ${line}`);
}


// ---- Main ---------------------------------------------------------------------------------------------------

function main() {
  try {
    config = parseOptions(process.argv.slice(2), process.env);
  } catch (err) {
    console.error(`ws-proxy: ${err.message}\nRun with --help for usage.`);
    process.exit(2);
  }
  if (config.help) {
    process.stdout.write(usage());
    return;
  }
  const listen = formatAddress(config.listen);
  if (!config.target && !config.allow && !config.open && !isLoopback(config.listen.host)) {
    console.error(`ws-proxy: refusing to run an open relay on ${listen}.\n`
        + 'Without --target or --allow, anyone who can reach this address could use the relay to connect to any\n'
        + 'host, including machines on your local network. Use --target host:port to relay to one server,\n'
        + '--allow host:port,... to limit the servers clients may choose, or --open if you really want this.');
    process.exit(1);
  }
  if (config.target && config.allow) console.error('ws-proxy: --allow has no effect with --target');

  let server;
  try {
    server = config.tls ? https.createServer(config.tls, respond) : http.createServer(respond);
  } catch (err) {
    console.error(`ws-proxy: bad --cert/--key: ${err.message}`);
    process.exit(1);
  }
  server.on('upgrade', upgrade);
  server.on('error', err => {
    if (server.listening) return console.error(`ws-proxy: ${err.message}`); // e.g. out of file descriptors
    console.error(`ws-proxy: cannot listen on ${listen}: ${err.message}`);
    process.exit(1);
  });
  server.listen(config.listen.port, config.listen.host, () => {
    const address = formatAddress({ host: config.listen.host, port: server.address().port });
    const servers = config.target ? `fixed target ${formatAddress(config.target)}`
        : config.allow ? `allowed servers ${config.allow.map(formatAddress).join(', ')}` : 'any server';
    const origins = config.origins === '*' ? 'any'
        : config.origins.map(rule => rule.base + (rule.anyPort ? ':*' : '')).join(', ');
    console.log(`${new Date().toISOString()} Minecraft 1.2.5 WebSocket relay listening on `
        + `${config.tls ? 'wss' : 'ws'}://${address}; ${servers}; origins: ${origins}`);
  });
  setInterval(() => {
    for (const ws of openSockets) ws.ping();
  }, PING_INTERVAL);
}

main();
