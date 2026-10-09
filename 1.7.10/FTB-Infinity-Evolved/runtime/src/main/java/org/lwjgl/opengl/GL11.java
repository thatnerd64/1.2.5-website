package org.lwjgl.opengl;

/** LWJGL GL11 for the browser (see retro.gl.GLEmu). Constants from LWJGL 2.9.3. */
public final class GL11 {
    public static final int GL_ACCUM = 256;
    public static final int GL_LOAD = 257;
    public static final int GL_RETURN = 258;
    public static final int GL_MULT = 259;
    public static final int GL_ADD = 260;
    public static final int GL_NEVER = 512;
    public static final int GL_LESS = 513;
    public static final int GL_EQUAL = 514;
    public static final int GL_LEQUAL = 515;
    public static final int GL_GREATER = 516;
    public static final int GL_NOTEQUAL = 517;
    public static final int GL_GEQUAL = 518;
    public static final int GL_ALWAYS = 519;
    public static final int GL_CURRENT_BIT = 1;
    public static final int GL_POINT_BIT = 2;
    public static final int GL_LINE_BIT = 4;
    public static final int GL_POLYGON_BIT = 8;
    public static final int GL_POLYGON_STIPPLE_BIT = 16;
    public static final int GL_PIXEL_MODE_BIT = 32;
    public static final int GL_LIGHTING_BIT = 64;
    public static final int GL_FOG_BIT = 128;
    public static final int GL_DEPTH_BUFFER_BIT = 256;
    public static final int GL_ACCUM_BUFFER_BIT = 512;
    public static final int GL_STENCIL_BUFFER_BIT = 1024;
    public static final int GL_VIEWPORT_BIT = 2048;
    public static final int GL_TRANSFORM_BIT = 0x1000;
    public static final int GL_ENABLE_BIT = 0x2000;
    public static final int GL_COLOR_BUFFER_BIT = 0x4000;
    public static final int GL_HINT_BIT = 0x8000;
    public static final int GL_EVAL_BIT = 0x10000;
    public static final int GL_LIST_BIT = 0x20000;
    public static final int GL_TEXTURE_BIT = 0x40000;
    public static final int GL_SCISSOR_BIT = 0x80000;
    public static final int GL_ALL_ATTRIB_BITS = 0xfffff;
    public static final int GL_POINTS = 0;
    public static final int GL_LINES = 1;
    public static final int GL_LINE_LOOP = 2;
    public static final int GL_LINE_STRIP = 3;
    public static final int GL_TRIANGLES = 4;
    public static final int GL_TRIANGLE_STRIP = 5;
    public static final int GL_TRIANGLE_FAN = 6;
    public static final int GL_QUADS = 7;
    public static final int GL_QUAD_STRIP = 8;
    public static final int GL_POLYGON = 9;
    public static final int GL_ZERO = 0;
    public static final int GL_ONE = 1;
    public static final int GL_SRC_COLOR = 768;
    public static final int GL_ONE_MINUS_SRC_COLOR = 769;
    public static final int GL_SRC_ALPHA = 770;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 771;
    public static final int GL_DST_ALPHA = 772;
    public static final int GL_ONE_MINUS_DST_ALPHA = 773;
    public static final int GL_DST_COLOR = 774;
    public static final int GL_ONE_MINUS_DST_COLOR = 775;
    public static final int GL_SRC_ALPHA_SATURATE = 776;
    public static final int GL_CONSTANT_COLOR = 0x8001;
    public static final int GL_ONE_MINUS_CONSTANT_COLOR = 0x8002;
    public static final int GL_CONSTANT_ALPHA = 0x8003;
    public static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;
    public static final int GL_TRUE = 1;
    public static final int GL_FALSE = 0;
    public static final int GL_CLIP_PLANE0 = 0x3000;
    public static final int GL_CLIP_PLANE1 = 0x3001;
    public static final int GL_CLIP_PLANE2 = 0x3002;
    public static final int GL_CLIP_PLANE3 = 0x3003;
    public static final int GL_CLIP_PLANE4 = 0x3004;
    public static final int GL_CLIP_PLANE5 = 0x3005;
    public static final int GL_BYTE = 0x1400;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_SHORT = 0x1402;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_INT = 0x1404;
    public static final int GL_UNSIGNED_INT = 0x1405;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_2_BYTES = 0x1407;
    public static final int GL_3_BYTES = 0x1408;
    public static final int GL_4_BYTES = 0x1409;
    public static final int GL_DOUBLE = 0x140a;
    public static final int GL_NONE = 0;
    public static final int GL_FRONT_LEFT = 1024;
    public static final int GL_FRONT_RIGHT = 1025;
    public static final int GL_BACK_LEFT = 1026;
    public static final int GL_BACK_RIGHT = 1027;
    public static final int GL_FRONT = 1028;
    public static final int GL_BACK = 1029;
    public static final int GL_LEFT = 1030;
    public static final int GL_RIGHT = 1031;
    public static final int GL_FRONT_AND_BACK = 1032;
    public static final int GL_AUX0 = 1033;
    public static final int GL_AUX1 = 1034;
    public static final int GL_AUX2 = 1035;
    public static final int GL_AUX3 = 1036;
    public static final int GL_NO_ERROR = 0;
    public static final int GL_INVALID_ENUM = 1280;
    public static final int GL_INVALID_VALUE = 1281;
    public static final int GL_INVALID_OPERATION = 1282;
    public static final int GL_STACK_OVERFLOW = 1283;
    public static final int GL_STACK_UNDERFLOW = 1284;
    public static final int GL_OUT_OF_MEMORY = 1285;
    public static final int GL_2D = 1536;
    public static final int GL_3D = 1537;
    public static final int GL_3D_COLOR = 1538;
    public static final int GL_3D_COLOR_TEXTURE = 1539;
    public static final int GL_4D_COLOR_TEXTURE = 1540;
    public static final int GL_PASS_THROUGH_TOKEN = 1792;
    public static final int GL_POINT_TOKEN = 1793;
    public static final int GL_LINE_TOKEN = 1794;
    public static final int GL_POLYGON_TOKEN = 1795;
    public static final int GL_BITMAP_TOKEN = 1796;
    public static final int GL_DRAW_PIXEL_TOKEN = 1797;
    public static final int GL_COPY_PIXEL_TOKEN = 1798;
    public static final int GL_LINE_RESET_TOKEN = 1799;
    public static final int GL_EXP = 2048;
    public static final int GL_EXP2 = 2049;
    public static final int GL_CW = 2304;
    public static final int GL_CCW = 2305;
    public static final int GL_COEFF = 2560;
    public static final int GL_ORDER = 2561;
    public static final int GL_DOMAIN = 2562;
    public static final int GL_CURRENT_COLOR = 2816;
    public static final int GL_CURRENT_INDEX = 2817;
    public static final int GL_CURRENT_NORMAL = 2818;
    public static final int GL_CURRENT_TEXTURE_COORDS = 2819;
    public static final int GL_CURRENT_RASTER_COLOR = 2820;
    public static final int GL_CURRENT_RASTER_INDEX = 2821;
    public static final int GL_CURRENT_RASTER_TEXTURE_COORDS = 2822;
    public static final int GL_CURRENT_RASTER_POSITION = 2823;
    public static final int GL_CURRENT_RASTER_POSITION_VALID = 2824;
    public static final int GL_CURRENT_RASTER_DISTANCE = 2825;
    public static final int GL_POINT_SMOOTH = 2832;
    public static final int GL_POINT_SIZE = 2833;
    public static final int GL_POINT_SIZE_RANGE = 2834;
    public static final int GL_POINT_SIZE_GRANULARITY = 2835;
    public static final int GL_LINE_SMOOTH = 2848;
    public static final int GL_LINE_WIDTH = 2849;
    public static final int GL_LINE_WIDTH_RANGE = 2850;
    public static final int GL_LINE_WIDTH_GRANULARITY = 2851;
    public static final int GL_LINE_STIPPLE = 2852;
    public static final int GL_LINE_STIPPLE_PATTERN = 2853;
    public static final int GL_LINE_STIPPLE_REPEAT = 2854;
    public static final int GL_LIST_MODE = 2864;
    public static final int GL_MAX_LIST_NESTING = 2865;
    public static final int GL_LIST_BASE = 2866;
    public static final int GL_LIST_INDEX = 2867;
    public static final int GL_POLYGON_MODE = 2880;
    public static final int GL_POLYGON_SMOOTH = 2881;
    public static final int GL_POLYGON_STIPPLE = 2882;
    public static final int GL_EDGE_FLAG = 2883;
    public static final int GL_CULL_FACE = 2884;
    public static final int GL_CULL_FACE_MODE = 2885;
    public static final int GL_FRONT_FACE = 2886;
    public static final int GL_LIGHTING = 2896;
    public static final int GL_LIGHT_MODEL_LOCAL_VIEWER = 2897;
    public static final int GL_LIGHT_MODEL_TWO_SIDE = 2898;
    public static final int GL_LIGHT_MODEL_AMBIENT = 2899;
    public static final int GL_SHADE_MODEL = 2900;
    public static final int GL_COLOR_MATERIAL_FACE = 2901;
    public static final int GL_COLOR_MATERIAL_PARAMETER = 2902;
    public static final int GL_COLOR_MATERIAL = 2903;
    public static final int GL_FOG = 2912;
    public static final int GL_FOG_INDEX = 2913;
    public static final int GL_FOG_DENSITY = 2914;
    public static final int GL_FOG_START = 2915;
    public static final int GL_FOG_END = 2916;
    public static final int GL_FOG_MODE = 2917;
    public static final int GL_FOG_COLOR = 2918;
    public static final int GL_DEPTH_RANGE = 2928;
    public static final int GL_DEPTH_TEST = 2929;
    public static final int GL_DEPTH_WRITEMASK = 2930;
    public static final int GL_DEPTH_CLEAR_VALUE = 2931;
    public static final int GL_DEPTH_FUNC = 2932;
    public static final int GL_ACCUM_CLEAR_VALUE = 2944;
    public static final int GL_STENCIL_TEST = 2960;
    public static final int GL_STENCIL_CLEAR_VALUE = 2961;
    public static final int GL_STENCIL_FUNC = 2962;
    public static final int GL_STENCIL_VALUE_MASK = 2963;
    public static final int GL_STENCIL_FAIL = 2964;
    public static final int GL_STENCIL_PASS_DEPTH_FAIL = 2965;
    public static final int GL_STENCIL_PASS_DEPTH_PASS = 2966;
    public static final int GL_STENCIL_REF = 2967;
    public static final int GL_STENCIL_WRITEMASK = 2968;
    public static final int GL_MATRIX_MODE = 2976;
    public static final int GL_NORMALIZE = 2977;
    public static final int GL_VIEWPORT = 2978;
    public static final int GL_MODELVIEW_STACK_DEPTH = 2979;
    public static final int GL_PROJECTION_STACK_DEPTH = 2980;
    public static final int GL_TEXTURE_STACK_DEPTH = 2981;
    public static final int GL_MODELVIEW_MATRIX = 2982;
    public static final int GL_PROJECTION_MATRIX = 2983;
    public static final int GL_TEXTURE_MATRIX = 2984;
    public static final int GL_ATTRIB_STACK_DEPTH = 2992;
    public static final int GL_CLIENT_ATTRIB_STACK_DEPTH = 2993;
    public static final int GL_ALPHA_TEST = 3008;
    public static final int GL_ALPHA_TEST_FUNC = 3009;
    public static final int GL_ALPHA_TEST_REF = 3010;
    public static final int GL_DITHER = 3024;
    public static final int GL_BLEND_DST = 3040;
    public static final int GL_BLEND_SRC = 3041;
    public static final int GL_BLEND = 3042;
    public static final int GL_LOGIC_OP_MODE = 3056;
    public static final int GL_INDEX_LOGIC_OP = 3057;
    public static final int GL_COLOR_LOGIC_OP = 3058;
    public static final int GL_AUX_BUFFERS = 3072;
    public static final int GL_DRAW_BUFFER = 3073;
    public static final int GL_READ_BUFFER = 3074;
    public static final int GL_SCISSOR_BOX = 3088;
    public static final int GL_SCISSOR_TEST = 3089;
    public static final int GL_INDEX_CLEAR_VALUE = 3104;
    public static final int GL_INDEX_WRITEMASK = 3105;
    public static final int GL_COLOR_CLEAR_VALUE = 3106;
    public static final int GL_COLOR_WRITEMASK = 3107;
    public static final int GL_INDEX_MODE = 3120;
    public static final int GL_RGBA_MODE = 3121;
    public static final int GL_DOUBLEBUFFER = 3122;
    public static final int GL_STEREO = 3123;
    public static final int GL_RENDER_MODE = 3136;
    public static final int GL_PERSPECTIVE_CORRECTION_HINT = 3152;
    public static final int GL_POINT_SMOOTH_HINT = 3153;
    public static final int GL_LINE_SMOOTH_HINT = 3154;
    public static final int GL_POLYGON_SMOOTH_HINT = 3155;
    public static final int GL_FOG_HINT = 3156;
    public static final int GL_TEXTURE_GEN_S = 3168;
    public static final int GL_TEXTURE_GEN_T = 3169;
    public static final int GL_TEXTURE_GEN_R = 3170;
    public static final int GL_TEXTURE_GEN_Q = 3171;
    public static final int GL_PIXEL_MAP_I_TO_I = 3184;
    public static final int GL_PIXEL_MAP_S_TO_S = 3185;
    public static final int GL_PIXEL_MAP_I_TO_R = 3186;
    public static final int GL_PIXEL_MAP_I_TO_G = 3187;
    public static final int GL_PIXEL_MAP_I_TO_B = 3188;
    public static final int GL_PIXEL_MAP_I_TO_A = 3189;
    public static final int GL_PIXEL_MAP_R_TO_R = 3190;
    public static final int GL_PIXEL_MAP_G_TO_G = 3191;
    public static final int GL_PIXEL_MAP_B_TO_B = 3192;
    public static final int GL_PIXEL_MAP_A_TO_A = 3193;
    public static final int GL_PIXEL_MAP_I_TO_I_SIZE = 3248;
    public static final int GL_PIXEL_MAP_S_TO_S_SIZE = 3249;
    public static final int GL_PIXEL_MAP_I_TO_R_SIZE = 3250;
    public static final int GL_PIXEL_MAP_I_TO_G_SIZE = 3251;
    public static final int GL_PIXEL_MAP_I_TO_B_SIZE = 3252;
    public static final int GL_PIXEL_MAP_I_TO_A_SIZE = 3253;
    public static final int GL_PIXEL_MAP_R_TO_R_SIZE = 3254;
    public static final int GL_PIXEL_MAP_G_TO_G_SIZE = 3255;
    public static final int GL_PIXEL_MAP_B_TO_B_SIZE = 3256;
    public static final int GL_PIXEL_MAP_A_TO_A_SIZE = 3257;
    public static final int GL_UNPACK_SWAP_BYTES = 3312;
    public static final int GL_UNPACK_LSB_FIRST = 3313;
    public static final int GL_UNPACK_ROW_LENGTH = 3314;
    public static final int GL_UNPACK_SKIP_ROWS = 3315;
    public static final int GL_UNPACK_SKIP_PIXELS = 3316;
    public static final int GL_UNPACK_ALIGNMENT = 3317;
    public static final int GL_PACK_SWAP_BYTES = 3328;
    public static final int GL_PACK_LSB_FIRST = 3329;
    public static final int GL_PACK_ROW_LENGTH = 3330;
    public static final int GL_PACK_SKIP_ROWS = 3331;
    public static final int GL_PACK_SKIP_PIXELS = 3332;
    public static final int GL_PACK_ALIGNMENT = 3333;
    public static final int GL_MAP_COLOR = 3344;
    public static final int GL_MAP_STENCIL = 3345;
    public static final int GL_INDEX_SHIFT = 3346;
    public static final int GL_INDEX_OFFSET = 3347;
    public static final int GL_RED_SCALE = 3348;
    public static final int GL_RED_BIAS = 3349;
    public static final int GL_ZOOM_X = 3350;
    public static final int GL_ZOOM_Y = 3351;
    public static final int GL_GREEN_SCALE = 3352;
    public static final int GL_GREEN_BIAS = 3353;
    public static final int GL_BLUE_SCALE = 3354;
    public static final int GL_BLUE_BIAS = 3355;
    public static final int GL_ALPHA_SCALE = 3356;
    public static final int GL_ALPHA_BIAS = 3357;
    public static final int GL_DEPTH_SCALE = 3358;
    public static final int GL_DEPTH_BIAS = 3359;
    public static final int GL_MAX_EVAL_ORDER = 3376;
    public static final int GL_MAX_LIGHTS = 3377;
    public static final int GL_MAX_CLIP_PLANES = 3378;
    public static final int GL_MAX_TEXTURE_SIZE = 3379;
    public static final int GL_MAX_PIXEL_MAP_TABLE = 3380;
    public static final int GL_MAX_ATTRIB_STACK_DEPTH = 3381;
    public static final int GL_MAX_MODELVIEW_STACK_DEPTH = 3382;
    public static final int GL_MAX_NAME_STACK_DEPTH = 3383;
    public static final int GL_MAX_PROJECTION_STACK_DEPTH = 3384;
    public static final int GL_MAX_TEXTURE_STACK_DEPTH = 3385;
    public static final int GL_MAX_VIEWPORT_DIMS = 3386;
    public static final int GL_MAX_CLIENT_ATTRIB_STACK_DEPTH = 3387;
    public static final int GL_SUBPIXEL_BITS = 3408;
    public static final int GL_INDEX_BITS = 3409;
    public static final int GL_RED_BITS = 3410;
    public static final int GL_GREEN_BITS = 3411;
    public static final int GL_BLUE_BITS = 3412;
    public static final int GL_ALPHA_BITS = 3413;
    public static final int GL_DEPTH_BITS = 3414;
    public static final int GL_STENCIL_BITS = 3415;
    public static final int GL_ACCUM_RED_BITS = 3416;
    public static final int GL_ACCUM_GREEN_BITS = 3417;
    public static final int GL_ACCUM_BLUE_BITS = 3418;
    public static final int GL_ACCUM_ALPHA_BITS = 3419;
    public static final int GL_NAME_STACK_DEPTH = 3440;
    public static final int GL_AUTO_NORMAL = 3456;
    public static final int GL_MAP1_COLOR_4 = 3472;
    public static final int GL_MAP1_INDEX = 3473;
    public static final int GL_MAP1_NORMAL = 3474;
    public static final int GL_MAP1_TEXTURE_COORD_1 = 3475;
    public static final int GL_MAP1_TEXTURE_COORD_2 = 3476;
    public static final int GL_MAP1_TEXTURE_COORD_3 = 3477;
    public static final int GL_MAP1_TEXTURE_COORD_4 = 3478;
    public static final int GL_MAP1_VERTEX_3 = 3479;
    public static final int GL_MAP1_VERTEX_4 = 3480;
    public static final int GL_MAP2_COLOR_4 = 3504;
    public static final int GL_MAP2_INDEX = 3505;
    public static final int GL_MAP2_NORMAL = 3506;
    public static final int GL_MAP2_TEXTURE_COORD_1 = 3507;
    public static final int GL_MAP2_TEXTURE_COORD_2 = 3508;
    public static final int GL_MAP2_TEXTURE_COORD_3 = 3509;
    public static final int GL_MAP2_TEXTURE_COORD_4 = 3510;
    public static final int GL_MAP2_VERTEX_3 = 3511;
    public static final int GL_MAP2_VERTEX_4 = 3512;
    public static final int GL_MAP1_GRID_DOMAIN = 3536;
    public static final int GL_MAP1_GRID_SEGMENTS = 3537;
    public static final int GL_MAP2_GRID_DOMAIN = 3538;
    public static final int GL_MAP2_GRID_SEGMENTS = 3539;
    public static final int GL_TEXTURE_1D = 3552;
    public static final int GL_TEXTURE_2D = 3553;
    public static final int GL_FEEDBACK_BUFFER_POINTER = 3568;
    public static final int GL_FEEDBACK_BUFFER_SIZE = 3569;
    public static final int GL_FEEDBACK_BUFFER_TYPE = 3570;
    public static final int GL_SELECTION_BUFFER_POINTER = 3571;
    public static final int GL_SELECTION_BUFFER_SIZE = 3572;
    public static final int GL_TEXTURE_WIDTH = 0x1000;
    public static final int GL_TEXTURE_HEIGHT = 0x1001;
    public static final int GL_TEXTURE_INTERNAL_FORMAT = 0x1003;
    public static final int GL_TEXTURE_BORDER_COLOR = 0x1004;
    public static final int GL_TEXTURE_BORDER = 0x1005;
    public static final int GL_DONT_CARE = 0x1100;
    public static final int GL_FASTEST = 0x1101;
    public static final int GL_NICEST = 0x1102;
    public static final int GL_LIGHT0 = 0x4000;
    public static final int GL_LIGHT1 = 0x4001;
    public static final int GL_LIGHT2 = 0x4002;
    public static final int GL_LIGHT3 = 0x4003;
    public static final int GL_LIGHT4 = 0x4004;
    public static final int GL_LIGHT5 = 0x4005;
    public static final int GL_LIGHT6 = 0x4006;
    public static final int GL_LIGHT7 = 0x4007;
    public static final int GL_AMBIENT = 0x1200;
    public static final int GL_DIFFUSE = 0x1201;
    public static final int GL_SPECULAR = 0x1202;
    public static final int GL_POSITION = 0x1203;
    public static final int GL_SPOT_DIRECTION = 0x1204;
    public static final int GL_SPOT_EXPONENT = 0x1205;
    public static final int GL_SPOT_CUTOFF = 0x1206;
    public static final int GL_CONSTANT_ATTENUATION = 0x1207;
    public static final int GL_LINEAR_ATTENUATION = 0x1208;
    public static final int GL_QUADRATIC_ATTENUATION = 0x1209;
    public static final int GL_COMPILE = 0x1300;
    public static final int GL_COMPILE_AND_EXECUTE = 0x1301;
    public static final int GL_CLEAR = 0x1500;
    public static final int GL_AND = 0x1501;
    public static final int GL_AND_REVERSE = 0x1502;
    public static final int GL_COPY = 0x1503;
    public static final int GL_AND_INVERTED = 0x1504;
    public static final int GL_NOOP = 0x1505;
    public static final int GL_XOR = 0x1506;
    public static final int GL_OR = 0x1507;
    public static final int GL_NOR = 0x1508;
    public static final int GL_EQUIV = 0x1509;
    public static final int GL_INVERT = 0x150a;
    public static final int GL_OR_REVERSE = 0x150b;
    public static final int GL_COPY_INVERTED = 0x150c;
    public static final int GL_OR_INVERTED = 0x150d;
    public static final int GL_NAND = 0x150e;
    public static final int GL_SET = 0x150f;
    public static final int GL_EMISSION = 0x1600;
    public static final int GL_SHININESS = 0x1601;
    public static final int GL_AMBIENT_AND_DIFFUSE = 0x1602;
    public static final int GL_COLOR_INDEXES = 0x1603;
    public static final int GL_MODELVIEW = 0x1700;
    public static final int GL_PROJECTION = 0x1701;
    public static final int GL_TEXTURE = 0x1702;
    public static final int GL_COLOR = 0x1800;
    public static final int GL_DEPTH = 0x1801;
    public static final int GL_STENCIL = 0x1802;
    public static final int GL_COLOR_INDEX = 0x1900;
    public static final int GL_STENCIL_INDEX = 0x1901;
    public static final int GL_DEPTH_COMPONENT = 0x1902;
    public static final int GL_RED = 0x1903;
    public static final int GL_GREEN = 0x1904;
    public static final int GL_BLUE = 0x1905;
    public static final int GL_ALPHA = 0x1906;
    public static final int GL_RGB = 0x1907;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_LUMINANCE = 0x1909;
    public static final int GL_LUMINANCE_ALPHA = 0x190a;
    public static final int GL_BITMAP = 0x1a00;
    public static final int GL_POINT = 0x1b00;
    public static final int GL_LINE = 0x1b01;
    public static final int GL_FILL = 0x1b02;
    public static final int GL_RENDER = 0x1c00;
    public static final int GL_FEEDBACK = 0x1c01;
    public static final int GL_SELECT = 0x1c02;
    public static final int GL_FLAT = 0x1d00;
    public static final int GL_SMOOTH = 0x1d01;
    public static final int GL_KEEP = 0x1e00;
    public static final int GL_REPLACE = 0x1e01;
    public static final int GL_INCR = 0x1e02;
    public static final int GL_DECR = 0x1e03;
    public static final int GL_VENDOR = 0x1f00;
    public static final int GL_RENDERER = 0x1f01;
    public static final int GL_VERSION = 0x1f02;
    public static final int GL_EXTENSIONS = 0x1f03;
    public static final int GL_S = 0x2000;
    public static final int GL_T = 0x2001;
    public static final int GL_R = 0x2002;
    public static final int GL_Q = 0x2003;
    public static final int GL_MODULATE = 0x2100;
    public static final int GL_DECAL = 0x2101;
    public static final int GL_TEXTURE_ENV_MODE = 0x2200;
    public static final int GL_TEXTURE_ENV_COLOR = 0x2201;
    public static final int GL_TEXTURE_ENV = 0x2300;
    public static final int GL_EYE_LINEAR = 0x2400;
    public static final int GL_OBJECT_LINEAR = 0x2401;
    public static final int GL_SPHERE_MAP = 0x2402;
    public static final int GL_TEXTURE_GEN_MODE = 0x2500;
    public static final int GL_OBJECT_PLANE = 0x2501;
    public static final int GL_EYE_PLANE = 0x2502;
    public static final int GL_NEAREST = 0x2600;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
    public static final int GL_LINEAR_MIPMAP_NEAREST = 0x2701;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_WRAP_S = 0x2802;
    public static final int GL_TEXTURE_WRAP_T = 0x2803;
    public static final int GL_CLAMP = 0x2900;
    public static final int GL_REPEAT = 0x2901;
    public static final int GL_CLIENT_PIXEL_STORE_BIT = 1;
    public static final int GL_CLIENT_VERTEX_ARRAY_BIT = 2;
    public static final int GL_ALL_CLIENT_ATTRIB_BITS = 0xffffffff;
    public static final int GL_POLYGON_OFFSET_FACTOR = 0x8038;
    public static final int GL_POLYGON_OFFSET_UNITS = 0x2a00;
    public static final int GL_POLYGON_OFFSET_POINT = 0x2a01;
    public static final int GL_POLYGON_OFFSET_LINE = 0x2a02;
    public static final int GL_POLYGON_OFFSET_FILL = 0x8037;
    public static final int GL_ALPHA4 = 0x803b;
    public static final int GL_ALPHA8 = 0x803c;
    public static final int GL_ALPHA12 = 0x803d;
    public static final int GL_ALPHA16 = 0x803e;
    public static final int GL_LUMINANCE4 = 0x803f;
    public static final int GL_LUMINANCE8 = 0x8040;
    public static final int GL_LUMINANCE12 = 0x8041;
    public static final int GL_LUMINANCE16 = 0x8042;
    public static final int GL_LUMINANCE4_ALPHA4 = 0x8043;
    public static final int GL_LUMINANCE6_ALPHA2 = 0x8044;
    public static final int GL_LUMINANCE8_ALPHA8 = 0x8045;
    public static final int GL_LUMINANCE12_ALPHA4 = 0x8046;
    public static final int GL_LUMINANCE12_ALPHA12 = 0x8047;
    public static final int GL_LUMINANCE16_ALPHA16 = 0x8048;
    public static final int GL_INTENSITY = 0x8049;
    public static final int GL_INTENSITY4 = 0x804a;
    public static final int GL_INTENSITY8 = 0x804b;
    public static final int GL_INTENSITY12 = 0x804c;
    public static final int GL_INTENSITY16 = 0x804d;
    public static final int GL_R3_G3_B2 = 0x2a10;
    public static final int GL_RGB4 = 0x804f;
    public static final int GL_RGB5 = 0x8050;
    public static final int GL_RGB8 = 0x8051;
    public static final int GL_RGB10 = 0x8052;
    public static final int GL_RGB12 = 0x8053;
    public static final int GL_RGB16 = 0x8054;
    public static final int GL_RGBA2 = 0x8055;
    public static final int GL_RGBA4 = 0x8056;
    public static final int GL_RGB5_A1 = 0x8057;
    public static final int GL_RGBA8 = 0x8058;
    public static final int GL_RGB10_A2 = 0x8059;
    public static final int GL_RGBA12 = 0x805a;
    public static final int GL_RGBA16 = 0x805b;
    public static final int GL_TEXTURE_RED_SIZE = 0x805c;
    public static final int GL_TEXTURE_GREEN_SIZE = 0x805d;
    public static final int GL_TEXTURE_BLUE_SIZE = 0x805e;
    public static final int GL_TEXTURE_ALPHA_SIZE = 0x805f;
    public static final int GL_TEXTURE_LUMINANCE_SIZE = 0x8060;
    public static final int GL_TEXTURE_INTENSITY_SIZE = 0x8061;
    public static final int GL_PROXY_TEXTURE_1D = 0x8063;
    public static final int GL_PROXY_TEXTURE_2D = 0x8064;
    public static final int GL_TEXTURE_PRIORITY = 0x8066;
    public static final int GL_TEXTURE_RESIDENT = 0x8067;
    public static final int GL_TEXTURE_BINDING_1D = 0x8068;
    public static final int GL_TEXTURE_BINDING_2D = 0x8069;
    public static final int GL_VERTEX_ARRAY = 0x8074;
    public static final int GL_NORMAL_ARRAY = 0x8075;
    public static final int GL_COLOR_ARRAY = 0x8076;
    public static final int GL_INDEX_ARRAY = 0x8077;
    public static final int GL_TEXTURE_COORD_ARRAY = 0x8078;
    public static final int GL_EDGE_FLAG_ARRAY = 0x8079;
    public static final int GL_VERTEX_ARRAY_SIZE = 0x807a;
    public static final int GL_VERTEX_ARRAY_TYPE = 0x807b;
    public static final int GL_VERTEX_ARRAY_STRIDE = 0x807c;
    public static final int GL_NORMAL_ARRAY_TYPE = 0x807e;
    public static final int GL_NORMAL_ARRAY_STRIDE = 0x807f;
    public static final int GL_COLOR_ARRAY_SIZE = 0x8081;
    public static final int GL_COLOR_ARRAY_TYPE = 0x8082;
    public static final int GL_COLOR_ARRAY_STRIDE = 0x8083;
    public static final int GL_INDEX_ARRAY_TYPE = 0x8085;
    public static final int GL_INDEX_ARRAY_STRIDE = 0x8086;
    public static final int GL_TEXTURE_COORD_ARRAY_SIZE = 0x8088;
    public static final int GL_TEXTURE_COORD_ARRAY_TYPE = 0x8089;
    public static final int GL_TEXTURE_COORD_ARRAY_STRIDE = 0x808a;
    public static final int GL_EDGE_FLAG_ARRAY_STRIDE = 0x808c;
    public static final int GL_VERTEX_ARRAY_POINTER = 0x808e;
    public static final int GL_NORMAL_ARRAY_POINTER = 0x808f;
    public static final int GL_COLOR_ARRAY_POINTER = 0x8090;
    public static final int GL_INDEX_ARRAY_POINTER = 0x8091;
    public static final int GL_TEXTURE_COORD_ARRAY_POINTER = 0x8092;
    public static final int GL_EDGE_FLAG_ARRAY_POINTER = 0x8093;
    public static final int GL_V2F = 0x2a20;
    public static final int GL_V3F = 0x2a21;
    public static final int GL_C4UB_V2F = 0x2a22;
    public static final int GL_C4UB_V3F = 0x2a23;
    public static final int GL_C3F_V3F = 0x2a24;
    public static final int GL_N3F_V3F = 0x2a25;
    public static final int GL_C4F_N3F_V3F = 0x2a26;
    public static final int GL_T2F_V3F = 0x2a27;
    public static final int GL_T4F_V4F = 0x2a28;
    public static final int GL_T2F_C4UB_V3F = 0x2a29;
    public static final int GL_T2F_C3F_V3F = 0x2a2a;
    public static final int GL_T2F_N3F_V3F = 0x2a2b;
    public static final int GL_T2F_C4F_N3F_V3F = 0x2a2c;
    public static final int GL_T4F_C4F_N3F_V4F = 0x2a2d;
    public static final int GL_LOGIC_OP = 3057;
    public static final int GL_TEXTURE_COMPONENTS = 0x1003;

