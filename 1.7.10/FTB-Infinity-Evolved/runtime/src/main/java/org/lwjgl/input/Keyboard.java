package org.lwjgl.input;

import retro.input.Input;

/** LWJGL Keyboard backed by page key events (see retro.input.Input). */
public final class Keyboard {
    public static final int EVENT_SIZE = 18;
    public static final int CHAR_NONE = 0;
    public static final int KEY_NONE = 0;
    public static final int KEY_ESCAPE = 1;
    public static final int KEY_1 = 2;
    public static final int KEY_2 = 3;
    public static final int KEY_3 = 4;
    public static final int KEY_4 = 5;
    public static final int KEY_5 = 6;
    public static final int KEY_6 = 7;
    public static final int KEY_7 = 8;
    public static final int KEY_8 = 9;
    public static final int KEY_9 = 10;
    public static final int KEY_0 = 11;
    public static final int KEY_MINUS = 12;
    public static final int KEY_EQUALS = 13;
    public static final int KEY_BACK = 14;
    public static final int KEY_TAB = 15;
    public static final int KEY_Q = 16;
    public static final int KEY_W = 17;
    public static final int KEY_E = 18;
    public static final int KEY_R = 19;
    public static final int KEY_T = 20;
    public static final int KEY_Y = 21;
    public static final int KEY_U = 22;
    public static final int KEY_I = 23;
    public static final int KEY_O = 24;
    public static final int KEY_P = 25;
    public static final int KEY_LBRACKET = 26;
    public static final int KEY_RBRACKET = 27;
    public static final int KEY_RETURN = 28;
    public static final int KEY_LCONTROL = 29;
    public static final int KEY_A = 30;
    public static final int KEY_S = 31;
    public static final int KEY_D = 32;
    public static final int KEY_F = 33;
    public static final int KEY_G = 34;
    public static final int KEY_H = 35;
    public static final int KEY_J = 36;
    public static final int KEY_K = 37;
    public static final int KEY_L = 38;
    public static final int KEY_SEMICOLON = 39;
    public static final int KEY_APOSTROPHE = 40;
    public static final int KEY_GRAVE = 41;
    public static final int KEY_LSHIFT = 42;
    public static final int KEY_BACKSLASH = 43;
    public static final int KEY_Z = 44;
    public static final int KEY_X = 45;
    public static final int KEY_C = 46;
    public static final int KEY_V = 47;
    public static final int KEY_B = 48;
    public static final int KEY_N = 49;
    public static final int KEY_M = 50;
    public static final int KEY_COMMA = 51;
    public static final int KEY_PERIOD = 52;
    public static final int KEY_SLASH = 53;
    public static final int KEY_RSHIFT = 54;
    public static final int KEY_MULTIPLY = 55;
    public static final int KEY_LMENU = 56;
    public static final int KEY_SPACE = 57;
    public static final int KEY_CAPITAL = 58;
    public static final int KEY_F1 = 59;
    public static final int KEY_F2 = 60;
    public static final int KEY_F3 = 61;
    public static final int KEY_F4 = 62;
    public static final int KEY_F5 = 63;
    public static final int KEY_F6 = 64;
    public static final int KEY_F7 = 65;
    public static final int KEY_F8 = 66;
    public static final int KEY_F9 = 67;
    public static final int KEY_F10 = 68;
    public static final int KEY_NUMLOCK = 69;
    public static final int KEY_SCROLL = 70;
    public static final int KEY_NUMPAD7 = 71;
    public static final int KEY_NUMPAD8 = 72;
    public static final int KEY_NUMPAD9 = 73;
    public static final int KEY_SUBTRACT = 74;
    public static final int KEY_NUMPAD4 = 75;
    public static final int KEY_NUMPAD5 = 76;
    public static final int KEY_NUMPAD6 = 77;
    public static final int KEY_ADD = 78;
    public static final int KEY_NUMPAD1 = 79;
    public static final int KEY_NUMPAD2 = 80;
    public static final int KEY_NUMPAD3 = 81;
    public static final int KEY_NUMPAD0 = 82;
    public static final int KEY_DECIMAL = 83;
    public static final int KEY_F11 = 87;
    public static final int KEY_F12 = 88;
    public static final int KEY_F13 = 100;
    public static final int KEY_F14 = 101;
    public static final int KEY_F15 = 102;
    public static final int KEY_F16 = 103;
    public static final int KEY_F17 = 104;
    public static final int KEY_F18 = 105;
    public static final int KEY_KANA = 112;
    public static final int KEY_F19 = 113;
    public static final int KEY_CONVERT = 121;
    public static final int KEY_NOCONVERT = 123;
    public static final int KEY_YEN = 125;
    public static final int KEY_NUMPADEQUALS = 141;
    public static final int KEY_CIRCUMFLEX = 144;
    public static final int KEY_AT = 145;
    public static final int KEY_COLON = 146;
    public static final int KEY_UNDERLINE = 147;
    public static final int KEY_KANJI = 148;
    public static final int KEY_STOP = 149;
    public static final int KEY_AX = 150;
    public static final int KEY_UNLABELED = 151;
    public static final int KEY_NUMPADENTER = 156;
    public static final int KEY_RCONTROL = 157;
    public static final int KEY_SECTION = 167;
    public static final int KEY_NUMPADCOMMA = 179;
    public static final int KEY_DIVIDE = 181;
    public static final int KEY_SYSRQ = 183;
    public static final int KEY_RMENU = 184;
    public static final int KEY_FUNCTION = 196;
    public static final int KEY_PAUSE = 197;
    public static final int KEY_HOME = 199;
    public static final int KEY_UP = 200;
    public static final int KEY_PRIOR = 201;
    public static final int KEY_LEFT = 203;
    public static final int KEY_RIGHT = 205;
    public static final int KEY_END = 207;
    public static final int KEY_DOWN = 208;
    public static final int KEY_NEXT = 209;
    public static final int KEY_INSERT = 210;
    public static final int KEY_DELETE = 211;
    public static final int KEY_CLEAR = 218;
    public static final int KEY_LMETA = 219;
    public static final int KEY_LWIN = 219;
    public static final int KEY_RMETA = 220;
    public static final int KEY_RWIN = 220;
    public static final int KEY_APPS = 221;
    public static final int KEY_POWER = 222;
    public static final int KEY_SLEEP = 223;
    public static final int KEYBOARD_SIZE = 256;
    public static final int BUFFER_SIZE = 50;

