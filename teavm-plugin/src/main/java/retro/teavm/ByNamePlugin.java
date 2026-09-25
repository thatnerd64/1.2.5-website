package retro.teavm;

import java.util.List;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * Makes the classes in {@code byname.txt} (mod entry classes and class names found in string constants)
 * reachable, loadable by {@code Class.forName}, and instantiable.
 *
 * <p>TeaVM's reflection policies alone do not cover this: they only register classes that dependency analysis
 * already reached, and only propagate them to {@code forName} call sites seen after they were registered.
 */
public class ByNamePlugin implements TeaVMPlugin {
    @Override
    public void install(TeaVMHost host) {
        List<String> byName = GameClasses.byName();
        if (byName.isEmpty()) {
            return;
        }
        var set = new java.util.HashSet<>(byName);
        host.add(new AbstractDependencyListener() {
            private MethodDependency getName;

            @Override
            public void started(DependencyAgent agent) {
                getName = agent.linkMethod(new MethodReference(Class.class, "getName", String.class));
                getName.getVariable(0).propagate(agent.getType(ValueType.object("java.lang.Class")));
                getName.use();
                for (String cls : byName) {
                    agent.linkClass(cls);
                }
            }

            @Override
            public void classReached(DependencyAgent agent, String className) {
                if (set.contains(className)) {
                    getName.getVariable(0).getClassValueNode().propagate(agent.getType(ValueType.object(className)));
                }
            }

            @Override
            public void methodReached(DependencyAgent agent, MethodDependency method) {
                MethodReference ref = method.getReference();
                if (ref.getClassName().equals("java.lang.Class") && ref.getName().equals("forName")) {
                    for (String cls : byName) {
                        method.getResult().getClassValueNode().propagate(agent.getType(ValueType.object(cls)));
                    }
                }
            }
        });
    }
}