    private GL11() {
    }

    static float[] floats(java.nio.FloatBuffer b, int n) {
        float[] out = new float[n];
        int p = b.position();
        for (int i = 0; i < n && p + i < b.limit(); i++) {
            out[i] = b.get(p + i);
        }
        return out;
    }

    // ---- matrices ----
    public static void glMatrixMode(int mode) { retro.gl.GLEmu.matrixMode(mode); }
    public static void glPushMatrix() { retro.gl.GLEmu.pushMatrix(); }
    public static void glPopMatrix() { retro.gl.GLEmu.popMatrix(); }
    public static void glLoadIdentity() { retro.gl.GLEmu.loadIdentity(); }
    public static void glTranslatef(float x, float y, float z) { retro.gl.GLEmu.translate(x, y, z); }
    public static void glTranslated(double x, double y, double z) { retro.gl.GLEmu.translate((float) x, (float) y, (float) z); }
    public static void glScalef(float x, float y, float z) { retro.gl.GLEmu.scale(x, y, z); }
    public static void glScaled(double x, double y, double z) { retro.gl.GLEmu.scale((float) x, (float) y, (float) z); }
    public static void glRotatef(float a, float x, float y, float z) { retro.gl.GLEmu.rotate(a, x, y, z); }
    public static void glRotated(double a, double x, double y, double z) { retro.gl.GLEmu.rotate((float) a, (float) x, (float) y, (float) z); }
    public static void glOrtho(double l, double r, double b, double t, double n, double f) { retro.gl.GLEmu.ortho(l, r, b, t, n, f); }
    public static void glFrustum(double l, double r, double b, double t, double n, double f) { retro.gl.GLEmu.frustum(l, r, b, t, n, f); }
    public static void glMultMatrix(java.nio.FloatBuffer m) { retro.gl.GLEmu.multMatrix(floats(m, 16)); }
    public static void glLoadMatrix(java.nio.FloatBuffer m) { retro.gl.GLEmu.loadMatrix(floats(m, 16)); }
    public static void glMultMatrix(java.nio.DoubleBuffer m) {
        float[] f = new float[16];
        for (int i = 0; i < 16; i++) { f[i] = (float) m.get(m.position() + i); }
        retro.gl.GLEmu.multMatrix(f);
    }
    public static void glLoadMatrix(java.nio.DoubleBuffer m) {
        float[] f = new float[16];
        for (int i = 0; i < 16; i++) { f[i] = (float) m.get(m.position() + i); }
        retro.gl.GLEmu.loadMatrix(f);
    }

