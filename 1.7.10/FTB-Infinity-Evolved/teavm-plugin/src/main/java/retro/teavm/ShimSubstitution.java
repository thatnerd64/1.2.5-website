package retro.teavm;

import org.teavm.extension.spi.substitution.SimpleSubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionSink;

/**
 * Maps {@code javax.*} (and {@code com.sun.imageio.*}) classes to our shims ({@code org.teavm.classlib.javax.foo.TBar}), the same scheme TeaVM's
 * class library uses for {@code java.*}. {@code java.awt} shims need no rule: TeaVM's own policy finds them.
 */
public class ShimSubstitution extends SimpleSubstitutionPolicy {
    @Override
    public void contribute(SubstitutionSink sink) {
        sink.selectClasses(inPackage("javax", true).or(inPackage("org.w3c.dom", true))
                        .or(inPackage("org.xml.sax", true))
                        // (the JDK's internal PNG reader, which CraftStudio constructs directly)
                        .or(inPackage("com.sun.imageio", true)))
                .packagePrefix("org.teavm.classlib.")
                .simpleNamePrefix("T");
    }
}
