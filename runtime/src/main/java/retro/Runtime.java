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
        System.setProperty("java.version", "1.6.0_45");
        System.setProperty("java.vendor", "Oracle Corporation");
        System.setProperty("java.io.tmpdir", "/tmp");
        System.setProperty("line.separator", "\n");
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
        for (String dir : new String[] { "saves", "screenshots", "texturepacks", "stats", "mods", "config" }) {
            fs.mkdirsQuietly(mcDir + "/" + dir);
        }

        JS.progress("Loading saved data", "", 0.92);
        String dbName = JS.config("storageName");
        Persistence.attach(fs, dbName != null ? dbName : "minecraft-1.2.5-full-retro");

        ClipboardBridge.install();
        Input.install();
        Input.setActive(true);
        JS.progress("Starting Minecraft", "", 1);
        JS.started();
    }
}
