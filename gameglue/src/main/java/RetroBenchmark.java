import net.minecraft.client.Minecraft;

/**
 * Benchmark mode ({@code ?benchmark=SEED}, see {@link retro.Benchmark}). Minecraft.runGameLoop calls {@link #frame}
 * at the start of every frame (added at build time, see Patches.benchmarkHook). On the title screen it creates a
 * fresh world from the seed, as Create New World would; in game it reports the entity count and, while the page
 * asks for it, walks the player: forward, weaving 45 degrees left and right, jumping over obstacles and out of water,
 * respawning after death. Everything else is the normal game, so a run measures what a player gets.
 * In the default package because the game's obfuscated classes live there.
 *
 * <p>Names: Minecraft.s currentScreen, .f theWorld, .h thePlayer, .A gameSettings, .c playerController,
 * .a(vp) displayGuiScreen, .a(String, String, fj) startWorld, .c() getSaveLoader; xt GuiMainMenu, uy GuiGameOver;
 * fj WorldSettings, vx WorldType (b DEFAULT); kb ISaveFormat (c deleteWorldDirectory, d flushCache);
 * aes PlayerControllerSP; vq.ag() respawnPlayer; nn Entity (o/p/q posX/Y/Z, u rotationYaw, A isCollidedHorizontally,
 * H() isInWater); hu GameSettings (n keyBindForward, r keyBindJump, e renderDistance, j fancyGraphics,
 * k ambientOcclusion, l clouds, C difficulty);
 * afu.e KeyBinding.pressed; xd.b World.loadedEntityList, xd.v chunkProvider, ca.c() IChunkProvider.makeString.
 */
public final class RetroBenchmark {
    private static final String FOLDER = "retro-benchmark";
    private static final double WEAVE_DEGREES = 45;
    private static final double WEAVE_SECONDS = 8;

    /** -1 not checked yet, 0 off, 1 waiting for the title screen, 2 world started. */
    private static int stage = -1;
    private static boolean walking;
    private static float baseYaw;
    private static double walkSeconds;
    private static long lastFrame;
    private static long lastReport;

    private RetroBenchmark() {
    }

    public static void frame(Minecraft mc) {
        if (stage == 0) {
            return;
        }
        if (stage < 0) {
            stage = retro.Benchmark.enabled() ? 1 : 0;
            if (stage == 0) {
                return;
            }
            retro.Benchmark.state("title");
        }
        if (stage == 1) {
            // The title screen shows once every mod has loaded.
            if (mc.s instanceof xt) {
                stage = 2;
                startWorld(mc);
            }
            return;
        }
        vq player = mc.h;
        xd world = mc.f;
        if (player == null || world == null) {
            return;
        }
        if (mc.s instanceof uy) {
            player.ag();
            mc.a((vp) null);
        }
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : Math.min(0.25, (now - lastFrame) / 1e9);
        lastFrame = now;
        walk(mc, player, retro.Benchmark.walking() && mc.s == null, dt);
        if (now - lastReport > 250_000_000L) {
            lastReport = now;
            retro.Benchmark.state(mc.s == null ? "ingame" : "menu");
            retro.Benchmark.stats(world.b.size(), world.v.c(), player.o, player.p, player.q, settings(mc.A));
        }
    }

    private static void startWorld(Minecraft mc) {
        // Seed as GuiCreateWorld reads it: a non-zero number as is, anything else by its hash code.
        String text = retro.Benchmark.seed();
        long seed;
        try {
            seed = Long.parseLong(text);
            if (seed == 0) {
                seed = text.hashCode();
            }
        } catch (NumberFormatException e) {
            seed = text.hashCode();
        }
        applySettings(mc.A);
        // Always a fresh world, so every run generates and measures the same one.
        kb saves = mc.c();
        saves.d();
        saves.c(FOLDER);
        mc.c = new aes(mc);
        vx.b.onGUICreateWorldPress();
        mc.a(FOLDER, "Benchmark", new fj(seed, 0, true, false, vx.b));
        mc.a((vp) null);
    }

    /**
     * Video settings and difficulty asked for in the page URL (render=far|normal|short|tiny, graphics=fancy|fast,
     * smooth=on|off, clouds=on|off, difficulty=peaceful|easy|normal|hard); unset ones keep the game's options. Set
     * before the world loads, which is when the game reads them, and not saved.
     */
    private static void applySettings(hu options) {
        int render = choice("render", "far", "normal", "short", "tiny");
        if (render >= 0) {
            options.e = render;
        }
        int graphics = choice("graphics", "fast", "fancy");
        if (graphics >= 0) {
            options.j = graphics == 1;
        }
        int smooth = choice("smooth", "off", "on");
        if (smooth >= 0) {
            options.k = smooth == 1;
        }
        int clouds = choice("clouds", "off", "on");
        if (clouds >= 0) {
            options.l = clouds == 1;
        }
        int difficulty = choice("difficulty", "peaceful", "easy", "normal", "hard");
        if (difficulty >= 0) {
            options.C = difficulty;
        }
    }

    private static String settings(hu options) {
        return "render " + new String[] { "far", "normal", "short", "tiny" }[options.e & 3]
                + ", graphics " + (options.j ? "fancy" : "fast") + ", smooth lighting " + (options.k ? "on" : "off")
                + ", clouds " + (options.l ? "on" : "off")
                + ", difficulty " + new String[] { "peaceful", "easy", "normal", "hard" }[options.C & 3];
    }

    private static int choice(String option, String... values) {
        String value = retro.Benchmark.option(option);
        for (int i = 0; value != null && i < values.length; i++) {
            if (values[i].equalsIgnoreCase(value)) {
                return i;
            }
        }
        return -1;
    }

    private static void walk(Minecraft mc, vq player, boolean on, double dt) {
        if (on) {
            if (!walking) {
                baseYaw = player.u;
                walkSeconds = 0;
            }
            walkSeconds += dt;
            player.u = baseYaw + (float) (WEAVE_DEGREES * Math.sin(walkSeconds * 2 * Math.PI / WEAVE_SECONDS));
            mc.A.n.e = true;
            mc.A.r.e = player.A || player.H();
        } else if (walking) {
            mc.A.n.e = false;
            mc.A.r.e = false;
        }
        walking = on;
    }
}
