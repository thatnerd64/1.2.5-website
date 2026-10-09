package cpw.mods.fml.relauncher;

import java.io.File;
import net.minecraft.launchwrapper.Launch;

/**
 * Seeds the state FMLTweaker and FMLLaunchHandler establish on desktop before Minecraft starts. It lives in FML's
 * package because that state is package-private.
 */
public final class RetroInjection {
    private RetroInjection() {
    }

    public static void init(File minecraftHome) {
        Launch.minecraftHome = minecraftHome;
        retro.compat.SystemCompat.setGameClassLoader(Launch.classLoader);
        Launch.assetsDir = new File(minecraftHome, "assets");
        FMLLaunchHandler.side = Side.CLIENT;
        FMLRelaunchLog.side = Side.CLIENT;
        FMLRelaunchLog.minecraftHome = minecraftHome;
        FMLInjectionData.build(minecraftHome, Launch.classLoader);
        seedDeobfuscationMaps();
        // The containers FML's and Forge's core plugins contribute (their IFMLLoadingPlugin.getModContainerClass()).
        replayCoremods(minecraftHome);
        // (recorded by the build: FML's and Forge's plus every coremod's, in the order the coremods loaded)
        byte[] recorded = retro.rt.Resources.read("retro/containers.txt");
        if (recorded != null) {
            for (String line : new String(recorded, java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                String name = line.trim();
                if (name.isEmpty()) {
                    continue;
                }
                try {
                    Class.forName(name);
                    FMLInjectionData.containers.add(name);
                } catch (Throwable t) {
                    System.err.println("Skipping the injected mod container " + name + " (not in this build)");
                }
            }
        } else {
            FMLInjectionData.containers.add("cpw.mods.fml.common.FMLContainer");
            FMLInjectionData.containers.add("net.minecraftforge.common.ForgeModContainer");
        }
        cpw.mods.fml.common.Loader.injectData(FMLInjectionData.data());
        // The classes were deobfuscated at build time and keep their SRG names, so reflection helpers need no mapping.
        net.minecraftforge.classloading.FMLForgePlugin.RUNTIME_DEOBF = false;
        File fml = new File(minecraftHome, "bin/forge-1.7.10-10.13.4.1614-1.7.10-universal.jar");
        net.minecraftforge.classloading.FMLForgePlugin.forgeLocation = fml;
        cpw.mods.fml.common.asm.FMLSanityChecker.fmlLocation = fml;
    }

    /**
     * On desktop FML loads the obf-to-SRG tables into FMLDeobfuscatingRemapper at launch; here the classes already carry
     * their SRG names, so the tables are empty (names map to themselves). Coremods read them reflectively and refuse to
     * work while they are null (CodeChickenCore's ObfMapping: "loaded too early").
     */
    private static void seedDeobfuscationMaps() {
        // CoreModManager's plugin list (CodeChickenCore inserts its deobfuscation plugin into it); the build time run
        // filled it, here nothing reads it
        try {
            java.lang.reflect.Field f = CoreModManager.class.getDeclaredField("loadPlugins");
            f.setAccessible(true);
            if (f.get(null) == null) {
                f.set(null, new java.util.ArrayList<>());
            }
        } catch (Throwable t) {
            System.err.println("CoreModManager.loadPlugins not seeded: " + t);
        }
        Object remapper = cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper.INSTANCE;
        // (only the raw tables: FML's own lookups short-circuit on its empty class map and never reach the others)
        for (String name : new String[] {"rawFieldMaps", "rawMethodMaps"}) {
            try {
                java.lang.reflect.Field f = remapper.getClass().getDeclaredField(name);
                f.setAccessible(true);
                if (f.get(remapper) == null) {
                    f.set(remapper, new java.util.HashMap<>());
                }
            } catch (Throwable t) {
                System.err.println("FMLDeobfuscatingRemapper." + name + " not seeded: " + t);
            }
        }
    }

    /**
     * The coremod plugins ran at build time. Their run-time state (the jar a mod container reports as its source,
     * is rebuilt here the way CoreModManager does it: transformers constructed, injectData, then the setup hook.
     */
    private static void replayCoremods(File minecraftHome) {
        byte[] recorded = retro.rt.Resources.read("retro/plugins.txt");
        if (recorded == null) {
            return;
        }
        java.util.List<Object> none = new java.util.ArrayList<>();
        for (String line : new String(recorded, java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
            String[] f = line.split("\t", -1);
            if (f.length < 3 || f[0].isEmpty() || f[0].equals("cpw.mods.fml.relauncher.FMLCorePlugin")
                    || f[0].equals("net.minecraftforge.classloading.FMLForgePlugin")) {
                continue;
            }
            File location = f[1].isEmpty() ? null : new File(new File(minecraftHome, "mods"), f[1]);
            try {
                IFMLLoadingPlugin plugin = (IFMLLoadingPlugin) Class.forName(f[0]).newInstance();
                // CoreModManager registers the plugin's ASM transformers (constructing them) before injectData; their
                // constructors publish the instance other code looks up (e.g. OpenModsClassTransformer.instance())
                if (f.length > 3) {
                    for (String transformer : f[3].split(",")) {
                        if (!transformer.trim().isEmpty()) {
                            try {
                                Class.forName(transformer.trim()).newInstance();
                            } catch (Throwable t) {
                                System.err.println("Transformer " + transformer + " not constructed: " + t);
                            }
                        }
                    }
                }
                java.util.HashMap<String, Object> data = new java.util.HashMap<>();
                data.put("mcLocation", minecraftHome);
                data.put("coremodList", none);
                data.put("runtimeDeobfuscationEnabled", false);
                data.put("coremodLocation", location);
                plugin.injectData(data);
            } catch (Throwable t) {
                System.err.println("Coremod " + f[0] + " could not be replayed:");
                retro.compat.SystemCompat.printStackTrace(t, System.err);
            }
            runSetupHook(f[2], minecraftHome, location);
        }
    }

    /**
     * A coremod's setup hook (IFMLCallHook), run after injectData as FMLPluginWrapper does. Many only prepare
     * transformation, but some load the mod's configuration into statics its mod container reads later
     * (CodeChickenCore's TweakTransformer.load). Dependency downloaders and FML's own certificate check are skipped.
     */
    private static void runSetupHook(String setupClass, File minecraftHome, File location) {
        if (setupClass.isEmpty() || setupClass.endsWith(".DepLoader")
                || setupClass.equals("cpw.mods.fml.common.asm.FMLSanityChecker")) {
            return;
        }
        try {
            IFMLCallHook hook = (IFMLCallHook) Class.forName(setupClass).newInstance();
            java.util.HashMap<String, Object> callData = new java.util.HashMap<>();
            callData.put("runtimeDeobfuscationEnabled", false);
            callData.put("mcLocation", minecraftHome);
            callData.put("classLoader", Launch.classLoader);
            callData.put("coremodLocation", location);
            callData.put("deobfuscationFileName", FMLInjectionData.debfuscationDataName());
            hook.injectData(callData);
            hook.call();
        } catch (Throwable t) {
            System.err.println("Setup hook " + setupClass + " failed (continuing):");
            retro.compat.SystemCompat.printStackTrace(t, System.err);
        }
    }
}
