package org.teavm.classlib.java.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** java.io.ObjectInputStream for the format written by {@link TObjectOutputStream}. */
public class TObjectInputStream extends InputStream implements TObjectInput {
    private final DataInputStream in;
    private final List<Object> handles = new ArrayList<>();
    private Object current;
    private Class<?> currentClass;

    public TObjectInputStream(InputStream in) throws IOException {
        this.in = new DataInputStream(in);
        if (this.in.readInt() != TObjectOutputStream.MAGIC) {
            throw new TStreamCorruptedException("invalid stream header (not written by this runtime)");
        }
    }

    protected TObjectInputStream() {
        this.in = new DataInputStream(new java.io.ByteArrayInputStream(new byte[0]));
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        int tag = in.readUnsignedByte();
        switch (tag) {
            case TObjectOutputStream.NULL:
                return null;
            case TObjectOutputStream.STRING: {
                int n = in.readInt();
                char[] c = new char[n];
                for (int i = 0; i < n; i++) {
                    c[i] = in.readChar();
                }
                return new String(c);
            }
            case TObjectOutputStream.INT:
                return in.readInt();
            case TObjectOutputStream.LONG:
                return in.readLong();
            case TObjectOutputStream.SHORT:
                return in.readShort();
            case TObjectOutputStream.BYTE:
                return in.readByte();
            case TObjectOutputStream.CHAR:
                return in.readChar();
            case TObjectOutputStream.FLOAT:
                return in.readFloat();
            case TObjectOutputStream.DOUBLE:
                return in.readDouble();
            case TObjectOutputStream.BOOLEAN:
                return in.readBoolean();
            case TObjectOutputStream.HANDLE:
                return handles.get(in.readInt());
            case TObjectOutputStream.ENUM: {
                Class<?> cls = Class.forName(in.readUTF());
                String name = in.readUTF();
                for (Object constant : cls.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equals(name)) {
                        return constant;
                    }
                }
                throw new InvalidClassException(cls.getName(), "no enum constant " + name);
            }
            case TObjectOutputStream.ARRAY:
                return readArray();
            case TObjectOutputStream.MAP: {
                Map<Object, Object> map = newMap(in.readUTF());
                handles.add(map);
                int n = in.readInt();
                for (int i = 0; i < n; i++) {
                    Object key = readObject();
                    map.put(key, readObject());
                }
                return map;
            }
            case TObjectOutputStream.COLLECTION: {
                Collection<Object> c = newCollection(in.readUTF());
                handles.add(c);
                int n = in.readInt();
                for (int i = 0; i < n; i++) {
                    c.add(readObject());
                }
                return c;
            }
            case TObjectOutputStream.OBJECT: {
                String name = in.readUTF();
                Class<?> cls = Class.forName(name);
                Object obj;
                try {
                    obj = cls.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException | RuntimeException e) {
                    throw new InvalidClassException(name, "no usable no-argument constructor");
                }
                handles.add(obj);
                readFields(obj, cls);
                return obj;
            }
            default:
                throw new TStreamCorruptedException("invalid type code " + tag);
        }
    }

    private void readFields(Object obj, Class<?> cls) throws IOException, ClassNotFoundException {
        Class<?> superclass = cls.getSuperclass();
        if (superclass != null && java.io.Serializable.class.isAssignableFrom(superclass)
                && !superclass.getName().startsWith("java.")) {
            readFields(obj, superclass);
        }
        Method hook = hook(cls, "readObject", java.io.ObjectInputStream.class);
        Object savedObject = current;
        Class<?> savedClass = currentClass;
        current = obj;
        currentClass = cls;
        try {
            if (hook != null) {
                hook.invoke(obj, this);
            } else {
                defaultReadObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw cause instanceof IOException ? (IOException) cause : new IOException(cause);
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            current = savedObject;
            currentClass = savedClass;
        }
    }

    public void defaultReadObject() throws IOException, ClassNotFoundException {
        if (current == null) {
            throw new TNotActiveException("not in call to readObject");
        }
        Map<String, Field> fields = new HashMap<>();
        for (Field f : serialFields(currentClass)) {
            fields.put(f.getName(), f);
        }
        int n = in.readInt();
        for (int i = 0; i < n; i++) {
            String name = in.readUTF();
            Object value = readObject();
            Field f = fields.get(name);
            if (f != null) {
                try {
                    f.set(current, value);
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    throw new InvalidClassException(currentClass.getName(), "cannot set field " + name);
                }
            }
        }
    }

    private Object readArray() throws IOException, ClassNotFoundException {
        String component = in.readUTF();
        int n = in.readInt();
        switch (component) {
            case "byte": {
                byte[] a = new byte[n];
                handles.add(a);
                in.readFully(a);
                return a;
            }
            case "int": {
                int[] a = new int[n];
                handles.add(a);
                for (int i = 0; i < n; i++) {
                    a[i] = in.readInt();
                }
                return a;
            }
            case "long": {
                long[] a = new long[n];
                handles.add(a);
                for (int i = 0; i < n; i++) {
                    a[i] = in.readLong();
                }
                return a;
            }
            case "short": {
                short[] a = new short[n];
                handles.add(a);
                for (int i = 0; i < n; i++) {
                    a[i] = in.readShort();
                }
                return a;
            }
            case "char": {
                char[] a = new char[n];
                handles.add(a);
                for (int i = 0; i < n; i++) {
                    a[i] = in.readChar();
                }
                return a;
            }
            case "float": {
                float[] a = new float[n];
                handles.add(a);
                for (int i = 0; i < n; i++) {
                    a[i] = in.readFloat();
                }
                return a;
            }
            case "double": {
                double[] a = new double[n];
                handles.add(a);
                for (int i = 0; i < n; i++) {
                    a[i] = in.readDouble();
                }
                return a;
            }
            case "boolean": {
                boolean[] a = new boolean[n];
                handles.add(a);
                for (int i = 0; i < n; i++) {
                    a[i] = in.readBoolean();
                }
                return a;
            }
            default: {
                Object[] a;
                if (component.equals("java.lang.String")) {
                    a = new String[n];
                } else if (component.startsWith("java.")) {
                    a = new Object[n];
                } else {
                    a = (Object[]) Array.newInstance(Class.forName(component), n);
                }
                handles.add(a);
                for (int i = 0; i < n; i++) {
                    a[i] = readObject();
                }
                return a;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> newMap(String name) {
        switch (name) {
            case "java.util.LinkedHashMap":
                return new java.util.LinkedHashMap<>();
            case "java.util.TreeMap":
                return new java.util.TreeMap<>();
            case "java.util.Hashtable":
                return new java.util.Hashtable<>();
            case "java.util.IdentityHashMap":
                return new java.util.IdentityHashMap<>();
            default:
                return new HashMap<>();
        }
    }

    private static Collection<Object> newCollection(String name) {
        switch (name) {
            case "java.util.HashSet":
                return new java.util.HashSet<>();
            case "java.util.LinkedHashSet":
                return new java.util.LinkedHashSet<>();
            case "java.util.TreeSet":
                return new java.util.TreeSet<>();
            case "java.util.LinkedList":
                return new java.util.LinkedList<>();
            case "java.util.ArrayDeque":
                return new java.util.ArrayDeque<>();
            case "java.util.Vector":
                return new java.util.Vector<>();
            case "java.util.Stack":
                return new java.util.Stack<>();
            default:
                return new ArrayList<>();
        }
    }

    /** The class's own private serialization hook, if it declares one. */
    static Method hook(Class<?> cls, String name, Class<?> param) {
        try {
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == 1
                        && m.getParameterTypes()[0] == param && !Modifier.isStatic(m.getModifiers())) {
                    m.setAccessible(true);
                    return m;
                }
            }
        } catch (RuntimeException e) {
            // No reflection metadata: fall back to default field serialization.
        }
        return null;
    }

    /** Non-static, non-transient fields declared by the class, in declaration order. */
    static Field[] serialFields(Class<?> cls) {
        List<Field> result = new ArrayList<>();
        for (Field f : cls.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (!Modifier.isStatic(mod) && !Modifier.isTransient(mod)) {
                f.setAccessible(true);
                result.add(f);
            }
        }
        return result.toArray(new Field[0]);
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        in.readFully(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        in.readFully(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return in.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return in.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return in.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return in.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return in.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return in.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return in.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return in.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return in.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return in.readDouble();
    }

    @Override
    @SuppressWarnings("deprecation")
    public String readLine() throws IOException {
        return in.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return in.readUTF();
    }

    public Object readUnshared() throws IOException, ClassNotFoundException {
        return readObject();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
