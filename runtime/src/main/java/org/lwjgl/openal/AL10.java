package org.lwjgl.openal;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.teavm.jso.JSBody;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Int8Array;

/** OpenAL 1.0 entry points used by paulscode SoundSystem, implemented by web/js/al.js. */
public final class AL10 {
    public static final int AL_INVALID = 0xffffffff;
    public static final int AL_NONE = 0;
    public static final int AL_FALSE = 0;
    public static final int AL_TRUE = 1;
    public static final int AL_SOURCE_TYPE = 0x1027;
    public static final int AL_SOURCE_ABSOLUTE = 513;
    public static final int AL_SOURCE_RELATIVE = 514;
    public static final int AL_CONE_INNER_ANGLE = 0x1001;
    public static final int AL_CONE_OUTER_ANGLE = 0x1002;
    public static final int AL_PITCH = 0x1003;
    public static final int AL_POSITION = 0x1004;
    public static final int AL_DIRECTION = 0x1005;
    public static final int AL_VELOCITY = 0x1006;
    public static final int AL_LOOPING = 0x1007;
    public static final int AL_BUFFER = 0x1009;
    public static final int AL_GAIN = 0x100a;
    public static final int AL_MIN_GAIN = 0x100d;
    public static final int AL_MAX_GAIN = 0x100e;
    public static final int AL_ORIENTATION = 0x100f;
    public static final int AL_REFERENCE_DISTANCE = 0x1020;
    public static final int AL_ROLLOFF_FACTOR = 0x1021;
    public static final int AL_CONE_OUTER_GAIN = 0x1022;
    public static final int AL_MAX_DISTANCE = 0x1023;
    public static final int AL_CHANNEL_MASK = 0x3000;
    public static final int AL_SOURCE_STATE = 0x1010;
    public static final int AL_INITIAL = 0x1011;
    public static final int AL_PLAYING = 0x1012;
    public static final int AL_PAUSED = 0x1013;
    public static final int AL_STOPPED = 0x1014;
    public static final int AL_BUFFERS_QUEUED = 0x1015;
    public static final int AL_BUFFERS_PROCESSED = 0x1016;
    public static final int AL_FORMAT_MONO8 = 0x1100;
    public static final int AL_FORMAT_MONO16 = 0x1101;
    public static final int AL_FORMAT_STEREO8 = 0x1102;
    public static final int AL_FORMAT_STEREO16 = 0x1103;
    public static final int AL_FORMAT_VORBIS_EXT = 0x10003;
    public static final int AL_FREQUENCY = 0x2001;
    public static final int AL_BITS = 0x2002;
    public static final int AL_CHANNELS = 0x2003;
    public static final int AL_SIZE = 0x2004;
    public static final int AL_DATA = 0x2005;
    public static final int AL_UNUSED = 0x2010;
    public static final int AL_PENDING = 0x2011;
    public static final int AL_PROCESSED = 0x2012;
    public static final int AL_NO_ERROR = 0;
    public static final int AL_INVALID_NAME = 0xa001;
    public static final int AL_INVALID_ENUM = 0xa002;
    public static final int AL_INVALID_VALUE = 0xa003;
    public static final int AL_INVALID_OPERATION = 0xa004;
    public static final int AL_OUT_OF_MEMORY = 0xa005;
    public static final int AL_VENDOR = 0xb001;
    public static final int AL_VERSION = 0xb002;
    public static final int AL_RENDERER = 0xb003;
    public static final int AL_EXTENSIONS = 0xb004;
    public static final int AL_DOPPLER_FACTOR = 0xc000;
    public static final int AL_DOPPLER_VELOCITY = 0xc001;
    public static final int AL_DISTANCE_MODEL = 0xd000;
    public static final int AL_INVERSE_DISTANCE = 0xd001;
    public static final int AL_INVERSE_DISTANCE_CLAMPED = 0xd002;

    private AL10() {
    }

    @JSBody(script = "return !!(window.RetroAL && window.RetroAL.available());")
    static native boolean available();

    @JSBody(script = "return RetroAL.genBuffer();")
    private static native int genBuffer();

    @JSBody(params = "id", script = "RetroAL.deleteBuffer(id);")
    private static native void deleteBuffer(int id);

