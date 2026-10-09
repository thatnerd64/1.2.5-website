package com.google.gson.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Gson's fallback for classes without a no-argument constructor. There is no sun.misc.Unsafe here, so this runs the
 * class's simplest constructor with default arguments (null, 0, empty arrays); Gson then fills in the fields.
 */
public abstract class UnsafeAllocator {
    public abstract <T> T newInstance(Class<T> c) throws Exception;

    public static UnsafeAllocator create() {
        return new UnsafeAllocator() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T newInstance(Class<T> c) throws Exception {
                Constructor<?>[] constructors = c.getDeclaredConstructors();
                Arrays.sort(constructors, new Comparator<Constructor<?>>() {
                    @Override
                    public int compare(Constructor<?> a, Constructor<?> b) {
                        return a.getParameterTypes().length - b.getParameterTypes().length;
                    }
                });
                Throwable last = null;
                for (Constructor<?> constructor : constructors) {
                    Class<?>[] types = constructor.getParameterTypes();
                    Object[] args = new Object[types.length];
                    for (int i = 0; i < types.length; i++) {
                        args[i] = defaultValue(types[i]);
                    }
                    try {
                        constructor.setAccessible(true);
                        return (T) constructor.newInstance(args);
                    } catch (Throwable t) {
                        last = t;
                    }
                }
                throw new UnsupportedOperationException("Cannot allocate " + c.getName()
                        + (last == null ? "" : " (" + last + ")"));
            }
        };
    }

    private static Object defaultValue(Class<?> type) {
        if (type == int.class) {
            return 0;
        } else if (type == boolean.class) {
            return false;
        } else if (type == long.class) {
            return 0L;
        } else if (type == double.class) {
            return 0.0;
        } else if (type == float.class) {
            return 0.0f;
        } else if (type == short.class) {
            return (short) 0;
        } else if (type == byte.class) {
            return (byte) 0;
        } else if (type == char.class) {
            return (char) 0;
        } else if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        }
        return null;
    }
}