    // ---- state ----
    public static void glEnable(int cap) { retro.gl.GLEmu.enable(cap); }
    public static void glDisable(int cap) { retro.gl.GLEmu.disable(cap); }
    public static boolean glIsEnabled(int cap) { return retro.gl.GLEmu.isEnabled(cap); }
    public static void glBlendFunc(int s, int d) { retro.gl.GLEmu.blendFunc(s, d); }
    public static void glAlphaFunc(int func, float ref) { retro.gl.GLEmu.alphaFunc(func, ref); }
    public static void glDepthFunc(int func) { retro.gl.GLEmu.depthFunc(func); }
    public static void glDepthMask(boolean flag) { retro.gl.GLEmu.depthMask(flag); }
    public static void glColorMask(boolean r, boolean g, boolean b, boolean a) { retro.gl.GLEmu.colorMask(r, g, b, a); }
    public static void glCullFace(int mode) { retro.gl.GLEmu.cullFace(mode); }
    public static void glFrontFace(int mode) { retro.gl.GLEmu.frontFace(mode); }
    public static void glPolygonOffset(float factor, float units) { retro.gl.GLEmu.polygonOffset(factor, units); }
    public static void glPolygonMode(int face, int mode) { }
    public static void glLineWidth(float w) { retro.gl.GLEmu.lineWidth(w); }
    public static void glPointSize(float size) { }
    public static void glShadeModel(int mode) { }
    public static void glHint(int target, int mode) { }
    public static void glLogicOp(int op) { }
    public static void glColorMaterial(int face, int mode) { }
    public static void glViewport(int x, int y, int w, int h) { retro.gl.GLEmu.viewport(x, y, w, h); }
    public static void glScissor(int x, int y, int w, int h) { retro.gl.GLEmu.scissor(x, y, w, h); }
    public static void glClear(int mask) { retro.gl.GLEmu.clear(mask); }
    public static void glClearColor(float r, float g, float b, float a) { retro.gl.GLEmu.clearColor(r, g, b, a); }
    public static void glClearDepth(double d) { retro.gl.GLEmu.clearDepth(d); }
    public static void glClearStencil(int s) { retro.gl.GLEmu.clearStencil(s); }
    public static void glStencilFunc(int func, int ref, int mask) { retro.gl.GLEmu.stencilFunc(func, ref, mask); }
    public static void glStencilOp(int fail, int zfail, int zpass) { retro.gl.GLEmu.stencilOp(fail, zfail, zpass); }
    public static void glStencilMask(int mask) { retro.gl.GLEmu.stencilMask(mask); }
    public static void glFlush() { retro.gl.GLEmu.flush(); }
    public static void glFinish() { retro.gl.GLEmu.finish(); }
    public static int glGetError() { return 0; }
    public static void glPushAttrib(int mask) { retro.gl.GLEmu.pushAttrib(mask); }
    public static void glPopAttrib() { retro.gl.GLEmu.popAttrib(); }
    public static void glPushClientAttrib(int mask) { }
    public static void glPopClientAttrib() { }
    public static void glTexEnvi(int target, int pname, int param) { }
    public static void glTexEnvf(int target, int pname, float param) { }
    public static void glTexEnv(int target, int pname, java.nio.FloatBuffer params) { }
    public static void glTexEnv(int target, int pname, java.nio.IntBuffer params) { }
    public static void glMaterial(int face, int pname, java.nio.FloatBuffer params) { }
    public static void glMaterialf(int face, int pname, float param) { }
    public static void glMateriali(int face, int pname, int param) { }
    public static void glDepthRange(double near, double far) { }
    public static void glLightModeli(int pname, int param) { }
    public static void glLightModelf(int pname, float param) { }

