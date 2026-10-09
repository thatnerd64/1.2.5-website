package retro.glue;

import cpw.mods.fml.relauncher.RetroInjection;
import retro.JS;

/** Page entry point: sets up the browser runtime and FML's launch state, then starts Minecraft as the desktop launcher does. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            retro.Runtime.boot();
            RetroInjection.init(retro.Runtime.minecraftDir());
            selfTest();
        } catch (Throwable t) {
            t.printStackTrace();
            JS.fatal("Start-up failed: " + t);
            return;
        }
        // Exceptions that end a thread (the server thread, Netty's loops) must not vanish silently.
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            System.err.println("Exception in thread \"" + thread.getName() + "\":");
            retro.compat.SystemCompat.printStackTrace(error);
        });
        java.io.File mc = retro.Runtime.minecraftDir();
        // What net.minecraft.client.main.Main does after parsing its options (jopt-simple's reflection is not worth
        // carrying here): build the session and the client, name the thread, run.
        net.minecraft.util.Session session = new net.minecraft.util.Session(retro.Runtime.username(),
                "00000000-0000-0000-0000-000000000000", "0", "legacy");
        net.minecraft.client.Minecraft minecraft = new net.minecraft.client.Minecraft(session, 854, 480, false, false,
                mc, new java.io.File(mc, "assets"), new java.io.File(mc, "resourcepacks"), java.net.Proxy.NO_PROXY,
                "1.7.10", com.google.common.collect.HashMultimap.create(), "1.7.10");
        // ?server=<address> (host[:port], or a wss:// relay URL): join it once loaded, as the launcher's --server does
        String server = JS.config("server");
        if (server != null && !server.trim().isEmpty()) {
            minecraft.func_71367_a(server.trim(), 25565);
        }
        Thread.currentThread().setName("Client thread");
        minecraft.func_99999_d();
    }

    /** Startup probe (prints [retro-selftest] lines): how Gson sees Mantle Pulsar's config classes. */
    private static void selfTest() {
        try {
            Class<?> gc = Class.forName("mantle.pulsar.internal.Configuration$GsonConfig");
            for (java.lang.reflect.Field f : gc.getDeclaredFields()) {
                java.lang.reflect.Type t = f.getGenericType();
                System.out.println("[retro-selftest] GsonConfig." + f.getName() + " : " + f.getType() + " generic=" + t
                        + " (" + (t == null ? "null" : t.getClass().getName()) + ")");
            }
            Object cfg = new com.google.gson.Gson().fromJson(
                    "{\"CONFIG_VERSION\":1,\"modules\":{\"a\":{\"enabled\":true}}}", gc);
            java.lang.reflect.Field mf = gc.getDeclaredField("modules");
            mf.setAccessible(true);
            Object m = mf.get(cfg);
            System.out.println("[retro-selftest] modules map: " + (m == null ? "null" : m.getClass().getName()));
            if (m instanceof java.util.Map) {
                for (Object v : ((java.util.Map<?, ?>) m).values()) {
                    System.out.println("[retro-selftest] value: " + (v == null ? "null" : v.getClass().getName()) + " " + v);
                }
            }
        } catch (Throwable e) {
            System.out.println("[retro-selftest] failed: " + e);
            retro.compat.SystemCompat.printStackTrace(e, System.out);
        }
    }
}
