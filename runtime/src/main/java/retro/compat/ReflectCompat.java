package retro.compat;

import java.lang.reflect.Field;

/** java.lang.reflect.Field primitive accessors, which TeaVM's Field lacks (see retro.build.Patches). */
public final class ReflectCompat {
    private ReflectCompat() {
    }

    public static int getInt(Field f, Object o) throws IllegalAccessException {
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
}