    // ---- current attributes ----
    public static void glColor3f(float r, float g, float b) { retro.gl.GLEmu.color(r, g, b, 1); }
    public static void glColor4f(float r, float g, float b, float a) { retro.gl.GLEmu.color(r, g, b, a); }
    public static void glColor3d(double r, double g, double b) { retro.gl.GLEmu.color((float) r, (float) g, (float) b, 1); }
    public static void glColor4d(double r, double g, double b, double a) { retro.gl.GLEmu.color((float) r, (float) g, (float) b, (float) a); }
    public static void glColor3ub(byte r, byte g, byte b) { retro.gl.GLEmu.color((r & 255) / 255f, (g & 255) / 255f, (b & 255) / 255f, 1); }
    public static void glColor4ub(byte r, byte g, byte b, byte a) { retro.gl.GLEmu.color((r & 255) / 255f, (g & 255) / 255f, (b & 255) / 255f, (a & 255) / 255f); }
    public static void glColor3b(byte r, byte g, byte b) { retro.gl.GLEmu.color(r / 127f, g / 127f, b / 127f, 1); }
    public static void glColor4b(byte r, byte g, byte b, byte a) { retro.gl.GLEmu.color(r / 127f, g / 127f, b / 127f, a / 127f); }
    public static void glNormal3f(float x, float y, float z) { retro.gl.GLEmu.normal(x, y, z); }
    public static void glNormal3d(double x, double y, double z) { retro.gl.GLEmu.normal((float) x, (float) y, (float) z); }
    public static void glNormal3b(byte x, byte y, byte z) { retro.gl.GLEmu.normal(x / 127f, y / 127f, z / 127f); }
    public static void glNormal3i(int x, int y, int z) { retro.gl.GLEmu.normal(x, y, z); }
    public static void glTexCoord2f(float s, float t) { retro.gl.GLEmu.texCoord(s, t); }
    public static void glTexCoord2d(double s, double t) { retro.gl.GLEmu.texCoord((float) s, (float) t); }
    public static void glTexCoord1f(float s) { retro.gl.GLEmu.texCoord(s, 0); }

