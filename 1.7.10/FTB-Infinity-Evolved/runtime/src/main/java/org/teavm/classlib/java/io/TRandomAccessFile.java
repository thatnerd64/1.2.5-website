package org.teavm.classlib.java.io;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;

/**
 * java.io.RandomAccessFile. Replaces TeaVM's version, which does not create a missing file in "rw" mode as the
 * JDK does (Minecraft's RegionFile relies on that to create region files).
 */
public class TRandomAccessFile implements java.io.DataInput, java.io.DataOutput, java.io.Closeable {
    private VirtualFileAccessor accessor;
    private final boolean writable;
    private final byte[] buf = new byte[8];

    public TRandomAccessFile(String name, String mode) throws FileNotFoundException {
        this(new File(name), mode);
    }

    public TRandomAccessFile(File file, String mode) throws FileNotFoundException {
        switch (mode) {
            case "r":
                writable = false;
                break;
            case "rw":
            case "rws":
            case "rwd":
                writable = true;
                break;
            default:
                throw new IllegalArgumentException("Illegal mode \"" + mode
                        + "\" must be one of \"r\", \"rw\", \"rws\", or \"rwd\"");
        }
        if (writable && !file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new FileNotFoundException(file.getPath() + " (" + e.getMessage() + ")");
            }
        }
        VirtualFile vf = ((TFile) (Object) file).findVirtualFile();
        if (vf == null || !vf.isFile()) {
            throw new FileNotFoundException(file.getPath() + " (No such file or directory)");
        }
        accessor = vf.createAccessor(true, writable, false);
        if (accessor == null) {
            throw new FileNotFoundException(file.getPath() + " (Permission denied)");
        }
    }

    private VirtualFileAccessor open() throws IOException {
        if (accessor == null) {
            throw new IOException("Stream Closed");
        }
        return accessor;
    }

    private void checkWritable() throws IOException {
        if (!writable) {
            throw new IOException("File opened read-only");
        }
    }

    @Override
    public void close() throws IOException {
        if (accessor != null) {
            accessor.close();
            accessor = null;
        }
    }

    public int read() throws IOException {
        return open().read(buf, 0, 1) <= 0 ? -1 : buf[0] & 0xFF;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        int n = open().read(b, off, len);
        return n <= 0 ? -1 : n;
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        int done = 0;
        while (done < len) {
            int n = read(b, off + done, len - done);
            if (n < 0) {
                throw new EOFException();
            }
            done += n;
        }
    }

    @Override
    public int skipBytes(int n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        long pos = getFilePointer();
        long len = length();
        long target = Math.min(len, pos + n);
        seek(target);
        return (int) (target - pos);
    }

    public long getFilePointer() throws IOException {
        return open().tell();
    }

    public void seek(long pos) throws IOException {
        if (pos < 0) {
            throw new IOException("Negative seek offset");
        }
        open().seek((int) pos);
    }

    public long length() throws IOException {
        return open().size();
    }

    public void setLength(long newLength) throws IOException {
        checkWritable();
        VirtualFileAccessor a = open();
        int pos = a.tell();
        a.resize((int) newLength);
        if (pos > newLength) {
            a.seek((int) newLength);
        }
    }

    private void readBuf(int n) throws IOException {
        readFully(buf, 0, n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return readUnsignedByte() != 0;
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) readUnsignedByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        int b = read();
        if (b < 0) {
            throw new EOFException();
        }
        return b;
    }

    @Override
    public short readShort() throws IOException {
        readBuf(2);
        return (short) ((buf[0] & 0xFF) << 8 | (buf[1] & 0xFF));
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return readShort() & 0xFFFF;
    }

    @Override
    public char readChar() throws IOException {
        return (char) readUnsignedShort();
    }

    @Override
    public int readInt() throws IOException {
        readBuf(4);
        return (buf[0] & 0xFF) << 24 | (buf[1] & 0xFF) << 16 | (buf[2] & 0xFF) << 8 | (buf[3] & 0xFF);
    }

    @Override
    public long readLong() throws IOException {
        long hi = readInt();
        return hi << 32 | (readInt() & 0xFFFFFFFFL);
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c = read();
        if (c < 0) {
            return null;
        }
        while (c >= 0 && c != '\n') {
            if (c == '\r') {
                long pos = getFilePointer();
                if (read() != '\n') {
                    seek(pos);
                }
                break;
            }
            sb.append((char) c);
            c = read();
        }
        return sb.toString();
    }

    @Override
    public String readUTF() throws IOException {
        int len = readUnsignedShort();
        byte[] bytes = new byte[len];
        readFully(bytes);
        char[] out = new char[len];
        int count = 0;
        int i = 0;
        while (i < len) {
            int a = bytes[i++] & 0xFF;
            if (a < 0x80) {
                out[count++] = (char) a;
            } else if ((a & 0xE0) == 0xC0) {
                if (i >= len) {
                    throw new UTFDataFormatException("malformed input");
                }
                out[count++] = (char) ((a & 0x1F) << 6 | (bytes[i++] & 0x3F));
            } else if ((a & 0xF0) == 0xE0) {
                if (i + 1 >= len) {
                    throw new UTFDataFormatException("malformed input");
                }
                out[count++] = (char) ((a & 0x0F) << 12 | (bytes[i++] & 0x3F) << 6 | (bytes[i++] & 0x3F));
            } else {
                throw new UTFDataFormatException("malformed input");
            }
        }
        return new String(out, 0, count);
    }

    public void write(int b) throws IOException {
        buf[0] = (byte) b;
        write(buf, 0, 1);
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        checkWritable();
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        open().write(b, off, len);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    @Override
    public void writeByte(int v) throws IOException {
        write(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        buf[0] = (byte) (v >> 8);
        buf[1] = (byte) v;
        write(buf, 0, 2);
    }

    @Override
    public void writeChar(int v) throws IOException {
        writeShort(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        buf[0] = (byte) (v >> 24);
        buf[1] = (byte) (v >> 16);
        buf[2] = (byte) (v >> 8);
        buf[3] = (byte) v;
        write(buf, 0, 4);
    }

    @Override
    public void writeLong(long v) throws IOException {
        writeInt((int) (v >> 32));
        writeInt((int) v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    public void writeBytes(String s) throws IOException {
        byte[] b = new byte[s.length()];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) s.charAt(i);
        }
        write(b);
    }

    @Override
    public void writeChars(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            writeChar(s.charAt(i));
        }
    }

    @Override
    public void writeUTF(String s) throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        new java.io.DataOutputStream(bytes).writeUTF(s);
        write(bytes.toByteArray());
    }
}
