package cpw.mods.fml.common;

import cpw.mods.fml.common.asm.transformers.ModAPITransformer;
import cpw.mods.fml.common.discovery.ASMDataTable;
import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;

/**
 * Replacement for FML's mod class loader. All mod classes are compiled into the page, so "loading" a class is a
 * {@code Class.forName} lookup. Mod discovery is unchanged: it scans the stub jars in the mods folder and the
 * class path sources named here.
 */
public class ModClassLoader extends ClassLoader {
    private static final List<String> STANDARD_LIBRARIES = java.util.Arrays.asList("jinput.jar", "lwjgl.jar",
            "lwjgl_util.jar", "rt.jar");

    public ModClassLoader() {
    }

    public ModClassLoader(ClassLoader parent) {
    }

    public void addFile(File modFile) throws MalformedURLException {
    }

    public File[] getParentSources() {
        return new File[] { new File(retro.Runtime.minecraftDir(), "bin/minecraft.jar") };
    }

    public List<String> getDefaultLibraries() {
        return STANDARD_LIBRARIES;
    }

    public void clearNegativeCacheFor(Set<String> classList) {
    }

    public ModAPITransformer addModAPITransformer(ASMDataTable dataTable) {
        return null;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
