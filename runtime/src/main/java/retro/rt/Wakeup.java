package retro.rt;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * Timers that keep their pace in a hidden tab. Browsers throttle a hidden page's timers (Chrome: at most once a
 * second, and after five minutes about once a minute), which would leave a multiplayer client silent until the
 * server drops it. While the page is hidden, due callbacks are run from a small Web Worker's interval instead:
 * its messages are not throttled that way.
 */
public final class Wakeup {
    @JSFunctor
    public interface Callback extends JSObject {
        void run();
    }

    private Wakeup() {
    }

    /** Runs {@code callback} after {@code millis} milliseconds (hidden tab: within about 50 ms of that). */
    @JSBody(params = { "millis", "callback" }, script = ""
            + "if (!document.hidden) { setTimeout(function() { callback(); }, millis); return; }"
            + "var ticker = window.retroWakeup;"
            + "if (!ticker) {"
            + "  ticker = window.retroWakeup = { due: [], worker: null };"
            + "  try {"
            + "    var source = 'setInterval(function() { postMessage(0); }, 50);';"
            + "    ticker.worker = new Worker(URL.createObjectURL(new Blob([source], { type: 'text/javascript' })));"
            + "    ticker.worker.onmessage = function() {"
            + "      var now = Date.now(), waiting = [], ready = [];"
            + "      for (var idx = 0; idx < ticker.due.length; idx++) {"
            + "        (ticker.due[idx].at <= now ? ready : waiting).push(ticker.due[idx]);"
            + "      }"
            + "      ticker.due = waiting;"
            + "      for (idx = 0; idx < ready.length; idx++) ready[idx].run();"
            + "    };"
            + "  } catch (failure) { ticker.worker = null; }"
            + "}"
            + "if (!ticker.worker) { setTimeout(function() { callback(); }, millis); return; }"
            + "ticker.due.push({ at: Date.now() + millis, run: function() { callback(); } });")
    public static native void schedule(int millis, Callback callback);

    /**
     * {@code Thread.sleep} for the game's network threads (see retro.build.Patches). A pending interrupt is
     * reported as by Thread.sleep; one arriving during the sleep does not cut it short (the threads sleep 2 ms).
     */
    public static void sleep(long millis) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        sleepFor((int) Math.max(0, Math.min(millis, Integer.MAX_VALUE)));
    }

    @Async
    private static native Void sleepFor(int millis);

    private static void sleepFor(int millis, AsyncCallback<Void> callback) {
        schedule(millis, () -> callback.complete(null));
    }
}
