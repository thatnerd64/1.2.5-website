package retro.compat;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retro.rt.Resources;

/**
 * {@code Class.getGenericSuperclass()} for classes whose generic superclass matters at runtime (anonymous
 * {@code TypeToken} subclasses used by Gson/Guava). The build records their signatures in
 * {@code retro/generic-supers.txt}; they are parsed here into {@link ParameterizedType} and friends.
 */
final class GenericSignatures {
    private static Map<String, String> signatures;

    private GenericSignatures() {
    }

    private static Map<String, String> signatures() {
        if (signatures == null) {
            signatures = new HashMap<>();
            byte[] data = Resources.read("retro/generic-supers.txt");
            if (data != null) {
                for (String line : new String(data, java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                    int tab = line.indexOf('\t');
                    if (tab > 0) {
                        signatures.put(line.substring(0, tab), line.substring(tab + 1));
                    }
                }
            }
        }
        return signatures;
    }

    static Type genericSuperclass(Class<?> cls) {
        String sig = signatures().get(cls.getName());
        if (sig == null) {
            return cls.getSuperclass();
        }
        Parser p = new Parser(sig);
        p.skipTypeParameters();
        try {
            return p.parseType();
        } catch (RuntimeException e) {
            return cls.getSuperclass();
        }
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        void skipTypeParameters() {
            if (pos < s.length() && s.charAt(pos) == '<') {
                int depth = 0;
                do {
                    char c = s.charAt(pos++);
                    if (c == '<') {
                        depth++;
                    } else if (c == '>') {
                        depth--;
                    }
                } while (depth > 0);
            }
        }

        Type parseType() {
            char c = s.charAt(pos);
            switch (c) {
                case 'L':
                    return parseClassType();
                case '[': {
                    pos++;
                    Type component = parseType();
                    if (component instanceof Class) {
                        return java.lang.reflect.Array.newInstance((Class<?>) component, 0).getClass();
                    }
                    return new GenericArray(component);
                }
                case 'T': {
                    int end = s.indexOf(';', pos);
                    pos = end + 1;
                    return Object.class;
                }
                default:
                    pos++;
                    return primitive(c);
            }
        }

        private Type parseClassType() {
            pos++;
            StringBuilder name = new StringBuilder();
            Type owner = null;
            Type result = null;
            while (true) {
                char c = s.charAt(pos);
                if (c == ';') {
                    pos++;
                    if (result == null) {
                        result = load(name.toString());
                    }
                    return result;
                } else if (c == '<') {
                    pos++;
                    List<Type> args = new ArrayList<>();
                    while (s.charAt(pos) != '>') {
                        char a = s.charAt(pos);
                        if (a == '*') {
                            pos++;
                            args.add(new Wildcard(new Type[] { Object.class }, new Type[0]));
                        } else if (a == '+') {
                            pos++;
                            args.add(new Wildcard(new Type[] { parseType() }, new Type[0]));
                        } else if (a == '-') {
                            pos++;
                            args.add(new Wildcard(new Type[] { Object.class }, new Type[] { parseType() }));
                        } else {
                            args.add(parseType());
                        }
                    }
                    pos++;
                    result = new Parameterized(load(name.toString()), args.toArray(new Type[0]), owner);
                } else if (c == '.') {
                    pos++;
                    owner = result != null ? result : load(name.toString());
                    result = null;
                    name.append('$');
                } else {
                    name.append(c == '/' ? '.' : c);
                    pos++;
                }
            }
        }

        private static Class<?> load(String name) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                return Object.class;
            }
        }

        private static Class<?> primitive(char c) {
            switch (c) {
                case 'Z':
                    return boolean.class;
                case 'B':
                    return byte.class;
                case 'C':
                    return char.class;
                case 'S':
                    return short.class;
                case 'I':
                    return int.class;
                case 'J':
                    return long.class;
                case 'F':
                    return float.class;
                case 'D':
                    return double.class;
                default:
                    return void.class;
            }
        }
    }

    static final class Parameterized implements ParameterizedType {
        private final Class<?> raw;
        private final Type[] args;
        private final Type owner;

        Parameterized(Class<?> raw, Type[] args, Type owner) {
            this.raw = raw;
            this.args = args;
            this.owner = owner;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return args.clone();
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return owner;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ParameterizedType)) {
                return false;
            }
            ParameterizedType p = (ParameterizedType) o;
            return raw.equals(p.getRawType()) && Arrays.equals(args, p.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return raw.hashCode() ^ Arrays.hashCode(args);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(raw.getName()).append('<');
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(args[i] instanceof Class ? ((Class<?>) args[i]).getName() : args[i].toString());
            }
            return sb.append('>').toString();
        }
    }

    static final class GenericArray implements GenericArrayType {
        private final Type component;

        GenericArray(Type component) {
            this.component = component;
        }

        @Override
        public Type getGenericComponentType() {
            return component;
        }
    }

    static final class Wildcard implements WildcardType {
        private final Type[] upper;
        private final Type[] lower;

        Wildcard(Type[] upper, Type[] lower) {
            this.upper = upper;
            this.lower = lower;
        }

        @Override
        public Type[] getUpperBounds() {
            return upper.clone();
        }

        @Override
        public Type[] getLowerBounds() {
            return lower.clone();
        }
    }
}