    @JSBody(params = { "id", "format", "data", "freq" }, script = "RetroAL.bufferData(id, format, data, freq);")
    private static native void bufferData(int id, int format, ArrayBufferView data, int freq);

    @JSBody(script = "return RetroAL.genSource();")
    private static native int genSource();

    @JSBody(params = "id", script = "RetroAL.deleteSource(id);")
    private static native void deleteSource(int id);

    @JSBody(params = { "fn", "id" }, script = "RetroAL[fn](id);")
    private static native void call(String fn, int id);

    @JSBody(params = { "fn", "id", "v" }, script = "RetroAL[fn](id, v);")
    private static native void callF(String fn, int id, float v);

    @JSBody(params = { "fn", "id", "v" }, script = "RetroAL[fn](id, v);")
    private static native void callB(String fn, int id, boolean v);

    @JSBody(params = { "fn", "id" }, script = "return RetroAL[fn](id);")
    private static native int query(String fn, int id);

    @JSBody(params = { "id", "x", "y", "z" }, script = "RetroAL.setPosition(id, x, y, z);")
    private static native void setPosition(int id, float x, float y, float z);

    @JSBody(params = { "x", "y", "z" }, script = "RetroAL.listenerPosition(x, y, z);")
    private static native void listenerPosition(float x, float y, float z);

    @JSBody(params = { "ax", "ay", "az", "ux", "uy", "uz" }, script = "RetroAL.listenerOrientation(ax, ay, az, ux, uy, uz);")
    private static native void listenerOrientation(float ax, float ay, float az, float ux, float uy, float uz);

    @JSBody(params = "v", script = "RetroAL.listenerGain(v);")
    private static native void listenerGain(float v);

    @JSBody(params = "m", script = "RetroAL.distanceModel(m);")
    private static native void distanceModel(int m);

    // ---- buffers ----

    public static void alGenBuffers(IntBuffer buffers) {
        for (int i = buffers.position(); i < buffers.limit(); i++) {
            buffers.put(i, genBuffer());
        }
    }

    public static int alGenBuffers() {
        return genBuffer();
    }

    public static void alDeleteBuffers(IntBuffer buffers) {
        for (int i = buffers.position(); i < buffers.limit(); i++) {
            deleteBuffer(buffers.get(i));
        }
    }

    public static void alDeleteBuffers(int buffer) {
        deleteBuffer(buffer);
    }

    public static boolean alIsBuffer(int buffer) {
        return buffer > 0;
    }

    public static void alBufferData(int buffer, int format, ByteBuffer data, int freq) {
        Int8Array all = Int8Array.fromJavaBuffer(data);
        bufferData(buffer, format, new Int8Array(all.getBuffer(), all.getByteOffset() + data.position(),
                data.remaining()), freq);
    }

    public static void alBufferData(int buffer, int format, java.nio.ShortBuffer data, int freq) {
        Int8Array all = Int8Array.fromJavaBuffer(data);
        bufferData(buffer, format, new Int8Array(all.getBuffer(), all.getByteOffset() + data.position() * 2,
                data.remaining() * 2), freq);
    }

    public static int alGetBufferi(int buffer, int pname) {
        return 0;
    }

    // ---- sources ----

