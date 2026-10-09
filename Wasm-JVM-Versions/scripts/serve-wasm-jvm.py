#!/usr/bin/env python3
"""
serve-wasm-jvm.py — HTTP server for In-Browser OpenJDK Zero / WebAssembly JVM.

Serves the WebAssembly JVM with mandatory COOP and COEP headers required
by modern browsers for SharedArrayBuffer and WebAssembly multi-threading.

Usage:
  python3 serve-wasm-jvm.py [port]
"""

import http.server
import os
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
SERVE_DIR = os.path.join(ROOT_DIR, "web")

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8180

REQUIRED_HEADERS = {
    "Cross-Origin-Opener-Policy":   "same-origin",
    "Cross-Origin-Embedder-Policy": "require-corp",
    "Access-Control-Allow-Origin":  "*",
}

class WasmJVMHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        for k, v in REQUIRED_HEADERS.items():
            self.send_header(k, v)
        super().end_headers()

    def guess_type(self, path):
        if path.endswith('.mjs'):
            return 'application/javascript'
        if path.endswith('.wasm'):
            return 'application/wasm'
        if path.endswith('.data'):
            return 'application/octet-stream'
        return super().guess_type(path)

    def log_message(self, fmt, *args):
        code = args[1] if len(args) > 1 else "???"
        if str(code).startswith(("4", "5")):
            super().log_message(fmt, *args)

def main():
    if not os.path.isdir(SERVE_DIR):
        print(f"Error: Directory not found: {SERVE_DIR}", file=sys.stderr)
        sys.exit(1)

    os.chdir(SERVE_DIR)
    print(f"============================================================")
    print(f"  OpenJDK Zero WebAssembly JVM Server")
    print(f"  Listening on: http://localhost:{PORT}/")
    print(f"  COOP: same-origin | COEP: require-corp (SharedArrayBuffer OK)")
    print(f"============================================================")
    print(f"  - Portal:       http://localhost:{PORT}/index.html")
    print(f"  - Java Editor:  http://localhost:{PORT}/editor.html")
    print(f"  - Graphical VM: http://localhost:{PORT}/doom.html")
    print(f"============================================================\n")

    server = http.server.ThreadingHTTPServer(("", PORT), WasmJVMHandler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServer stopped.")

if __name__ == "__main__":
    main()
