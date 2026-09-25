package retro.img;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** PNG decoding/encoding in plain Java (exact, non-premultiplied pixels, unlike the browser's decoders). */
public final class Png {
    private Png() {
    }

    /** Decoded image: packed ARGB pixels, row-major. */
    public static final class Image {
        public final int width;
        public final int height;
        public final int[] argb;
        public final boolean hasAlpha;

        public Image(int width, int height, int[] argb, boolean hasAlpha) {
            this.width = width;
            this.height = height;
            this.argb = argb;
            this.hasAlpha = hasAlpha;
        }
    }

    public static boolean isPng(byte[] data) {
        return data.length > 8 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G';
    }

    public static Image decode(byte[] data) throws IOException {
        if (!isPng(data)) {
            throw new IOException("Not a PNG file");
        }
        int pos = 8;
        int width = 0;
        int height = 0;
        int bitDepth = 0;
        int colorType = 0;
        int interlace = 0;
        int[] palette = null;
        byte[] trnsPalette = null;
        int trnsGray = -1;
        int trnsR = -1;
        int trnsG = -1;
        int trnsB = -1;
        ByteArrayOutputStream idat = new ByteArrayOutputStream(data.length);
        while (pos + 8 <= data.length) {
            int length = readInt(data, pos);
            int type = readInt(data, pos + 4);
            int start = pos + 8;
            if (start + length > data.length) {
                break;
            }
            switch (type) {
                case 0x49484452: // IHDR
                    width = readInt(data, start);
                    height = readInt(data, start + 4);
                    bitDepth = data[start + 8] & 0xFF;
                    colorType = data[start + 9] & 0xFF;
                    interlace = data[start + 12] & 0xFF;
                    break;
                case 0x504C5445: { // PLTE
                    palette = new int[length / 3];
                    for (int i = 0; i < palette.length; i++) {
                        palette[i] = 0xFF000000 | ((data[start + i * 3] & 0xFF) << 16)
                                | ((data[start + i * 3 + 1] & 0xFF) << 8) | (data[start + i * 3 + 2] & 0xFF);
                    }
                    break;
                }
                case 0x74524E53: // tRNS
                    if (colorType == 3) {
                        trnsPalette = new byte[length];
                        System.arraycopy(data, start, trnsPalette, 0, length);
                    } else if (colorType == 0 && length >= 2) {
                        trnsGray = readShort(data, start);
                    } else if (colorType == 2 && length >= 6) {
                        trnsR = readShort(data, start);
                        trnsG = readShort(data, start + 2);
                        trnsB = readShort(data, start + 4);
                    }
                    break;
                case 0x49444154: // IDAT
                    idat.write(data, start, length);
                    break;
                case 0x49454E44: // IEND
                    pos = data.length;
                    continue;
                default:
                    break;
            }
            pos = start + length + 4;
        }
        if (width <= 0 || height <= 0) {
            throw new IOException("Invalid PNG header");
        }
        if (palette != null && trnsPalette != null) {
            for (int i = 0; i < trnsPalette.length && i < palette.length; i++) {
                palette[i] = (palette[i] & 0xFFFFFF) | ((trnsPalette[i] & 0xFF) << 24);
            }
        }
        int channels;
        switch (colorType) {
            case 0:
            case 3:
                channels = 1;
                break;
            case 2:
                channels = 3;
                break;
            case 4:
                channels = 2;
                break;
            case 6:
                channels = 4;
                break;
            default:
                throw new IOException("Unsupported PNG color type " + colorType);
        }
        int bitsPerPixel = channels * bitDepth;
        int bpp = Math.max(1, bitsPerPixel / 8);

        byte[] raw = inflate(idat.toByteArray());
        int[] argb = new int[width * height];
        Decoder d = new Decoder(raw, argb, width, colorType, bitDepth, channels, bpp, palette, trnsGray, trnsR,
                trnsG, trnsB);
        if (interlace == 0) {
            d.pass(0, 0, 1, 1, width, height);
        } else {
            int[][] adam7 = { { 0, 0, 8, 8 }, { 4, 0, 8, 8 }, { 0, 4, 4, 8 }, { 2, 0, 4, 4 }, { 0, 2, 2, 4 },
                    { 1, 0, 2, 2 }, { 0, 1, 1, 2 } };
            for (int[] p : adam7) {
                int pw = (width - p[0] + p[2] - 1) / p[2];
                int ph = (height - p[1] + p[3] - 1) / p[3];
                if (pw > 0 && ph > 0) {
                    d.pass(p[0], p[1], p[2], p[3], pw, ph);
                }
            }
        }
        boolean alpha = colorType == 4 || colorType == 6 || trnsPalette != null || trnsGray >= 0 || trnsR >= 0;
        return new Image(width, height, argb, alpha);
    }

    private static final class Decoder {
        final byte[] raw;
        final int[] out;
        final int width;
        final int colorType;
        final int bitDepth;
        final int channels;
        final int bpp;
        final int[] palette;
        final int trnsGray;
        final int trnsR;
        final int trnsG;
        final int trnsB;
        int pos;

        Decoder(byte[] raw, int[] out, int width, int colorType, int bitDepth, int channels, int bpp, int[] palette,
                int trnsGray, int trnsR, int trnsG, int trnsB) {
            this.raw = raw;
            this.out = out;
            this.width = width;
            this.colorType = colorType;
            this.bitDepth = bitDepth;
            this.channels = channels;
            this.bpp = bpp;
            this.palette = palette;
            this.trnsGray = trnsGray;
            this.trnsR = trnsR;
            this.trnsG = trnsG;
            this.trnsB = trnsB;
        }

        void pass(int x0, int y0, int dx, int dy, int pw, int ph) throws IOException {
            int stride = (pw * channels * bitDepth + 7) / 8;
            byte[] prev = new byte[stride];
            byte[] line = new byte[stride];
            for (int row = 0; row < ph; row++) {
                if (pos + 1 + stride > raw.length) {
                    throw new IOException("Truncated PNG data");
                }
                int filter = raw[pos++];
                System.arraycopy(raw, pos, line, 0, stride);
                pos += stride;
                unfilter(filter, line, prev, stride);
                int y = y0 + row * dy;
                int base = y * width;
                for (int col = 0; col < pw; col++) {
                    out[base + x0 + col * dx] = pixel(line, col);
                }
                byte[] t = prev;
                prev = line;
                line = t;
            }
        }

        private void unfilter(int filter, byte[] line, byte[] prev, int stride) throws IOException {
            switch (filter) {
                case 0:
                    break;
                case 1:
                    for (int i = bpp; i < stride; i++) {
                        line[i] += line[i - bpp];
                    }
                    break;
                case 2:
                    for (int i = 0; i < stride; i++) {
                        line[i] += prev[i];
                    }
                    break;
                case 3:
                    for (int i = 0; i < stride; i++) {
                        int left = i >= bpp ? line[i - bpp] & 0xFF : 0;
                        line[i] += (byte) ((left + (prev[i] & 0xFF)) >> 1);
                    }
                    break;
                case 4:
                    for (int i = 0; i < stride; i++) {
                        int a = i >= bpp ? line[i - bpp] & 0xFF : 0;
                        int b = prev[i] & 0xFF;
                        int c = i >= bpp ? prev[i - bpp] & 0xFF : 0;
                        int p = a + b - c;
                        int pa = Math.abs(p - a);
                        int pb = Math.abs(p - b);
                        int pc = Math.abs(p - c);
                        int pred = pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
                        line[i] += (byte) pred;
                    }
                    break;
                default:
                    throw new IOException("Bad PNG filter " + filter);
            }
        }

        private int sample(byte[] line, int index) {
            switch (bitDepth) {
                case 8:
                    return line[index] & 0xFF;
                case 16:
                    return ((line[index * 2] & 0xFF) << 8) | (line[index * 2 + 1] & 0xFF);
                default: {
                    int bitPos = index * bitDepth;
                    int b = line[bitPos >> 3] & 0xFF;
                    int shift = 8 - bitDepth - (bitPos & 7);
                    return (b >> shift) & ((1 << bitDepth) - 1);
                }
            }
        }

        private int to8(int v) {
            switch (bitDepth) {
                case 16:
                    return (v * 255 + 32767) / 65535;
                case 8:
                    return v;
                case 4:
                    return v * 17;
                case 2:
                    return v * 85;
                default:
                    return v * 255;
            }
        }

        private int pixel(byte[] line, int col) {
            int i = col * channels;
            switch (colorType) {
                case 6:
                    if (bitDepth == 8) {
                        return ((line[i + 3] & 0xFF) << 24) | ((line[i] & 0xFF) << 16) | ((line[i + 1] & 0xFF) << 8)
                                | (line[i + 2] & 0xFF);
                    }
                    return (to8(sample(line, i + 3)) << 24) | (to8(sample(line, i)) << 16)
                            | (to8(sample(line, i + 1)) << 8) | to8(sample(line, i + 2));
                case 2: {
                    int r = sample(line, i);
                    int g = sample(line, i + 1);
                    int b = sample(line, i + 2);
                    int a = (r == trnsR && g == trnsG && b == trnsB) ? 0 : 0xFF;
                    return (a << 24) | (to8(r) << 16) | (to8(g) << 8) | to8(b);
                }
                case 3: {
                    int idx = sample(line, col);
                    return palette != null && idx < palette.length ? palette[idx] : 0xFF000000;
                }
                case 4: {
                    int v = to8(sample(line, i));
                    return (to8(sample(line, i + 1)) << 24) | (v << 16) | (v << 8) | v;
                }
                default: {
                    int raw = sample(line, i);
                    int v = to8(raw);
                    int a = raw == trnsGray ? 0 : 0xFF;
                    return (a << 24) | (v << 16) | (v << 8) | v;
                }
            }
        }
    }

    private static byte[] inflate(byte[] compressed) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 4);
        byte[] buf = new byte[65536];
        try {
            while (!inflater.finished()) {
                int n = inflater.inflate(buf);
                if (n == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        break;
                    }
                }
                out.write(buf, 0, n);
            }
        } catch (DataFormatException e) {
            throw new IOException("Corrupt PNG data", e);
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }

    /** Encodes ARGB pixels as an 8-bit RGBA (or RGB) PNG. */
    public static byte[] encode(int width, int height, int[] argb, boolean alpha) {
        int channels = alpha ? 4 : 3;
        int stride = width * channels;
        byte[] raw = new byte[(stride + 1) * height];
        int p = 0;
        for (int y = 0; y < height; y++) {
            raw[p++] = 0;
            for (int x = 0; x < width; x++) {
                int c = argb[y * width + x];
                raw[p++] = (byte) (c >> 16);
                raw[p++] = (byte) (c >> 8);
                raw[p++] = (byte) c;
                if (alpha) {
                    raw[p++] = (byte) (c >>> 24);
                }
            }
        }
        Deflater deflater = new Deflater(6);
        deflater.setInput(raw);
        deflater.finish();
        ByteArrayOutputStream z = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        while (!deflater.finished()) {
            int n = deflater.deflate(buf);
            z.write(buf, 0, n);
        }
        deflater.end();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x89);
        out.write('P');
        out.write('N');
        out.write('G');
        out.write(0x0D);
        out.write(0x0A);
        out.write(0x1A);
        out.write(0x0A);
        byte[] ihdr = new byte[13];
        writeInt(ihdr, 0, width);
        writeInt(ihdr, 4, height);
        ihdr[8] = 8;
        ihdr[9] = (byte) (alpha ? 6 : 2);
        chunk(out, "IHDR", ihdr);
        chunk(out, "IDAT", z.toByteArray());
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) {
        byte[] header = new byte[8];
        writeInt(header, 0, data.length);
        for (int i = 0; i < 4; i++) {
            header[4 + i] = (byte) type.charAt(i);
        }
        out.write(header, 0, 8);
        out.write(data, 0, data.length);
        CRC32 crc = new CRC32();
        crc.update(header, 4, 4);
        crc.update(data, 0, data.length);
        byte[] c = new byte[4];
        writeInt(c, 0, (int) crc.getValue());
        out.write(c, 0, 4);
    }

    private static int readInt(byte[] b, int i) {
        return ((b[i] & 0xFF) << 24) | ((b[i + 1] & 0xFF) << 16) | ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
    }

    private static int readShort(byte[] b, int i) {
        return ((b[i] & 0xFF) << 8) | (b[i + 1] & 0xFF);
    }

    private static void writeInt(byte[] b, int i, int v) {
        b[i] = (byte) (v >>> 24);
        b[i + 1] = (byte) (v >>> 16);
        b[i + 2] = (byte) (v >>> 8);
        b[i + 3] = (byte) v;
    }
}
