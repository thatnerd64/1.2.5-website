package retro;

import java.io.File;
import org.teavm.runtime.fs.VirtualFileSystemProvider;
import retro.fs.Persistence;
import retro.fs.RetroFileSystem;
import retro.input.ClipboardBridge;
import retro.input.Input;
import retro.rt.Pak;
import retro.rt.Resources;

/** Browser start-up: file system, downloads, saved data, input. Runs before Minecraft.main. */
public final class Runtime {
    public static final String HOME = "/home/player";
    private static String username = "Player";

    private Runtime() {
    }

    public static File minecraftDir() {
        return new File(HOME, ".minecraft");
    }

    public static String username() {
        return username;
    }

    public static void boot() {
        // Must happen before anything touches java.io.File (its static init binds the file system).
        RetroFileSystem fs = new RetroFileSystem(HOME);
        VirtualFileSystemProvider.setInstance(fs);

        System.setProperty("user.home", HOME);
        System.setProperty("user.dir", HOME);
        System.setProperty("user.name", "player");
        System.setProperty("os.name", "Linux");
        System.setProperty("os.arch", "x86");
        System.setProperty("os.version", "web");
        System.setProperty("java.version", "1.8.0_302");
        System.setProperty("java.vendor", "Oracle Corporation");
        System.setProperty("java.specification.version", "1.8");
        System.setProperty("java.specification.name", "Java Platform API Specification");
        System.setProperty("java.specification.vendor", "Oracle Corporation");
        System.setProperty("java.vm.specification.version", "1.8");
        System.setProperty("java.vm.specification.name", "Java Virtual Machine Specification");
        System.setProperty("java.vm.name", "Java HotSpot(TM) 64-Bit Server VM");
        System.setProperty("java.vm.version", "25.302-b08");
        System.setProperty("java.vm.vendor", "Oracle Corporation");
        System.setProperty("java.runtime.name", "Java(TM) SE Runtime Environment");
        System.setProperty("java.runtime.version", "1.8.0_302-b08");
        System.setProperty("java.class.version", "52.0");
        System.setProperty("java.io.tmpdir", "/tmp");
        System.setProperty("line.separator", "\n");
        // the rest of what a 64-bit HotSpot reports (mods read these unguarded: FTBLib's OS checks sun.arch.data.model)
        System.setProperty("file.separator", "/");
        System.setProperty("path.separator", ":");
        System.setProperty("sun.arch.data.model", "64");
        System.setProperty("sun.cpu.endian", "little");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        System.setProperty("java.home", "/usr/lib/jvm/java-8/jre");
        System.setProperty("java.class.path", "");
        System.setProperty("java.library.path", "");
        System.setProperty("java.ext.dirs", "");
        System.setProperty("java.vm.info", "mixed mode");
        System.setProperty("java.vendor.url", "http://java.oracle.com/");
        System.setProperty("user.language", "en");
        System.setProperty("user.country", "US");
        System.setProperty("user.timezone", "UTC");
        // Netty: no sun.misc.Unsafe, no Javassist, no native transports
        System.setProperty("io.netty.noUnsafe", "true");
        System.setProperty("io.netty.noJavassist", "true");
        System.setProperty("io.netty.tryReflectionSetAccessible", "false");
        System.setProperty("file.encoding", "UTF-8");

        String name = JS.config("username");
        if (name != null && !name.isBlank()) {
            username = name.trim();
        }

        JS.progress("Downloading", "Game assets", 0);
        Pak assets = Pak.load("assets.pak", (loaded, total) ->
                JS.progress("Downloading", "Game assets", total > 0 ? 0.6 * loaded / total : 0));
        Resources.init(assets);

        JS.progress("Downloading", "Modpack files", 0.6);
        Pak files = Pak.load("fs.pak", (loaded, total) ->
                JS.progress("Downloading", "Modpack files", 0.6 + (total > 0 ? 0.3 * loaded / total : 0)));
        String mcDir = HOME + "/.minecraft";
        for (Pak.Entry entry : files.entries().values()) {
            fs.addBaseFile(mcDir + "/" + entry.path, files, entry);
        }
        for (String dir : new String[] { "saves", "screenshots", "resourcepacks", "mods", "config", "logs", "assets" }) {
            fs.mkdirsQuietly(mcDir + "/" + dir);
        }

        JS.progress("Loading saved data", "", 0.92);
        String dbName = JS.config("storageName");
        Persistence.attach(fs, dbName != null ? dbName : "minecraft-1.7.10-ftb-infinity-evolved");

        retro.rt.FileUrls.install();
        ClipboardBridge.install();
        Input.install();
        Input.setActive(true);
        JS.progress("Starting Minecraft", "", 1);
        JS.started();
    }
}
