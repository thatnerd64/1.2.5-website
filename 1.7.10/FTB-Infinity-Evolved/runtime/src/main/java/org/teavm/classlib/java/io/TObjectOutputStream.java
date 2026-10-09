package org.teavm.classlib.java.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * java.io.ObjectOutputStream. TeaVM has no Java serialization, so this writes a compact format of its own
 * (read back by {@link TObjectInputStream}): strings, boxed primitives, arrays, enums, java.util collections and
 * maps, and Serializable game classes field by field (honouring private writeObject hooks). Mods use it for small
 * save files such as Additional Pipes' teleport frequencies.
 */
public class TObjectOutputStream extends OutputStream implements TObjectOutput {
    static final int MAGIC = 0x52574F53;

    static final int NULL = 0;
    static final int STRING = 1;
    static final int INT = 2;
    static final int LONG = 3;
    static final int SHORT = 4;
    static final int BYTE = 5;
    static final int CHAR = 6;
    static final int FLOAT = 7;
    static final int DOUBLE = 8;
    static final int BOOLEAN = 9;
    static final int COLLECTION = 10;
    static final int MAP = 11;
    static final int OBJECT = 12;
    static final int HANDLE = 13;
    static final int ARRAY = 14;
    static final int ENUM = 15;

    private final DataOutputStream out;
    private final IdentityHashMap<Object, Integer> handles = new IdentityHashMap<>();
    private Object current;
    private Class<?> currentClass;

    public TObjectOutputStream(OutputStream out) throws IOException {
        this.out = new DataOutputStream(out);
        this.out.writeInt(MAGIC);
    }

    protected TObjectOutputStream() {
        this.out = new DataOutputStream(new java.io.ByteArrayOutputStream());
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        if (obj == null) {
            out.writeByte(NULL);
            return;
        }
        if (obj instanceof String) {
            out.writeByte(STRING);
            writeLongUTF((String) obj);
            return;
        }
        if (obj instanceof Integer) {
            out.writeByte(INT);
            out.writeInt((Integer) obj);
            return;
        }
        if (obj instanceof Long) {
            out.writeByte(LONG);
            out.writeLong((Long) obj);
            return;
        }
        if (obj instanceof Short) {
            out.writeByte(SHORT);
            out.writeShort((Short) obj);
            return;
        }
        if (obj instanceof Byte) {
            out.writeByte(BYTE);
            out.writeByte((Byte) obj);
            return;
        }
        if (obj instanceof Character) {
            out.writeByte(CHAR);
            out.writeChar((Character) obj);
            return;
        }
        if (obj instanceof Float) {
            out.writeByte(FLOAT);
            out.writeFloat((Float) obj);
            return;
        }
        if (obj instanceof Double) {
            out.writeByte(DOUBLE);
            out.writeDouble((Double) obj);
            return;
        }
        if (obj instanceof Boolean) {
            out.writeByte(BOOLEAN);
            out.writeBoolean((Boolean) obj);
            return;
        }
        Integer handle = handles.get(obj);
        if (handle != null) {
            out.writeByte(HANDLE);
            out.writeInt(handle);
            return;
        }
        Class<?> cls = obj.getClass();
        if (obj instanceof Enum) {
            out.writeByte(ENUM);
            out.writeUTF(((Enum<?>) obj).getDeclaringClass().getName());
            out.writeUTF(((Enum<?>) obj).name());
            return;
        }
        handles.put(obj, handles.size());
        if (cls.isArray()) {
            writeArray(obj);
        } else if (cls.getName().startsWith("java.") && obj instanceof Map) {
            out.writeByte(MAP);
            out.writeUTF(cls.getName());
            Map<?, ?> map = (Map<?, ?>) obj;
            out.writeInt(map.size());
            for (Map.Entry<?, ?> e : map.entrySet()) {
                writeObject(e.getKey());
                writeObject(e.getValue());
            }
        } else if (cls.getName().startsWith("java.") && obj instanceof Collection) {
            out.writeByte(COLLECTION);
            out.writeUTF(cls.getName());
            Collection<?> c = (Collection<?>) obj;
            out.writeInt(c.size());
            for (Object o : c) {
                writeObject(o);
            }
        } else if (obj instanceof Serializable && !cls.getName().startsWith("java.")) {
            out.writeByte(OBJECT);
            out.writeUTF(cls.getName());
            writeFields(obj, cls);
        } else {
            throw new TNotSerializableException(cls.getName());
        }
    }

    private void writeFields(Object obj, Class<?> cls) throws IOException {
        Class<?> superclass = cls.getSuperclass();
        if (superclass != null && Serializable.class.isAssignableFrom(superclass)
                && !superclass.getName().startsWith("java.")) {
            writeFields(obj, superclass);
        }
        Method hook = TObjectInputStream.hook(cls, "writeObject", java.io.ObjectOutputStream.class);
        Object savedObject = current;
        Class<?> savedClass = currentClass;
        current = obj;
        currentClass = cls;
        try {
            if (hook != null) {
                hook.invoke(obj, this);
            } else {
                defaultWriteObject();
            }
        } catch (IOException e) {
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

    public void defaultWriteObject() throws IOException {
        if (current == null) {
            throw new TNotActiveException("not in call to writeObject");
        }
        Field[] fields = TObjectInputStream.serialFields(currentClass);
        out.writeInt(fields.length);
        for (Field f : fields) {
            out.writeUTF(f.getName());
            try {
                writeObject(f.get(current));
            } catch (IllegalAccessException e) {
                throw new IOException(e);
            }
        }
    }

    private void writeArray(Object array) throws IOException {
        out.writeByte(ARRAY);
        Class<?> component = array.getClass().getComponentType();
        out.writeUTF(component.getName());
        int n = Array.getLength(array);
        out.writeInt(n);
        if (array instanceof byte[]) {
            out.write((byte[]) array);
        } else if (array instanceof int[]) {
            for (int v : (int[]) array) {
                out.writeInt(v);
            }
        } else if (array instanceof long[]) {
            for (long v : (long[]) array) {
                out.writeLong(v);
            }
        } else if (array instanceof short[]) {
            for (short v : (short[]) array) {
                out.writeShort(v);
            }
        } else if (array instanceof char[]) {
            for (char v : (char[]) array) {
                out.writeChar(v);
            }
        } else if (array instanceof float[]) {
            for (float v : (float[]) array) {
                out.writeFloat(v);
            }
        } else if (array instanceof double[]) {
            for (double v : (double[]) array) {
                out.writeDouble(v);
            }
        } else if (array instanceof boolean[]) {
            for (boolean v : (boolean[]) array) {
                out.writeBoolean(v);
            }
        } else {
            for (Object v : (Object[]) array) {
                writeObject(v);
            }
        }
    }

    private void writeLongUTF(String s) throws IOException {
        out.writeInt(s.length());
        for (int i = 0; i < s.length(); i++) {
            out.writeChar(s.charAt(i));
        }
    }

    public void writeUnshared(Object obj) throws IOException {
        writeObject(obj);
    }

    public void reset() throws IOException {
        handles.clear();
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        out.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        out.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        out.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        out.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        out.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        out.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        out.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        out.writeDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        out.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        out.writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        out.writeUTF(s);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