    private static final String[] NAMES = new String[256];
    private static boolean created;
    private static boolean repeat;
    private static int eventKey;
    private static char eventChar;
    private static boolean eventState;
    private static boolean eventRepeat;
    private static long eventNanos;

    static {
        NAMES[0] = "NONE";
        NAMES[1] = "ESCAPE";
        NAMES[2] = "1";
        NAMES[3] = "2";
        NAMES[4] = "3";
        NAMES[5] = "4";
        NAMES[6] = "5";
        NAMES[7] = "6";
        NAMES[8] = "7";
        NAMES[9] = "8";
        NAMES[10] = "9";
        NAMES[11] = "0";
        NAMES[12] = "MINUS";
        NAMES[13] = "EQUALS";
        NAMES[14] = "BACK";
        NAMES[15] = "TAB";
        NAMES[16] = "Q";
        NAMES[17] = "W";
        NAMES[18] = "E";
        NAMES[19] = "R";
        NAMES[20] = "T";
        NAMES[21] = "Y";
        NAMES[22] = "U";
        NAMES[23] = "I";
        NAMES[24] = "O";
        NAMES[25] = "P";
        NAMES[26] = "LBRACKET";
        NAMES[27] = "RBRACKET";
        NAMES[28] = "RETURN";
        NAMES[29] = "LCONTROL";
        NAMES[30] = "A";
        NAMES[31] = "S";
        NAMES[32] = "D";
        NAMES[33] = "F";
        NAMES[34] = "G";
        NAMES[35] = "H";
        NAMES[36] = "J";
        NAMES[37] = "K";
        NAMES[38] = "L";
        NAMES[39] = "SEMICOLON";
        NAMES[40] = "APOSTROPHE";
        NAMES[41] = "GRAVE";
        NAMES[42] = "LSHIFT";
        NAMES[43] = "BACKSLASH";
        NAMES[44] = "Z";
        NAMES[45] = "X";
        NAMES[46] = "C";
        NAMES[47] = "V";
        NAMES[48] = "B";
        NAMES[49] = "N";
        NAMES[50] = "M";
        NAMES[51] = "COMMA";
        NAMES[52] = "PERIOD";
        NAMES[53] = "SLASH";
        NAMES[54] = "RSHIFT";
        NAMES[55] = "MULTIPLY";
        NAMES[56] = "LMENU";
        NAMES[57] = "SPACE";
        NAMES[58] = "CAPITAL";
        NAMES[59] = "F1";
        NAMES[60] = "F2";
        NAMES[61] = "F3";
        NAMES[62] = "F4";
        NAMES[63] = "F5";
        NAMES[64] = "F6";
        NAMES[65] = "F7";
        NAMES[66] = "F8";
        NAMES[67] = "F9";
        NAMES[68] = "F10";
        NAMES[69] = "NUMLOCK";
        NAMES[70] = "SCROLL";
        NAMES[71] = "NUMPAD7";
        NAMES[72] = "NUMPAD8";
        NAMES[73] = "NUMPAD9";
        NAMES[74] = "SUBTRACT";
        NAMES[75] = "NUMPAD4";
        NAMES[76] = "NUMPAD5";
        NAMES[77] = "NUMPAD6";
        NAMES[78] = "ADD";
        NAMES[79] = "NUMPAD1";
        NAMES[80] = "NUMPAD2";
        NAMES[81] = "NUMPAD3";
        NAMES[82] = "NUMPAD0";
        NAMES[83] = "DECIMAL";
        NAMES[87] = "F11";
        NAMES[88] = "F12";
        NAMES[100] = "F13";
        NAMES[101] = "F14";
        NAMES[102] = "F15";
        NAMES[103] = "F16";
        NAMES[104] = "F17";
        NAMES[105] = "F18";
        NAMES[112] = "KANA";
        NAMES[113] = "F19";
        NAMES[121] = "CONVERT";
        NAMES[123] = "NOCONVERT";
        NAMES[125] = "YEN";
        NAMES[141] = "NUMPADEQUALS";
        NAMES[144] = "CIRCUMFLEX";
        NAMES[145] = "AT";
        NAMES[146] = "COLON";
        NAMES[147] = "UNDERLINE";
        NAMES[148] = "KANJI";
        NAMES[149] = "STOP";
        NAMES[150] = "AX";
        NAMES[151] = "UNLABELED";
        NAMES[156] = "NUMPADENTER";
        NAMES[157] = "RCONTROL";
        NAMES[167] = "SECTION";
        NAMES[179] = "NUMPADCOMMA";
        NAMES[181] = "DIVIDE";
        NAMES[183] = "SYSRQ";
        NAMES[184] = "RMENU";
        NAMES[196] = "FUNCTION";
        NAMES[197] = "PAUSE";
        NAMES[199] = "HOME";
        NAMES[200] = "UP";
        NAMES[201] = "PRIOR";
        NAMES[203] = "LEFT";
        NAMES[205] = "RIGHT";
        NAMES[207] = "END";
        NAMES[208] = "DOWN";
        NAMES[209] = "NEXT";
        NAMES[210] = "INSERT";
        NAMES[211] = "DELETE";
        NAMES[218] = "CLEAR";
        NAMES[219] = "LMETA";
        NAMES[220] = "RMETA";
        NAMES[221] = "APPS";
        NAMES[222] = "POWER";
        NAMES[223] = "SLEEP";
    }