    // ---- immediate mode ----
    public static void glBegin(int mode) { retro.gl.GLEmu.begin(mode); }
    public static void glEnd() { retro.gl.GLEmu.end(); }
    public static void glVertex2f(float x, float y) { retro.gl.GLEmu.vertex(x, y, 0); }
    public static void glVertex2d(double x, double y) { retro.gl.GLEmu.vertex((float) x, (float) y, 0); }
    public static void glVertex2i(int x, int y) { retro.gl.GLEmu.vertex(x, y, 0); }
    public static void glVertex3f(float x, float y, float z) { retro.gl.GLEmu.vertex(x, y, z); }
    public static void glVertex3d(double x, double y, double z) { retro.gl.GLEmu.vertex((float) x, (float) y, (float) z); }
    public static void glVertex3i(int x, int y, int z) { retro.gl.GLEmu.vertex(x, y, z); }
    public static void glRectf(float x1, float y1, float x2, float y2) {
        retro.gl.GLEmu.begin(GL_QUADS); retro.gl.GLEmu.vertex(x1, y1, 0); retro.gl.GLEmu.vertex(x2, y1, 0); retro.gl.GLEmu.vertex(x2, y2, 0); retro.gl.GLEmu.vertex(x1, y2, 0); retro.gl.GLEmu.end();
    }
    public static void glRecti(int x1, int y1, int x2, int y2) { glRectf(x1, y1, x2, y2); }
    public static void glRectd(double x1, double y1, double x2, double y2) { glRectf((float) x1, (float) y1, (float) x2, (float) y2); }

