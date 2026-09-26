package retro.input;

import org.teavm.jso.JSBody;

/**
 * Keyboard and mouse input from the page, queued in LWJGL's format. Event capture runs in JavaScript (so no
 * event is lost while the game thread is busy); {@code org.lwjgl.input.Keyboard/Mouse} drain the queues.
 */
public final class Input {
    private Input() {
    }

    @JSBody(script = ""
            + "if (window.__retroInput) return;"
            + "var canvas = document.getElementById('game');"
            + "var S = window.__retroInput = { keys: new Uint8Array(256), keyQueue: [], mouseQueue: [],"
            + "  keyDownAt: new Float64Array(256).fill(-1e9), buttonDownAt: new Float64Array(8).fill(-1e9),"
            + "  buttons: [0,0,0,0,0,0,0,0], pending: [0,0,0,0,0,0,0,0], x: 0, y: 0, dx: 0, dy: 0, wheel: 0,"
            + "  grabbed: false, repeat: false,"
            + "  focus: true };"
            + "var map = {Escape:1,Digit1:2,Digit2:3,Digit3:4,Digit4:5,Digit5:6,Digit6:7,Digit7:8,Digit8:9,Digit9:10,"
            + "Digit0:11,Minus:12,Equal:13,Backspace:14,Tab:15,KeyQ:16,KeyW:17,KeyE:18,KeyR:19,KeyT:20,KeyY:21,"
            + "KeyU:22,KeyI:23,KeyO:24,KeyP:25,BracketLeft:26,BracketRight:27,Enter:28,ControlLeft:29,KeyA:30,"
            + "KeyS:31,KeyD:32,KeyF:33,KeyG:34,KeyH:35,KeyJ:36,KeyK:37,KeyL:38,Semicolon:39,Quote:40,Backquote:41,"
            + "ShiftLeft:42,Backslash:43,KeyZ:44,KeyX:45,KeyC:46,KeyV:47,KeyB:48,KeyN:49,KeyM:50,Comma:51,"
            + "Period:52,Slash:53,ShiftRight:54,NumpadMultiply:55,AltLeft:56,Space:57,CapsLock:58,F1:59,F2:60,"
            + "F3:61,F4:62,F5:63,F6:64,F7:65,F8:66,F9:67,F10:68,NumLock:69,ScrollLock:70,Numpad7:71,Numpad8:72,"
            + "Numpad9:73,NumpadSubtract:74,Numpad4:75,Numpad5:76,Numpad6:77,NumpadAdd:78,Numpad1:79,Numpad2:80,"
            + "Numpad3:81,Numpad0:82,NumpadDecimal:83,IntlBackslash:86,F11:87,F12:88,F13:100,F14:101,F15:102,"
            + "NumpadEqual:141,NumpadEnter:156,ControlRight:157,NumpadDivide:181,PrintScreen:183,AltRight:184,"
            + "Pause:197,Home:199,ArrowUp:200,PageUp:201,ArrowLeft:203,ArrowRight:205,End:207,ArrowDown:208,"
            + "PageDown:209,Insert:210,Delete:211,MetaLeft:219,MetaRight:220,ContextMenu:221,OSLeft:219,OSRight:220};"
            + "function charOf(e) {"
            + "  if (e.key && e.key.length === 1) return e.key.charCodeAt(0);"
            + "  switch (e.key) { case 'Enter': return 13; case 'Backspace': return 8; case 'Tab': return 9;"
            + "    case 'Escape': return 27; case 'Delete': return 127; }"
            + "  return 0;"
            + "}"
            + "function allowDefault(e) {"
            + "  if ((e.ctrlKey || e.metaKey) && (e.code === 'KeyV' || e.code === 'KeyC' || e.code === 'KeyX')) return true;"
            + "  if (e.code === 'F12') return true;"
            + "  return false;"
            + "}"
            + "window.addEventListener('keydown', function(e) {"
            + "  if (!S.active) return;"
            + "  if (e.code === 'F11') { e.preventDefault(); if (document.fullscreenElement) document.exitFullscreen();"
            + "    else document.documentElement.requestFullscreen().catch(function(){}); return; }"
            + "  var k = map[e.code] || 0;"
            + "  if (!allowDefault(e)) e.preventDefault();"
            + "  if (e.repeat && !S.repeat) return;"
            + "  S.keys[k] = 1; S.keyDownAt[k] = performance.now();"
            + "  S.keyQueue.push([k, charOf(e), 1, e.repeat ? 1 : 0]);"
            + "  if (S.keyQueue.length > 256) S.keyQueue.shift();"
            + "});"
            + "window.addEventListener('keyup', function(e) {"
            + "  if (!S.active) return;"
            + "  var k = map[e.code] || 0;"
            + "  if (!allowDefault(e)) e.preventDefault();"
            + "  S.keys[k] = 0;"
            + "  S.keyQueue.push([k, 0, 0, 0]);"
            + "});"
            + "window.addEventListener('blur', function() {"
            + "  for (var i = 0; i < 256; i++) if (S.keys[i]) { S.keys[i] = 0; S.keyQueue.push([i, 0, 0, 0]); }"
            + "  for (var b = 0; b < 8; b++) if (S.buttons[b]) { S.buttons[b] = 0;"
            + "    S.mouseQueue.push([b, 0, S.x, S.y, 0, 0, 0]); }"
            + "});"
            + "function pos(e) {"
            + "  var r = canvas.getBoundingClientRect();"
            + "  var sx = canvas.width / Math.max(1, r.width), sy = canvas.height / Math.max(1, r.height);"
            + "  S.x = Math.max(0, Math.min(canvas.width - 1, Math.floor((e.clientX - r.left) * sx)));"
            + "  S.y = Math.max(0, Math.min(canvas.height - 1, canvas.height - 1 - Math.floor((e.clientY - r.top) * sy)));"
            + "}"
            + "function btn(b) { return b === 0 ? 0 : b === 2 ? 1 : b === 1 ? 2 : b; }"
            + "canvas.addEventListener('mousedown', function(e) {"
            + "  if (!S.active) return;"
            + "  e.preventDefault(); canvas.focus && canvas.focus();"
            + "  if (window.__retroAudioResume) window.__retroAudioResume();"
            + "  if (S.grabbed && document.pointerLockElement !== canvas) { S.requestLock(); }"
            + "  pos(e); var b = btn(e.button); S.buttons[b] = 1; S.pending[b]++;"
            + "  S.buttonDownAt[b] = performance.now();"
            + "  S.mouseQueue.push([b, 1, S.x, S.y, 0, 0, 0]);"
            + "});"
            + "window.addEventListener('mouseup', function(e) {"
            + "  if (!S.active) return;"
            + "  var b = btn(e.button); if (!S.buttons[b]) return; S.buttons[b] = 0;"
            + "  if (document.pointerLockElement !== canvas) pos(e);"
            + "  S.mouseQueue.push([b, 0, S.x, S.y, 0, 0, 0]);"
            + "});"
            + "window.addEventListener('mousemove', function(e) {"
            + "  if (!S.active) return;"
            + "  var dx, dy;"
            + "  if (document.pointerLockElement === canvas) { dx = e.movementX || 0; dy = -(e.movementY || 0); }"
            + "  else { var ox = S.x, oy = S.y; pos(e); dx = S.x - ox; dy = S.y - oy; }"
            + "  S.dx += dx; S.dy += dy;"
            + "  var q = S.mouseQueue, last = q.length ? q[q.length - 1] : null;"
            + "  if (last && last[0] === -1 && last[4] === 0) { last[2] = S.x; last[3] = S.y; last[5] += dx; last[6] += dy; }"
            + "  else q.push([-1, 0, S.x, S.y, 0, dx, dy]);"
            + "});"
            + "canvas.addEventListener('wheel', function(e) {"
            + "  if (!S.active) return;"
            + "  e.preventDefault();"
            + "  var d = e.deltaY > 0 ? -120 : e.deltaY < 0 ? 120 : 0;"
            + "  if (!d) return;"
            + "  S.wheel += d; S.mouseQueue.push([-1, 0, S.x, S.y, d, 0, 0]);"
            + "}, { passive: false });"
            + "canvas.addEventListener('contextmenu', function(e) { e.preventDefault(); });"
            + "function lockPlain() {"
            + "  try { var p = canvas.requestPointerLock(); if (p && p.catch) p.catch(function() {}); } catch (e) {}"
            + "}"
            + "S.requestLock = function() {"
            // Without a user gesture (e.g. when a world finishes loading) this fails; the next click locks.
            + "  try { var p = canvas.requestPointerLock({ unadjustedMovement: true });"
            + "    if (p && p.catch) p.catch(function(err) { if (err && err.name === 'NotSupportedError') lockPlain(); }); }"
            + "  catch (e) { lockPlain(); }"
            + "};"
            + "document.addEventListener('pointerlockchange', function() {"
            + "  if (document.pointerLockElement !== canvas && S.grabbed) {"
            + "    S.keyQueue.push([1, 27, 1, 0]); S.keyQueue.push([1, 0, 0, 0]);"
            + "  }"
            + "});")
    public static native void install();

