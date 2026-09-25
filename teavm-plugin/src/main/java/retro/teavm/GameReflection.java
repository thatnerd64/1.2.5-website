package retro.teavm;

import java.util.HashSet;
import java.util.Set;
import org.teavm.extension.introspect.IntrospectField;
import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.extension.spi.reflection.SimpleReflectionPolicy;

/**
 * Reflection available to game and mod code:
 * <ul>
 *   <li>every field (ModLoader/FML and mods read private fields by index or name),</li>
 *   <li>every constructor (entities, tile entities and packets are instantiated reflectively),</li>
 *   <li>methods whose name appears in some string constant (reflective calls by name).</li>
 * </ul>
 * Making every method reflectable would make all code reachable and exhaust the compiler's memory.
 */
public class GameReflection extends SimpleReflectionPolicy {
    @Override
    protected void setup() {
        var all = GameClasses.all();
        Set<String> methods = new HashSet<>(GameClasses.reflectMethods());
        selectClasses(c -> c != null && all.contains(c.name()))
                .reflectableMembers(m -> {
                    if (m instanceof IntrospectField) {
                        return true;
                    }
                    if (m instanceof IntrospectMethod) {
                        IntrospectMethod method = (IntrospectMethod) m;
                        return method.name().equals("<init>")
                                || methods.contains(method.declaringClass().name() + " " + method.name());
                    }
                    return false;
                });
    }
}
