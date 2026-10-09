package cpw.mods.fml.common.eventhandler;

import cpw.mods.fml.common.ModContainer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Replacement for Forge's event listener. The original generates one class per handler method with ASM and defines
 * it at run time, which a compiled page cannot do; the generated class only casts and calls the method, so this
 * calls it reflectively (handler methods are reflectable because they carry a run-time annotation).
 */
public class ASMEventHandler implements IEventListener {
    private final Object target;
    private final Method method;
    private final boolean isStatic;
    private final SubscribeEvent subInfo;
    private final ModContainer owner;
    private final String readable;

    public ASMEventHandler(Object target, Method method, ModContainer owner) throws Exception {
        this.owner = owner;
        this.target = target;
        this.method = method;
        this.isStatic = Modifier.isStatic(method.getModifiers());
        this.subInfo = method.getAnnotation(SubscribeEvent.class);
        this.readable = "ASM: " + target + " " + method.getName();
        try {
            method.setAccessible(true);
        } catch (RuntimeException e) {
            // public handlers do not need it
        }
    }

    @Override
    public void invoke(Event event) {
        if (event.isCancelable() && event.isCanceled() && !subInfo.receiveCanceled()) {
            return;
        }
        try {
            method.invoke(isStatic ? null : target, event);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public EventPriority getPriority() {
        return subInfo.priority();
    }

    public Class<?> createWrapper(Method callback) {
        throw new UnsupportedOperationException("handlers are called reflectively");
    }

    @Override
    public String toString() {
        return readable;
    }
}