    private Keyboard() {
    }

    public static void create() {
        created = true;
    }

    public static boolean isCreated() {
        return created;
    }

    public static void destroy() {
        created = false;
    }

    public static void poll() {
    }

    public static boolean next() {
        int e = Input.nextKey();
        if (e < 0) {
            return false;
        }
        eventKey = e & 0xFF;
        eventChar = (char) ((e >>> 8) & 0xFFFF);
        eventState = ((e >>> 24) & 1) != 0;
        eventRepeat = ((e >>> 25) & 1) != 0;
        eventNanos = System.nanoTime();
        return true;
    }

    public static int getEventKey() {
        return eventKey;
    }

    public static char getEventCharacter() {
        return eventChar;
    }

    public static boolean getEventKeyState() {
        return eventState;
    }

    public static boolean isRepeatEvent() {
        return eventRepeat;
    }

    public static long getEventNanoseconds() {
        return eventNanos;
    }

    public static boolean isKeyDown(int key) {
        return key >= 0 && key < 256 && Input.isKeyDown(key);
    }

    public static String getKeyName(int key) {
        return key >= 0 && key < NAMES.length ? NAMES[key] : null;
    }

    public static int getKeyIndex(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (name.equals(NAMES[i])) {
                return i;
            }
        }
        return KEY_NONE;
    }

    public static int getKeyCount() {
        return KEYBOARD_SIZE;
    }

    public static int getNumKeyboardEvents() {
        return Input.keyQueueSize();
    }

    public static void enableRepeatEvents(boolean enable) {
        repeat = enable;
        Input.setRepeat(enable);
    }

    public static boolean areRepeatEventsEnabled() {
        return repeat;
    }
}