    public static void alGenSources(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            sources.put(i, genSource());
        }
    }

    public static int alGenSources() {
        return genSource();
    }

    public static void alDeleteSources(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            deleteSource(sources.get(i));
        }
    }

    public static void alDeleteSources(int source) {
        deleteSource(source);
    }

    public static boolean alIsSource(int source) {
        return source > 0;
    }

    public static void alSourcePlay(int source) {
        call("play", source);
    }

    public static void alSourcePlay(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            call("play", sources.get(i));
        }
    }

    public static void alSourcePause(int source) {
        call("pause", source);
    }

    public static void alSourcePause(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            call("pause", sources.get(i));
        }
    }

    public static void alSourceStop(int source) {
        call("stop", source);
    }

    public static void alSourceStop(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            call("stop", sources.get(i));
        }
    }

    public static void alSourceRewind(int source) {
        call("rewind", source);
    }

    public static void alSourceRewind(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            call("rewind", sources.get(i));
        }
    }

    public static void alSourceQueueBuffers(int source, IntBuffer buffers) {
        for (int i = buffers.position(); i < buffers.limit(); i++) {
            callF("queueBuffer", source, buffers.get(i));
        }
    }

    public static void alSourceQueueBuffers(int source, int buffer) {
        callF("queueBuffer", source, buffer);
    }

    public static void alSourceUnqueueBuffers(int source, IntBuffer buffers) {
        for (int i = buffers.position(); i < buffers.limit(); i++) {
            buffers.put(i, query("unqueueBuffer", source));
        }
    }

    public static int alSourceUnqueueBuffers(int source) {
        return query("unqueueBuffer", source);
    }

    public static void alSourcei(int source, int pname, int value) {
        switch (pname) {
            case AL_BUFFER:
                callF("setBuffer", source, value);
                break;
            case AL_LOOPING:
                callB("setLooping", source, value != 0);
                break;
            case AL_SOURCE_RELATIVE:
                callB("setRelative", source, value != 0);
                break;
            default:
                alSourcef(source, pname, value);
                break;
        }
    }

    public static void alSourcef(int source, int pname, float value) {
        switch (pname) {
            case AL_GAIN:
                callF("setGain", source, value);
                break;
            case AL_PITCH:
                callF("setPitch", source, value);
                break;
            case AL_ROLLOFF_FACTOR:
                callF("setRolloff", source, value);
                break;
            case AL_REFERENCE_DISTANCE:
                callF("setRefDistance", source, value);
                break;
            case AL_MAX_DISTANCE:
                callF("setMaxDistance", source, value);
                break;
            default:
                break;
        }
    }

    public static void alSource3f(int source, int pname, float x, float y, float z) {
        if (pname == AL_POSITION) {
            setPosition(source, x, y, z);
        }
    }

    public static void alSource(int source, int pname, FloatBuffer value) {
        int p = value.position();
        alSource3f(source, pname, value.get(p), value.get(p + 1), value.get(p + 2));
    }

    public static void alSource(int source, int pname, IntBuffer value) {
        alSourcei(source, pname, value.get(value.position()));
    }

    public static int alGetSourcei(int source, int pname) {
        switch (pname) {
            case AL_SOURCE_STATE:
                return query("getState", source);
            case AL_BUFFERS_PROCESSED:
                return query("processed", source);
            case AL_BUFFERS_QUEUED:
                return query("queued", source);
            default:
                return 0;
        }
    }

    public static void alGetSource(int source, int pname, IntBuffer out) {
        out.put(out.position(), alGetSourcei(source, pname));
    }

    public static float alGetSourcef(int source, int pname) {
        return 0;
    }

    // ---- listener & globals ----

    public static void alListener(int pname, FloatBuffer value) {
        int p = value.position();
        if (pname == AL_POSITION) {
            listenerPosition(value.get(p), value.get(p + 1), value.get(p + 2));
        } else if (pname == AL_ORIENTATION) {
            listenerOrientation(value.get(p), value.get(p + 1), value.get(p + 2), value.get(p + 3),
                    value.get(p + 4), value.get(p + 5));
        } else if (pname == AL_GAIN) {
            listenerGain(value.get(p));
        }
    }

    public static void alListener3f(int pname, float x, float y, float z) {
        if (pname == AL_POSITION) {
            listenerPosition(x, y, z);
        }
    }

    public static void alListenerf(int pname, float value) {
        if (pname == AL_GAIN) {
            listenerGain(value);
        }
    }

    public static void alListeneri(int pname, int value) {
        alListenerf(pname, value);
    }

    public static void alDistanceModel(int model) {
        distanceModel(model);
    }

    public static void alDopplerFactor(float value) {
    }

    public static void alDopplerVelocity(float value) {
    }

    public static int alGetError() {
        return AL_NO_ERROR;
    }

    public static String alGetString(int pname) {
        switch (pname) {
            case AL_VENDOR:
                return "Web Audio";
            case AL_VERSION:
                return "1.1";
            case AL_RENDERER:
                return "RetroAL";
            default:
                return "";
        }
    }

    public static boolean alIsExtensionPresent(String name) {
        return false;
    }

    public static int alGetEnumValue(String name) {
        return 0;
    }

    public static void alEnable(int capability) {
    }

    public static void alDisable(int capability) {
    }

    public static boolean alIsEnabled(int capability) {
        return false;
    }

    public static int alGetInteger(int pname) {
        return 0;
    }

    public static float alGetFloat(int pname) {
        return 0;
    }
}
