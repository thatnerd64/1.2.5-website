package org.teavm.classlib.java.text;

/**
 * java.text.Bidi, simplified: text is treated as one run at the base level. Only used by Minecraft for
 * right-to-left languages.
 */
public final class TBidi {
    public static final int DIRECTION_LEFT_TO_RIGHT = 0;
    public static final int DIRECTION_RIGHT_TO_LEFT = 1;
    public static final int DIRECTION_DEFAULT_LEFT_TO_RIGHT = -2;
    public static final int DIRECTION_DEFAULT_RIGHT_TO_LEFT = -1;
    private final int length;
    private final int level;

    public TBidi(String paragraph, int flags) {
        this.length = paragraph.length();
        this.level = flags == DIRECTION_RIGHT_TO_LEFT ? 1 : 0;
    }

    public TBidi(char[] text, int textStart, byte[] embeddings, int embStart, int paragraphLength, int flags) {
        this.length = paragraphLength;
        this.level = flags == DIRECTION_RIGHT_TO_LEFT ? 1 : 0;
    }

    public boolean isMixed() {
        return false;
    }

    public boolean isLeftToRight() {
        return level == 0;
    }

    public boolean isRightToLeft() {
        return level == 1;
    }

    public boolean baseIsLeftToRight() {
        return level == 0;
    }

    public int getBaseLevel() {
        return level;
    }

    public int getLength() {
        return length;
    }

    public int getLevelAt(int offset) {
        return level;
    }

    public int getRunCount() {
        return 1;
    }

    public int getRunLevel(int run) {
        return level;
    }

    public int getRunStart(int run) {
        return 0;
    }

    public int getRunLimit(int run) {
        return length;
    }

    public static boolean requiresBidi(char[] text, int start, int limit) {
        return false;
    }

    public static void reorderVisually(byte[] levels, int levelStart, Object[] objects, int objectStart, int count) {
    }
}
