package cpw.mods.fml.common.functions;

import com.google.common.base.Function;
import cpw.mods.fml.common.versioning.ArtifactVersion;

/** Diagnostic replacement: names the offending class when a mod's requirement set holds something else. */
public class ArtifactVersionNameFunction implements Function<Object, String> {
    @Override
    public String apply(Object v) {
        if (v instanceof ArtifactVersion) {
            return ((ArtifactVersion) v).getLabel();
        }
        throw new IllegalStateException("A mod requirement that is not an ArtifactVersion: "
                + (v == null ? "null" : v.getClass().getName() + " " + v));
    }
}
