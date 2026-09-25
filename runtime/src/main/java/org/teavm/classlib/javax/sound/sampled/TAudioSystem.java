package org.teavm.classlib.javax.sound.sampled;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/** javax.sound.sampled.AudioSystem: reads RIFF WAVE (PCM) files, which is what paulscode's CodecWav needs. */
public final class TAudioSystem {
    public static final int NOT_SPECIFIED = -1;

    private TAudioSystem() {
    }

    public static TAudioInputStream getAudioInputStream(URL url) throws TUnsupportedAudioFileException, IOException {
        return getAudioInputStream(new BufferedInputStream(url.openStream()));
    }

    public static TAudioInputStream getAudioInputStream(File file) throws TUnsupportedAudioFileException,
            IOException {
        return getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
    }

    public static TAudioInputStream getAudioInputStream(InputStream in) throws TUnsupportedAudioFileException,
            IOException {
        DataInputStream data = new DataInputStream(in);
        byte[] id = new byte[4];
        data.readFully(id);
        if (!"RIFF".equals(ascii(id))) {
            throw new TUnsupportedAudioFileException("Stream is not a RIFF WAVE file");
        }
        le32(data);
        data.readFully(id);
        if (!"WAVE".equals(ascii(id))) {
            throw new TUnsupportedAudioFileException("Stream is not a RIFF WAVE file");
        }
        TAudioFormat format = null;
        while (true) {
            data.readFully(id);
            long size = le32(data) & 0xFFFFFFFFL;
            String chunk = ascii(id);
            if ("fmt ".equals(chunk)) {
                int tag = le16(data);
                int channels = le16(data);
                int rate = le32(data);
                le32(data);
                int blockAlign = le16(data);
                int bits = le16(data);
                skip(data, size - 16 + (size & 1));
                if (tag != 1 && tag != 0xFFFE) {
                    throw new TUnsupportedAudioFileException("Unsupported WAVE encoding " + tag);
                }
                format = new TAudioFormat(bits <= 8 ? TAudioFormat.Encoding.PCM_UNSIGNED
                        : TAudioFormat.Encoding.PCM_SIGNED, rate, bits, channels, blockAlign, rate, false);
            } else if ("data".equals(chunk)) {
                if (format == null) {
                    throw new TUnsupportedAudioFileException("WAVE data before format");
                }
                return new TAudioInputStream(in, format, size / Math.max(1, format.getFrameSize()));
            } else {
                skip(data, size + (size & 1));
            }
        }
    }

    private static void skip(DataInputStream in, long n) throws IOException {
        while (n > 0) {
            int skipped = in.skipBytes((int) Math.min(n, Integer.MAX_VALUE));
            if (skipped <= 0) {
                in.readByte();
                skipped = 1;
            }
            n -= skipped;
        }
    }

    private static String ascii(byte[] b) {
        char[] c = new char[b.length];
        for (int i = 0; i < b.length; i++) {
            c[i] = (char) (b[i] & 0xFF);
        }
        return new String(c);
    }

    private static int le16(DataInputStream in) throws IOException {
        int a = in.readUnsignedByte();
        return a | in.readUnsignedByte() << 8;
    }

    private static int le32(DataInputStream in) throws IOException {
        return le16(in) | le16(in) << 16;
    }
}
