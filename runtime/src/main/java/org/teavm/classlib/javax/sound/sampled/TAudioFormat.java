package org.teavm.classlib.javax.sound.sampled;

/** javax.sound.sampled.AudioFormat: a plain value holder, as used by paulscode's codecs and OpenAL library. */
public class TAudioFormat {
    protected Encoding encoding;
    protected float sampleRate;
    protected int sampleSizeInBits;
    protected int channels;
    protected int frameSize;
    protected float frameRate;
    protected boolean bigEndian;

    public TAudioFormat(Encoding encoding, float sampleRate, int sampleSizeInBits, int channels, int frameSize,
            float frameRate, boolean bigEndian) {
        this.encoding = encoding;
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.frameSize = frameSize;
        this.frameRate = frameRate;
        this.bigEndian = bigEndian;
    }

    public TAudioFormat(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
        this(signed ? Encoding.PCM_SIGNED : Encoding.PCM_UNSIGNED, sampleRate, sampleSizeInBits, channels,
                channels == -1 || sampleSizeInBits == -1 ? -1 : ((sampleSizeInBits + 7) / 8) * channels,
                sampleRate, bigEndian);
    }

    public Encoding getEncoding() {
        return encoding;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    public int getChannels() {
        return channels;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    public boolean matches(TAudioFormat format) {
        return format.getEncoding().equals(encoding) && format.getChannels() == channels
                && format.getSampleSizeInBits() == sampleSizeInBits && format.getFrameSize() == frameSize
                && (format.getSampleRate() == sampleRate || format.getSampleRate() == -1)
                && (sampleSizeInBits <= 8 || format.isBigEndian() == bigEndian);
    }

    @Override
    public String toString() {
        return encoding + " " + sampleRate + " Hz, " + sampleSizeInBits + " bit, " + channels + " channels, "
                + (bigEndian ? "big-endian" : "little-endian");
    }

    public static class Encoding {
        public static final Encoding PCM_SIGNED = new Encoding("PCM_SIGNED");
        public static final Encoding PCM_UNSIGNED = new Encoding("PCM_UNSIGNED");
        public static final Encoding PCM_FLOAT = new Encoding("PCM_FLOAT");
        public static final Encoding ULAW = new Encoding("ULAW");
        public static final Encoding ALAW = new Encoding("ALAW");

        private final String name;

        public Encoding(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Encoding && name.equals(((Encoding) obj).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
