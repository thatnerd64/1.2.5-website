package cpw.mods.fml.client;

import cpw.mods.fml.common.FMLContainerHolder;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.discovery.JarDiscoverer;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import retro.glue.ModAssets;

/**
 * A mod's resource pack, served from the packed classpath store (see ModAssets). Implements the interface directly:
 * the transformed AbstractResourcePack no longer lines up with it for javac (lost throws clauses).
 */
public class FMLFolderResourcePack implements IResourcePack, FMLContainerHolder {
    private final ModContainer container;
    private final ModAssets assets;

    public FMLFolderResourcePack(ModContainer container) {
        this.container = container;
        this.assets = new ModAssets(container.getSource(), container.getName(),
                JarDiscoverer.domainsFor(container.getSource().getName()));
    }

    private static String path(ResourceLocation location) {
        return "assets/" + location.func_110624_b() + "/" + location.func_110623_a();
    }

    @Override
    public InputStream func_110590_a(ResourceLocation location) {
        try {
            return assets.open(path(location));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean func_110589_b(ResourceLocation location) {
        return assets.has(path(location));
    }

    @Override
    public Set func_110587_b() {
        return assets.domains();
    }

    @Override
    public IMetadataSection func_135058_a(IMetadataSerializer serializer, String section) {
        try (java.io.Reader reader = new java.io.InputStreamReader(assets.open("pack.mcmeta"),
                java.nio.charset.StandardCharsets.UTF_8)) {
            return serializer.func_110503_a(section, new com.google.gson.JsonParser().parse(reader).getAsJsonObject());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public BufferedImage func_110586_a() {
        try {
            return ImageIO.read(assets.open(container.getMetadata().logoFile));
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String func_130077_b() {
        return "FMLFileResourcePack:" + container.getName();
    }

    @Override
    public ModContainer getFMLContainer() {
        return container;
    }
}
