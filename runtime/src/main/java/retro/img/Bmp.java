package retro.img;

import java.io.IOException;

/**
 * Windows BMP decoder (uncompressed 1/4/8/24/32-bit and BI_BITFIELDS), as supported by the JDK's ImageIO. Some
 * mods ship BMP files named {@code .png} (e.g. Dimensional Anchors' GUI), which desktop Java reads fine.
 */
public final class Bmp {
    private Bmp() {
    }

    public static boolean isBmp(byte[] data) {
        return data.length > 26 && data[0] == 'B' && data[1] == 'M';
    }

    public static Png.Image decode(byte[] d) throws IOException {
        int dataOffset = i32(d, 10);
        int headerSize = i32(d, 14);
        int width;
        int height;
        int bpp;
        int compression = 0;
        int colorsUsed = 0;
        if (headerSize == 12) {
            width = u16(d, 18);
            height = (short) u16(d, 20);
            bpp = u16(d, 24);
        } else {
            width = i32(d, 18);
            height = i32(d, 22);
            bpp = u16(d, 28);
            compression = i32(d, 30);
            colorsUsed = i32(d, 46);
        }
        boolean topDown = height < 0;
        height = Math.abs(height);
        if (width <= 0 || height <= 0 || (compression != 0 && compression != 3)) {
            throw new IOException("Unsupported BMP (compression " + compression + ")");
        }
        int[] masks = { 0xFF0000, 0xFF00, 0xFF, 0 };
        if (compression == 3) {
            int m = headerSize >= 52 ? 14 + 40 : 14 + headerSize;
            masks[0] = i32(d, m);
            masks[1] = i32(d, m + 4);
            masks[2] = i32(d, m + 8);
            if (headerSize >= 56) {
                masks[3] = i32(d, m + 12);
            }
        } else if (bpp == 16) {
            masks = new int[] { 0x7C00, 0x3E0, 0x1F, 0 };
        }
        int[] palette = null;
        if (bpp <= 8) {
            int entries = colorsUsed != 0 ? colorsUsed : 1 << bpp;
            int entrySize = headerSize == 12 ? 3 : 4;
            int p = 14 + headerSize;
            palette = new int[256];
            for (int i = 0; i < entries && i < 256 && p + i * entrySize + 2 < d.length; i++) {
                int o = p + i * entrySize;
                palette[i] = 0xFF000000 | (d[o + 2] & 0xFF) << 16 | (d[o + 1] & 0xFF) << 8 | (d[o] & 0xFF);
            }
        }
        int stride = ((width * bpp + 31) / 32) * 4;
        int[] argb = new int[width * height];
        boolean alpha = masks[3] != 0;
        for (int y = 0; y < height; y++) {
            int row = dataOffset + (topDown ? y : height - 1 - y) * stride;
            int out = y * width;
            for (int x = 0; x < width; x++) {
                int c;
                switch (bpp) {
                    case 1:
                        c = palette[(d[row + (x >> 3)] >> (7 - (x & 7))) & 1];
                        break;
                    case 4:
                        c = palette[(d[row + (x >> 1)] >> ((x & 1) == 0 ? 4 : 0)) & 0xF];
                        break;
                    case 8:
                        c = palette[d[row + x] & 0xFF];
                        break;
                    case 24: {
                        int o = row + x * 3;
                        c = 0xFF000000 | (d[o + 2] & 0xFF) << 16 | (d[o + 1] & 0xFF) << 8 | (d[o] & 0xFF);
                        break;
                    }
                    case 16:
                    case 32: {
                        int v = bpp == 16 ? u16(d, row + x * 2) : i32(d, row + x * 4);
                        if (compression == 0 && bpp == 32) {
                            c = 0xFF000000 | (v & 0xFFFFFF);
                        } else {
                            c = (alpha ? channel(v, masks[3]) : 0xFF) << 24 | channel(v, masks[0]) << 16
                                    | channel(v, masks[1]) << 8 | channel(v, masks[2]);
                        }
                        break;
                    }
                    default:
                        throw new IOException("Unsupported BMP bit depth " + bpp);
                }
                argb[out + x] = c;
            }
        }
        return new Png.Image(width, height, argb, alpha);
    }

    private static int channel(int v, int mask) {
        if (mask == 0) {
            return 0;
        }
        int shift = Integer.numberOfTrailingZeros(mask);
        int bits = Integer.bitCount(mask);
        int value = (v & mask) >>> shift;
        return bits >= 8 ? value >>> (bits - 8) : value * 255 / ((1 << bits) - 1);
    }

    private static int u16(byte[] d, int o) {
        return (d[o] & 0xFF) | (d[o + 1] & 0xFF) << 8;
    }

    private static int i32(byte[] d, int o) {
        return (d[o] & 0xFF) | (d[o + 1] & 0xFF) << 8 | (d[o + 2] & 0xFF) << 16 | (d[o + 3] & 0xFF) << 24;
    }
}
