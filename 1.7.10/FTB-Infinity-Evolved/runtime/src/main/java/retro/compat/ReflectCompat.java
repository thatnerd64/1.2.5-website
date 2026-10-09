package retro.compat;

import java.lang.reflect.Field;

/** java.lang.reflect.Field primitive accessors, which TeaVM's Field lacks (see retro.build.Patches). */
public final class ReflectCompat {
    private ReflectCompat() {
    }

    /**
     * Stands in for java.lang.reflect.Field's private "modifiers": mods clear FINAL through it to write static final
     * fields (Immersive Engineering grows Potion.potionTypes that way). TeaVM's Field.set ignores final, so writes
     * to the stand-in are dropped and reads return the target field's modifiers.
     */
    public static int modifiersStandIn;

    public static Field getDeclaredField(Class<?> cls, String name) throws NoSuchFieldException {
        if (cls == Field.class && name.equals("modifiers")) {
            return ReflectCompat.class.getDeclaredField("modifiersStandIn");
        }
        return cls.getDeclaredField(name);
    }

    /**
     * Field.getType: TeaVM reports null for a field whose type was compiled out (never instantiated or referenced
     * elsewhere); code that lists fields and compares types (Immersive Engineering on Potion) needs a class.
     */
    public static Class<?> fieldType(Field f) {
        Class<?> type = f.getType();
        if (debugReflect == null) {
            String v = retro.JS.config("debugReflect");
            debugReflect = v != null ? v : "";
        }
        if (!debugReflect.isEmpty() && f.getDeclaringClass().getName().equals(debugReflect)) {
            // ?debugreflect=<class> on the page: what reflection reports for that class's fields
            System.err.println("[retro] " + debugReflect + "." + f.getName() + " : " + type);
        }
        return type != null ? type : Object.class;
    }

    private static String debugReflect;

    private static boolean isModifiersStandIn(Field f) {
        return f.getDeclaringClass() == ReflectCompat.class && f.getName().equals("modifiersStandIn");
    }

    public static int getInt(Field f, Object o) throws IllegalAccessException {
        if (isModifiersStandIn(f) && o instanceof Field) {
            return ((Field) o).getModifiers();
        }
        Object v = f.get(o);
        return v instanceof Character ? (Character) v : ((Number) v).intValue();
    }

    public static long getLong(Field f, Object o) throws IllegalAccessException {
        Object v = f.get(o);
        return v instanceof Character ? (Character) v : ((Number) v).longValue();
    }

    public static float getFloat(Field f, Object o) throws IllegalAccessException {
        Object v = f.get(o);
        return v instanceof Character ? (Character) v : ((Number) v).floatValue();
    }

    public static double getDouble(Field f, Object o) throws IllegalAccessException {
        Object v = f.get(o);
        return v instanceof Character ? (Character) v : ((Number) v).doubleValue();
    }

    public static boolean getBoolean(Field f, Object o) throws IllegalAccessException {
        return (Boolean) f.get(o);
    }

    public static byte getByte(Field f, Object o) throws IllegalAccessException {
        return ((Number) f.get(o)).byteValue();
    }

    public static short getShort(Field f, Object o) throws IllegalAccessException {
        return ((Number) f.get(o)).shortValue();
    }

    public static char getChar(Field f, Object o) throws IllegalAccessException {
        return (Character) f.get(o);
    }

    private static Object convert(Field f, double v) {
        Class<?> t = f.getType();
        if (t == int.class) {
            return (int) v;
        } else if (t == long.class) {
            return (long) v;
        } else if (t == float.class) {
            return (float) v;
        } else if (t == double.class) {
            return v;
        } else if (t == short.class) {
            return (short) v;
        } else if (t == byte.class) {
            return (byte) v;
        } else if (t == char.class) {
            return (char) v;
        }
        return v;
    }

    public static void setInt(Field f, Object o, int v) throws IllegalAccessException {
        if (isModifiersStandIn(f)) {
            return;
        }
        f.set(o, convert(f, v));
    }

    public static void setLong(Field f, Object o, long v) throws IllegalAccessException {
        f.set(o, f.getType() == long.class ? (Object) v : convert(f, v));
    }

    public static void setFloat(Field f, Object o, float v) throws IllegalAccessException {
        f.set(o, convert(f, v));
    }

    public static void setDouble(Field f, Object o, double v) throws IllegalAccessException {
        f.set(o, convert(f, v));
    }

    public static void setBoolean(Field f, Object o, boolean v) throws IllegalAccessException {
        f.set(o, v);
    }

    public static void setByte(Field f, Object o, byte v) throws IllegalAccessException {
        f.set(o, convert(f, v));
    }

    public static void setShort(Field f, Object o, short v) throws IllegalAccessException {
        f.set(o, convert(f, v));
    }

    public static void setChar(Field f, Object o, char v) throws IllegalAccessException {
        f.set(o, f.getType() == char.class ? (Object) v : convert(f, v));
    }

    private static final Object[] NO_ARGS = new Object[0];

    /** Method.invoke: the JDK accepts null for "no arguments" (Gson, Guava and mods rely on it); TeaVM does not. */
    public static Object methodInvoke(java.lang.reflect.Method method, Object target, Object[] args)
            throws IllegalAccessException, java.lang.reflect.InvocationTargetException {
        return method.invoke(target, args == null ? NO_ARGS : args);
    }