    @JSBody(params = "on", script = "window.__retroInput.active = on;")
    public static native void setActive(boolean on);

    // ---- Keyboard ----

    @JSBody(script = "return window.__retroInput.keyQueue.length;")
    public static native int keyQueueSize();

    /** Returns key | char << 8 | down << 24 | repeat << 25, or -1 when the queue is empty. */
    @JSBody(script = ""
            + "var e = window.__retroInput.keyQueue.shift();"
            + "if (!e) return -1;"
            + "return (e[0] & 255) | ((e[1] & 65535) << 8) | (e[2] << 24) | (e[3] << 25);")
    public static native int nextKey();

    /**
     * A key (or button) also reads as down for at least 75 ms after it was pressed: mods such as Single Player
     * Commands poll key state once per game tick (50 ms), and would miss a quicker tap.
     */
    @JSBody(params = "key", script = "var S = window.__retroInput;"
            + " return S.keys[key] === 1 || performance.now() - S.keyDownAt[key] < 75;")
    public static native boolean isKeyDown(int key);

    @JSBody(params = "on", script = "window.__retroInput.repeat = on;")
    public static native void setRepeat(boolean on);

    // ---- Mouse ----

    /** Fills {@code out} with button, state, x, y, dwheel, dx, dy; returns false when the queue is empty. */
    public static boolean nextMouse(int[] out) {
        return nextMouseImpl(out);
    }