    // ---- lighting / fog / texgen ----
    public static void glLight(int light, int pname, java.nio.FloatBuffer params) { retro.gl.GLEmu.light(light, pname, floats(params, 4)); }
    public static void glLightf(int light, int pname, float param) { retro.gl.GLEmu.light(light, pname, new float[] { param, 0, 0, 0 }); }
    public static void glLighti(int light, int pname, int param) { retro.gl.GLEmu.light(light, pname, new float[] { param, 0, 0, 0 }); }
    public static void glLightModel(int pname, java.nio.FloatBuffer params) { retro.gl.GLEmu.lightModel(pname, floats(params, 4)); }
    public static void glFogi(int pname, int param) { retro.gl.GLEmu.fogi(pname, param); }
    public static void glFogf(int pname, float param) { retro.gl.GLEmu.fogf(pname, param); }
    public static void glFog(int pname, java.nio.FloatBuffer params) { retro.gl.GLEmu.fogv(pname, floats(params, 4)); }
    public static void glFog(int pname, java.nio.IntBuffer params) { retro.gl.GLEmu.fogi(pname, params.get(params.position())); }
    public static void glTexGeni(int coord, int pname, int param) { retro.gl.GLEmu.texGeni(coord, pname, param); }
    public static void glTexGenf(int coord, int pname, float param) { retro.gl.GLEmu.texGeni(coord, pname, (int) param); }
    public static void glTexGen(int coord, int pname, java.nio.FloatBuffer params) { retro.gl.GLEmu.texGen(coord, pname, floats(params, 4)); }

