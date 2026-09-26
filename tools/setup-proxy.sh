#!/usr/bin/env bash
# Sets up and starts the multiplayer relay (tools/ws-proxy.js) for the browser build of Minecraft 1.2.5.
#
#   ./setup-proxy.sh                  asks what you want to do
#   ./setup-proxy.sh --player         relay on this computer to join any server
#   ./setup-proxy.sh --server [--target 127.0.0.1:25565] [--port 25566]
#                    [--cert fullchain.pem --key privkey.pem | --tunnel | --no-tls]
#
# Needs Node.js 16 or later. Works on Linux and macOS (on Windows: Git Bash or WSL). The script can be downloaded
# on its own: it fetches ws-proxy.js next to itself if it is missing.
set -euo pipefail

RELAY_URL="https://raw.githubusercontent.com/thatnerd64/1.2.5-website/main/tools/ws-proxy.js"
HERE="$(cd "$(dirname "$0")" && pwd)"
RELAY="$HERE/ws-proxy.js"

say() { printf '%s\n' "$*"; }
fail() { printf 'Error: %s\n' "$*" >&2; exit 1; }
ask() { # ask "question" default -> answer
  local answer
  read -r -p "$1 [$2]: " answer
  printf '%s' "${answer:-$2}"
}

# ---- Node.js and the relay ----
command -v node >/dev/null 2>&1 || fail "Node.js is not installed. Get version 16 or later from https://nodejs.org"
NODE_MAJOR="$(node -p 'process.versions.node.split(".")[0]')"
[ "$NODE_MAJOR" -ge 16 ] || fail "Node.js $(node --version) is too old; version 16 or later is needed"
if [ ! -f "$RELAY" ]; then
  say "Downloading ws-proxy.js..."
  if command -v curl >/dev/null 2>&1; then curl -fsSL "$RELAY_URL" -o "$RELAY"
  elif command -v wget >/dev/null 2>&1; then wget -q "$RELAY_URL" -O "$RELAY"
  else fail "ws-proxy.js is missing and neither curl nor wget is available to download it"; fi
fi

# ---- Options ----
MODE="" TARGET="" PORT="" CERT="" KEY="" TLS=""
while [ $# -gt 0 ]; do
  case "$1" in
    --player) MODE=player ;;
    --server) MODE=server ;;
    --target) TARGET="$2"; shift ;;
    --port) PORT="$2"; shift ;;
    --cert) CERT="$2"; TLS=cert; shift ;;
    --key) KEY="$2"; shift ;;
    --tunnel) TLS=tunnel ;;
    --no-tls) TLS=none ;;
    -h|--help) sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) fail "unknown option $1 (see --help)" ;;
  esac
  shift
done

if [ -z "$MODE" ]; then
  say "Minecraft 1.2.5 browser relay"
  say "  1) I'm a player: let my browser join any 1.2.5 server"
  say "  2) I run a server: let browser players join it"
  case "$(ask "Choose" 1)" in
    2) MODE=server ;;
    *) MODE=player ;;
  esac
fi

# ---- Player: a relay on this computer ----
if [ "$MODE" = player ]; then
  say ""
  say "In the game's launcher, set Multiplayer relay to:  ws://localhost:25566"
  say "Then add servers in Minecraft by their normal address (e.g. play.example.com:25565)."
  say "The server needs online-mode=false. Press Ctrl+C to stop the relay."
  say ""
  exec node "$RELAY"
fi

# ---- Server owner: a relay in front of one server ----
[ -n "$TARGET" ] || TARGET="$(ask "Minecraft server address" 127.0.0.1:25565)"
[ -n "$PORT" ] || PORT="$(ask "Port for the relay" 25566)"
case "$PORT" in *[!0-9]*|'') fail "bad port $PORT" ;; esac

if [ -f server.properties ] && grep -qi '^online-mode=true' server.properties; then
  say "Warning: server.properties has online-mode=true. Browser players can only join with online-mode=false."
fi

if [ -z "$TLS" ]; then
  say ""
  say "The game's page uses HTTPS, so players need a secure (wss://) address. How should the relay get one?"
  say "  1) I have a certificate for this machine's domain (e.g. from Let's Encrypt)"
  say "  2) A free Cloudflare quick tunnel (needs cloudflared; the address changes on each start)"
  say "  3) Nothing: I put my own HTTPS reverse proxy (Caddy, nginx) in front"
  case "$(ask "Choose" 2)" in
    1) TLS=cert ;;
    3) TLS=none ;;
    *) TLS=tunnel ;;
  esac
fi

ARGS=(--target "$TARGET")
TUNNEL_PID="" RELAY_PID="" LOG=""
cleanup() { # stop the tunnel and the relay together with this script (Ctrl+C included)
  for pid in $RELAY_PID $TUNNEL_PID; do kill "$pid" 2>/dev/null || true; done
  [ -z "$LOG" ] || rm -f "$LOG"
}
trap cleanup EXIT
trap 'exit 130' INT TERM
case "$TLS" in
  cert)
    [ -n "$CERT" ] || CERT="$(ask "Certificate file (full chain, PEM)" /etc/letsencrypt/live/example.com/fullchain.pem)"
    [ -n "$KEY" ] || KEY="$(ask "Private key file (PEM)" "$(dirname "$CERT")/privkey.pem")"
    [ -r "$CERT" ] || fail "cannot read $CERT"
    [ -r "$KEY" ] || fail "cannot read $KEY"
    ARGS+=(--listen "0.0.0.0:$PORT" --cert "$CERT" --key "$KEY")
    say ""
    say "Players add this server address in Minecraft:  wss://YOUR-DOMAIN:$PORT"
    say "(open port $PORT in your firewall / router)"
    ;;
  tunnel)
    command -v cloudflared >/dev/null 2>&1 || fail "cloudflared is not installed: \
https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/"
    ARGS+=(--listen "127.0.0.1:$PORT")
    LOG="$(mktemp)"
    cloudflared tunnel --url "http://localhost:$PORT" >"$LOG" 2>&1 &
    TUNNEL_PID=$!
    say "Starting the tunnel..."
    URL=""
    for _ in $(seq 1 60); do
      URL="$(grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' "$LOG" | head -n 1 || true)"
      [ -n "$URL" ] && break
      kill -0 "$TUNNEL_PID" 2>/dev/null || { cat "$LOG" >&2; fail "cloudflared stopped"; }
      sleep 1
    done
    [ -n "$URL" ] || fail "no tunnel address after 60 seconds (see $LOG)"
    say ""
    say "Players add this server address in Minecraft:  wss://${URL#https://}"
    ;;
  none)
    ARGS+=(--listen "127.0.0.1:$PORT")
    say ""
    say "Point your HTTPS reverse proxy (with WebSocket support) at http://127.0.0.1:$PORT."
    say "Players add wss://YOUR-DOMAIN as the server address. Caddy example:"
    say "  your.domain { reverse_proxy 127.0.0.1:$PORT }"
    ;;
esac

say "The server needs online-mode=false. Press Ctrl+C to stop."
say ""
node "$RELAY" "${ARGS[@]}" &
RELAY_PID=$!
wait "$RELAY_PID"
