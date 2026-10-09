package org.lwjgl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public final class BufferUtils {
    private BufferUtils() {
    }

    public static ByteBuffer createByteBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static ShortBuffer createShortBuffer(int size) {
        return createByteBuffer(size << 1).asShortBuffer();
    }

    public static CharBuffer createCharBuffer(int size) {
        return createByteBuffer(size << 1).asCharBuffer();
    }

    public static IntBuffer createIntBuffer(int size) {
        return createByteBuffer(size << 2).asIntBuffer();
    }

    public static LongBuffer createLongBuffer(int size) {
        return createByteBuffer(size << 3).asLongBuffer();
    }

    public static FloatBuffer createFloatBuffer(int size) {
        return createByteBuffer(size << 2).asFloatBuffer();
    }

    public static DoubleBuffer createDoubleBuffer(int size) {
        return createByteBuffer(size << 3).asDoubleBuffer();
    }

    public static int getElementSizeExponent(Buffer buf) {
        if (buf instanceof ByteBuffer) {
            return 0;
        } else if (buf instanceof ShortBuffer || buf instanceof CharBuffer) {
            return 1;
        } else if (buf instanceof FloatBuffer || buf instanceof IntBuffer) {
            return 2;
        }
        return 3;
    }

    public static int getOffset(Buffer buffer) {
        return buffer.position() << getElementSizeExponent(buffer);
    }

    public static void zeroBuffer(ByteBuffer b) {
        for (int i = b.position(); i < b.limit(); i++) {
            b.put(i, (byte) 0);
        }
    }

    public static void zeroBuffer(FloatBuffer b) {
        for (int i = b.position(); i < b.limit(); i++) {
            b.put(i, 0);
        }
    }

    public static void zeroBuffer(IntBuffer b) {
        for (int i = b.position(); i < b.limit(); i++) {
            b.put(i, 0);
        }
    }
}