    // ---- textures ----
    public static int glGenTextures() { return retro.gl.GLEmu.genTexture(); }
    public static void glGenTextures(java.nio.IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) { ids.put(i, retro.gl.GLEmu.genTexture()); }
    }
    public static void glDeleteTextures(int id) { retro.gl.GLEmu.deleteTexture(id); }
    public static void glDeleteTextures(java.nio.IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) { retro.gl.GLEmu.deleteTexture(ids.get(i)); }
    }
    public static boolean glIsTexture(int id) { return id > 0; }
    public static void glBindTexture(int target, int id) { retro.gl.GLEmu.bindTexture(target, id); }
    public static void glTexParameteri(int target, int pname, int param) { retro.gl.GLEmu.texParameteri(target, pname, param); }
    public static void glTexParameterf(int target, int pname, float param) { retro.gl.GLEmu.texParameteri(target, pname, (int) param); }
    public static void glTexParameter(int target, int pname, java.nio.IntBuffer params) { retro.gl.GLEmu.texParameteri(target, pname, params.get(params.position())); }
    public static void glTexParameter(int target, int pname, java.nio.FloatBuffer params) { retro.gl.GLEmu.texParameteri(target, pname, (int) params.get(params.position())); }
    public static void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, java.nio.ByteBuffer pixels) {
        retro.gl.GLEmu.texImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
    }
    public static void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, java.nio.IntBuffer pixels) {
        retro.gl.GLEmu.texImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
    }
    public static void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, java.nio.FloatBuffer pixels) {
        retro.gl.GLEmu.texImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
    }
    public static void glTexSubImage2D(int target, int level, int x, int y, int width, int height, int format, int type, java.nio.ByteBuffer pixels) {
        retro.gl.GLEmu.texSubImage2D(target, level, x, y, width, height, format, type, pixels);
    }
    public static void glTexSubImage2D(int target, int level, int x, int y, int width, int height, int format, int type, java.nio.IntBuffer pixels) {
        retro.gl.GLEmu.texSubImage2D(target, level, x, y, width, height, format, type, pixels);
    }
    public static void glCopyTexSubImage2D(int target, int level, int xoff, int yoff, int x, int y, int w, int h) {
        retro.gl.GLEmu.copyTexSubImage2D(target, level, xoff, yoff, x, y, w, h);
    }
    public static void glCopyTexImage2D(int target, int level, int internalFormat, int x, int y, int w, int h, int border) {
        retro.gl.GLEmu.texImage2D(target, level, internalFormat, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
        retro.gl.GLEmu.copyTexSubImage2D(target, level, 0, 0, x, y, w, h);
    }
    public static int glGetTexLevelParameteri(int target, int level, int pname) { return retro.gl.GLEmu.getTexLevelParameteri(target, level, pname); }
    public static void glGetTexLevelParameter(int target, int level, int pname, java.nio.IntBuffer params) {
        params.put(params.position(), retro.gl.GLEmu.getTexLevelParameteri(target, level, pname));
    }
    public static void glPixelStorei(int pname, int param) { retro.gl.GLEmu.pixelStorei(pname, param); }
    public static void glPixelStoref(int pname, float param) { retro.gl.GLEmu.pixelStorei(pname, (int) param); }
    public static void glReadPixels(int x, int y, int w, int h, int format, int type, java.nio.ByteBuffer pixels) { retro.gl.GLEmu.readPixels(x, y, w, h, format, type, pixels); }
    public static void glReadPixels(int x, int y, int w, int h, int format, int type, java.nio.IntBuffer pixels) { retro.gl.GLEmu.readPixels(x, y, w, h, format, type, pixels); }
    public static void glReadPixels(int x, int y, int w, int h, int format, int type, java.nio.FloatBuffer pixels) { retro.gl.GLEmu.readPixels(x, y, w, h, format, type, pixels); }

    // ---- vertex arrays ----
    public static void glEnableClientState(int cap) { retro.gl.GLEmu.enableClientState(cap); }
    public static void glDisableClientState(int cap) { retro.gl.GLEmu.disableClientState(cap); }
    public static void glVertexPointer(int size, int stride, java.nio.FloatBuffer b) { retro.gl.GLEmu.vertexPointer(size, GL_FLOAT, stride, b); }
    public static void glVertexPointer(int size, int stride, java.nio.DoubleBuffer b) { retro.gl.GLEmu.vertexPointer(size, GL_FLOAT, stride, b); }
    public static void glVertexPointer(int size, int stride, java.nio.IntBuffer b) { retro.gl.GLEmu.vertexPointer(size, GL_INT, stride, b); }
    public static void glVertexPointer(int size, int stride, java.nio.ShortBuffer b) { retro.gl.GLEmu.vertexPointer(size, GL_SHORT, stride, b); }
    public static void glVertexPointer(int size, int type, int stride, java.nio.ByteBuffer b) { retro.gl.GLEmu.vertexPointer(size, type, stride, b); }
    public static void glVertexPointer(int size, int type, int stride, long offset) { retro.gl.GLEmu.vertexPointer(size, type, stride, offset); }
    public static void glColorPointer(int size, int stride, java.nio.FloatBuffer b) { retro.gl.GLEmu.colorPointer(size, GL_FLOAT, stride, b); }
    public static void glColorPointer(int size, boolean unsigned, int stride, java.nio.ByteBuffer b) { retro.gl.GLEmu.colorPointer(size, unsigned ? GL_UNSIGNED_BYTE : GL_BYTE, stride, b); }
    public static void glColorPointer(int size, int type, int stride, java.nio.ByteBuffer b) { retro.gl.GLEmu.colorPointer(size, type, stride, b); }
    public static void glColorPointer(int size, int type, int stride, long offset) { retro.gl.GLEmu.colorPointer(size, type, stride, offset); }
    public static void glNormalPointer(int stride, java.nio.ByteBuffer b) { retro.gl.GLEmu.normalPointer(GL_BYTE, stride, b); }
    public static void glNormalPointer(int stride, java.nio.FloatBuffer b) { retro.gl.GLEmu.normalPointer(GL_FLOAT, stride, b); }
    public static void glNormalPointer(int stride, java.nio.IntBuffer b) { retro.gl.GLEmu.normalPointer(GL_INT, stride, b); }
    public static void glNormalPointer(int type, int stride, java.nio.ByteBuffer b) { retro.gl.GLEmu.normalPointer(type, stride, b); }
    public static void glNormalPointer(int type, int stride, long offset) { retro.gl.GLEmu.normalPointer(type, stride, offset); }
    public static void glTexCoordPointer(int size, int stride, java.nio.FloatBuffer b) { retro.gl.GLEmu.texCoordPointer(size, GL_FLOAT, stride, b); }
    public static void glTexCoordPointer(int size, int stride, java.nio.ShortBuffer b) { retro.gl.GLEmu.texCoordPointer(size, GL_SHORT, stride, b); }
    public static void glTexCoordPointer(int size, int stride, java.nio.IntBuffer b) { retro.gl.GLEmu.texCoordPointer(size, GL_INT, stride, b); }
    public static void glTexCoordPointer(int size, int stride, java.nio.DoubleBuffer b) { retro.gl.GLEmu.texCoordPointer(size, GL_FLOAT, stride, b); }
    public static void glTexCoordPointer(int size, int type, int stride, java.nio.ByteBuffer b) { retro.gl.GLEmu.texCoordPointer(size, type, stride, b); }
    public static void glTexCoordPointer(int size, int type, int stride, long offset) { retro.gl.GLEmu.texCoordPointer(size, type, stride, offset); }
    public static void glDrawArrays(int mode, int first, int count) { retro.gl.GLEmu.drawArrays(mode, first, count); }
    public static void glDrawElements(int mode, java.nio.IntBuffer indices) { retro.gl.GLEmu.drawElements(mode, indices.remaining(), GL_UNSIGNED_INT, indices); }
    public static void glDrawElements(int mode, java.nio.ShortBuffer indices) { retro.gl.GLEmu.drawElements(mode, indices.remaining(), GL_UNSIGNED_SHORT, indices); }
    public static void glDrawElements(int mode, java.nio.ByteBuffer indices) { retro.gl.GLEmu.drawElements(mode, indices.remaining(), GL_UNSIGNED_BYTE, indices); }
    public static void glArrayElement(int i) { }

    // ---- display lists ----
    public static int glGenLists(int range) { return retro.gl.GLEmu.genLists(range); }
    public static void glNewList(int list, int mode) { retro.gl.GLEmu.newList(list, mode); }
    public static void glEndList() { retro.gl.GLEmu.endList(); }
    public static void glCallList(int list) { retro.gl.GLEmu.callList(list); }
    public static void glCallLists(java.nio.IntBuffer lists) {
        for (int i = lists.position(); i < lists.limit(); i++) { retro.gl.GLEmu.callList(lists.get(i)); }
    }
    public static void glCallLists(java.nio.ByteBuffer lists) {
        for (int i = lists.position(); i < lists.limit(); i++) { retro.gl.GLEmu.callList(lists.get(i) & 255); }
    }
    public static void glDeleteLists(int list, int range) { retro.gl.GLEmu.deleteLists(list, range); }
    public static boolean glIsList(int list) { return retro.gl.GLEmu.isList(list); }
    public static void glListBase(int base) { }

    // ---- queries ----
    public static void glGetFloat(int pname, java.nio.FloatBuffer params) {
        float[] v = retro.gl.GLEmu.getFloats(pname);
        int p = params.position();
        for (int i = 0; i < v.length && p + i < params.limit(); i++) { params.put(p + i, v[i]); }
    }
    public static float glGetFloat(int pname) { return retro.gl.GLEmu.getFloats(pname)[0]; }
    public static void glGetDouble(int pname, java.nio.DoubleBuffer params) {
        float[] v = retro.gl.GLEmu.getFloats(pname);
        int p = params.position();
        for (int i = 0; i < v.length && p + i < params.limit(); i++) { params.put(p + i, v[i]); }
    }
    public static void glGetInteger(int pname, java.nio.IntBuffer params) {
        int[] v = retro.gl.GLEmu.getIntegers(pname);
        int p = params.position();
        for (int i = 0; i < v.length && p + i < params.limit(); i++) { params.put(p + i, v[i]); }
    }
    public static int glGetInteger(int pname) { return retro.gl.GLEmu.getIntegers(pname)[0]; }
    public static boolean glGetBoolean(int pname) { return retro.gl.GLEmu.getBoolean(pname); }
    public static void glGetBoolean(int pname, java.nio.ByteBuffer params) { params.put(params.position(), (byte) (retro.gl.GLEmu.getBoolean(pname) ? 1 : 0)); }
    public static String glGetString(int name) { return retro.gl.GLEmu.getString(name); }

    public static void glGetTexImage(int target, int level, int format, int type, java.nio.IntBuffer pixels) {
        // only used for screenshots of single textures; not available on WebGL
    }
    public static float glGetTexParameterf(int target, int pname) { return 0f; }
    public static int glGetTexParameteri(int target, int pname) { return 0; }
}