    /** Constructor.newInstance: an abstract class cannot be instantiated (TeaVM has no constructor to call). */
    public static Object constructorNewInstance(java.lang.reflect.Constructor<?> ctor, Object[] args)
            throws InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
        if (java.lang.reflect.Modifier.isAbstract(ctor.getDeclaringClass().getModifiers())) {
            throw new InstantiationException(ctor.getDeclaringClass().getName());
        }
        try {
            return ctor.newInstance(args == null ? NO_ARGS : args);
        } catch (java.lang.reflect.InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw e;
        } catch (Throwable t) {
            // a constructor the compiler did not include (no reflection data): say which one
            throw new InstantiationException(ctor.getDeclaringClass().getName() + ": " + t);
        }
    }

    /** Class.newInstance, with the same check. */
    @SuppressWarnings("deprecation")
    private static java.util.Map<String, String> linkFailures;

    /**
     * Class.forName(name): like a JVM, initializing a class that fails bytecode verification (it needs a class of
     * an absent mod) throws NoClassDefFoundError; mods use that to detect optional dependencies. The build keeps
     * such classes (a class literal naming one must still work) and lists them in retro/linkfail.txt.
     */
    public static Class<?> forName(String name) throws ClassNotFoundException {
        Class<?> cls = lookup(name);
        checkLinkable(name);
        return cls;
    }

    public static Class<?> forName(String name, boolean initialize, ClassLoader loader)
            throws ClassNotFoundException {
        Class<?> cls = lookup(name);
        if (initialize) {
            checkLinkable(name);
        }
        return cls;
    }

    /** Class.forName, plus array names ("[B", "[Ljava.lang.String;"), which TeaVM's lookup does not know. */
    private static Class<?> lookup(String name) throws ClassNotFoundException {
        if (!name.startsWith("[")) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                String alias = GeneratedClasses.alias(name);
                if (alias == null) {
                    throw e;
                }
                return Class.forName(alias);
            }
        }
        Class<?> component;
        String rest = name.substring(1);
        switch (rest.isEmpty() ? ' ' : rest.charAt(0)) {
            case 'Z': component = boolean.class; break;
            case 'B': component = byte.class; break;
            case 'C': component = char.class; break;
            case 'S': component = short.class; break;
            case 'I': component = int.class; break;
            case 'J': component = long.class; break;
            case 'F': component = float.class; break;
            case 'D': component = double.class; break;
            case '[': component = lookup(rest); break;
            case 'L':
                if (!rest.endsWith(";")) {
                    throw new ClassNotFoundException(name);
                }
                component = lookup(rest.substring(1, rest.length() - 1));
                break;
            default:
                throw new ClassNotFoundException(name);
        }
        if (component.isPrimitive() && rest.length() != 1) {
            throw new ClassNotFoundException(name);
        }
        return java.lang.reflect.Array.newInstance(component, 0).getClass();
    }

    private static void checkLinkable(String name) {
        if (linkFailures == null) {
            linkFailures = new java.util.HashMap<>();
            byte[] list = retro.rt.Resources.read("retro/linkfail.txt");
            if (list != null) {
                for (String line : new String(list, java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                    int tab = line.indexOf('\t');
                    if (tab > 0) {
                        linkFailures.put(line.substring(0, tab), line.substring(tab + 1));
                    }
                }
            }
        }
        String missing = linkFailures.get(name);
        if (missing != null) {
            throw new NoClassDefFoundError(missing);
        }
    }

    /**
     * Class.getMethod with the JDK's lookup order: the class's own public methods, then its superclasses', then its
     * interfaces'. TeaVM 0.15 lets a superclass declaration replace the subclass's override (for void methods
     * always), so getDeclaringClass() reported the superclass; Railcraft calls a module's preInit only if the module
     * overrides it, and ran none.
     */
    public static java.lang.reflect.Method getMethod(Class<?> cls, String name, Class<?>[] params)
            throws NoSuchMethodException {
        java.lang.reflect.Method m = findPublicMethod(cls, name, params == null ? new Class<?>[0] : params);
        if (m == null) {
            throw new NoSuchMethodException(cls.getName() + "." + name);
        }
        return m;
    }

    private static java.lang.reflect.Method findPublicMethod(Class<?> cls, String name, Class<?>[] params) {
        java.lang.reflect.Method best = null;
        for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(m.getModifiers()) && m.getName().equals(name)
                    && java.util.Arrays.equals(m.getParameterTypes(), params)
                    && (best == null || best.getReturnType().isAssignableFrom(m.getReturnType()))) {
                best = m;
            }
        }
        if (best != null) {
            return best;
        }
        if (!cls.isInterface() && cls.getSuperclass() != null) {
            best = findPublicMethod(cls.getSuperclass(), name, params);
            if (best != null) {
                return best;
            }
        }
        for (Class<?> itf : cls.getInterfaces()) {
            best = findPublicMethod(itf, name, params);
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private static java.util.Set<String> reportedInstantiation;

    public static Object classNewInstance(Class<?> cls) throws InstantiationException, IllegalAccessException {
        if (java.lang.reflect.Modifier.isAbstract(cls.getModifiers()) || cls.isInterface()) {
            throw new InstantiationException(cls.getName());
        }
        try {
            return cls.newInstance();
        } catch (InstantiationException e) {
            // Mods often swallow this (Railcraft's modules); a class the build did not make instantiable by
            // reflection shows up here
            if (reportedInstantiation == null) {
                reportedInstantiation = new java.util.HashSet<>();
            }
            if (reportedInstantiation.add(cls.getName())) {
                System.err.println("[retro] Class.newInstance failed for " + cls.getName()
                        + " (not instantiable by reflection in this build)");
            }
            throw e;
        }
    }
}
