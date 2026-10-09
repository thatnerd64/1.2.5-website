package retro.aot;

import java.io.File;
import java.util.List;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Makes {@link Dump} the launch target in place of {@code net.minecraft.client.main.Main}. It must be the first
 * {@code --tweakClass}: LaunchWrapper takes the launch target from the first tweaker, while the second one
 * ({@code FMLTweaker}) still installs Forge's transformers and the coremods exactly as the desktop game does.
 */
public final class DumpTweaker implements ITweaker {
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        classLoader.addTransformerExclusion("retro.aot.");
        classLoader.addClassLoaderExclusion("retro.aot.");
    }

    @Override
    public String getLaunchTarget() {
        return "retro.aot.Dump";
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}
