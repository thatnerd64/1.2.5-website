package cpw.mods.fml.common;

import java.io.File;
import java.net.MalformedURLException;

/**
 * Replacement for FML's URLClassLoader-based mod class loader. All mod classes are compiled into the page, so
 * "loading" a class is a {@code Class.forName} lookup; FML's discovery code is otherwise unchanged and still
 * scans the mods folder (which holds stub jars listing each mod's entries).
 */
public class ModClassLoader extends ClassLoader {
    public ModClassLoader() {
    }

    public ModClassLoader(ClassLoader parent) {
    }

    public void addFile(File modFile) throws MalformedURLException {
    }

    public File[] getParentSources() {
        return new File[] { new File(retro.Runtime.minecraftDir(), "bin/minecraft.jar") };
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