    @JSBody(params = "out", script = ""
            + "var S = window.__retroInput, e = S.mouseQueue.shift();"
            + "if (!e) return false;"
            + "if (e[1] && e[0] >= 0 && S.pending[e[0]] > 0) S.pending[e[0]]--;"
            + "for (var i = 0; i < 7; i++) out[i] = e[i];"
            + "return true;")
    private static native boolean nextMouseImpl(@org.teavm.jso.JSByRef int[] out);

    @JSBody(script = "return window.__retroInput.x;")
    public static native int mouseX();

    @JSBody(script = "return window.__retroInput.y;")
    public static native int mouseY();

    @JSBody(script = "var S = window.__retroInput, d = S.dx; S.dx = 0; return d;")
    public static native int takeDX();

    @JSBody(script = "var S = window.__retroInput, d = S.dy; S.dy = 0; return d;")
    public static native int takeDY();

    @JSBody(script = "var S = window.__retroInput, d = S.wheel; S.wheel = 0; return d;")
    public static native int takeWheel();

    /**
     * A button also counts as down until the game has read its press event: GUI code such as GuiSlot discards
     * queued events while no button is down, which would lose quick clicks (press and release within one frame).
     */
    @JSBody(params = "b", script = "var S = window.__retroInput; return S.buttons[b] === 1 || S.pending[b] > 0"
            + " || performance.now() - S.buttonDownAt[b] < 75;")
    public static native boolean isButtonDown(int b);

    @JSBody(params = "on", script = ""
            + "var S = window.__retroInput; var canvas = document.getElementById('game');"
            + "S.grabbed = on;"
            + "if (on) { if (document.pointerLockElement !== canvas) S.requestLock(); }"
            + "else if (document.pointerLockElement === canvas) document.exitPointerLock();"
            + "S.dx = 0; S.dy = 0;")
    public static native void setGrabbed(boolean on);

    @JSBody(script = "return document.pointerLockElement === document.getElementById('game');")
    public static native boolean isGrabbed();

    @JSBody(params = { "x", "y" }, script = "window.__retroInput.x = x; window.__retroInput.y = y;")
    public static native void setCursorPosition(int x, int y);

    @JSBody(script = "return document.getElementById('game').matches(':hover');")
    public static native boolean isInsideWindow();
}
