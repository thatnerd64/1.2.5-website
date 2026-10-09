package retro.compat;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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
    private static final Map<Class<?>, Info> INFOS = new HashMap<>();

    private GenericSignatures() {
    }

    /** What a class's Signature attribute says: its type parameters, generic superclass and interfaces. */
    private static final class Info {
        TypeVariable<?>[] params = new TypeVariable<?>[0];
        Type superType;
        Type[] interfaces = new Type[0];
    }

    private static final String E = "<E:Ljava/lang/Object;>";
    private static final String KV = "<K:Ljava/lang/Object;V:Ljava/lang/Object;>";
    private static final String OBJ = "Ljava/lang/Object;";

    /**
     * The generic shape of the JDK collection and map types (TeaVM's class library keeps no Signature attributes).
     * Gson resolves the element type of a {@code Set<X>} field by walking Set -> Collection through
     * getGenericInterfaces(), so without these every collection element is read as a plain object.
     */
    private static void builtins(Map<String, String> into) {
        String[][] collections = {
            {"java.lang.Iterable", "<T:Ljava/lang/Object;>" + OBJ},
            {"java.util.Collection", E + OBJ + "Ljava/lang/Iterable<TE;>;"},
            {"java.util.SequencedCollection", E + OBJ + "Ljava/util/Collection<TE;>;"},
            {"java.util.Set", E + OBJ + "Ljava/util/Collection<TE;>;"},
            {"java.util.SequencedSet", E + OBJ + "Ljava/util/SequencedCollection<TE;>;Ljava/util/Set<TE;>;"},
            {"java.util.List", E + OBJ + "Ljava/util/SequencedCollection<TE;>;"},
            {"java.util.Queue", E + OBJ + "Ljava/util/Collection<TE;>;"},
            {"java.util.Deque", E + OBJ + "Ljava/util/Queue<TE;>;Ljava/util/SequencedCollection<TE;>;"},
            {"java.util.SortedSet", E + OBJ + "Ljava/util/Set<TE;>;Ljava/util/SequencedSet<TE;>;"},
            {"java.util.NavigableSet", E + OBJ + "Ljava/util/SortedSet<TE;>;"},
            {"java.util.AbstractCollection", E + OBJ + "Ljava/util/Collection<TE;>;"},
            {"java.util.AbstractList", E + "Ljava/util/AbstractCollection<TE;>;Ljava/util/List<TE;>;"},
            {"java.util.AbstractSequentialList", E + "Ljava/util/AbstractList<TE;>;"},
            {"java.util.AbstractSet", E + "Ljava/util/AbstractCollection<TE;>;Ljava/util/Set<TE;>;"},
            {"java.util.ArrayList", E + "Ljava/util/AbstractList<TE;>;Ljava/util/List<TE;>;"},
            {"java.util.LinkedList", E + "Ljava/util/AbstractSequentialList<TE;>;Ljava/util/List<TE;>;Ljava/util/Deque<TE;>;"},
            {"java.util.ArrayDeque", E + "Ljava/util/AbstractCollection<TE;>;Ljava/util/Deque<TE;>;"},
            {"java.util.Vector", E + "Ljava/util/AbstractList<TE;>;Ljava/util/List<TE;>;"},
            {"java.util.HashSet", E + "Ljava/util/AbstractSet<TE;>;Ljava/util/Set<TE;>;"},
            {"java.util.LinkedHashSet", E + "Ljava/util/HashSet<TE;>;Ljava/util/SequencedSet<TE;>;Ljava/util/Set<TE;>;"},
            {"java.util.TreeSet", E + "Ljava/util/AbstractSet<TE;>;Ljava/util/NavigableSet<TE;>;"},
            {"java.util.Map", KV + OBJ},
            {"java.util.SequencedMap", KV + OBJ + "Ljava/util/Map<TK;TV;>;"},
            {"java.util.SortedMap", KV + OBJ + "Ljava/util/SequencedMap<TK;TV;>;"},
            {"java.util.NavigableMap", KV + OBJ + "Ljava/util/SortedMap<TK;TV;>;"},
            {"java.util.concurrent.ConcurrentMap", KV + OBJ + "Ljava/util/Map<TK;TV;>;"},
            {"java.util.AbstractMap", KV + OBJ + "Ljava/util/Map<TK;TV;>;"},
            {"java.util.HashMap", KV + "Ljava/util/AbstractMap<TK;TV;>;Ljava/util/Map<TK;TV;>;"},
            {"java.util.LinkedHashMap", KV + "Ljava/util/HashMap<TK;TV;>;Ljava/util/SequencedMap<TK;TV;>;Ljava/util/Map<TK;TV;>;"},
            {"java.util.TreeMap", KV + "Ljava/util/AbstractMap<TK;TV;>;Ljava/util/NavigableMap<TK;TV;>;"},
            {"java.util.Hashtable", KV + "Ljava/util/Dictionary<TK;TV;>;Ljava/util/Map<TK;TV;>;"},
            {"java.util.Dictionary", KV + OBJ},
            {"java.util.concurrent.ConcurrentHashMap", KV + "Ljava/util/AbstractMap<TK;TV;>;Ljava/util/concurrent/ConcurrentMap<TK;TV;>;"},
        };
        for (String[] c : collections) {
            into.putIfAbsent(c[0], c[1]);
        }
    }

    private static synchronized Map<String, String> signatures() {
        if (signatures == null) {
            signatures = new HashMap<>();
            builtins(signatures);
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

    private static synchronized Info info(Class<?> cls) {
        Info info = INFOS.get(cls);
        if (info == null) {
            info = new Info();
            String sig = signatures().get(cls.getName());
            if (sig != null) {
                try {
                    Parser p = new Parser(sig, cls);
                    info.params = p.parseTypeParameters();
                    // (visible before the supertypes are parsed: their type variables look up the declaring class's
                    // parameters, this class's included)
                    INFOS.put(cls, info);
                    info.superType = p.parseType();
                    java.util.ArrayList<Type> itfs = new java.util.ArrayList<>();
                    while (p.hasMore()) {
                        itfs.add(p.parseType());
                    }
                    // getGenericInterfaces()[i] must describe getInterfaces()[i]; the class library's interface lists
                    // differ from the JDK's (extra marker interfaces, another order), so match them up by raw type
                    Class<?>[] raw = cls.getInterfaces();
                    Type[] aligned = new Type[raw.length];
                    for (int i = 0; i < raw.length; i++) {
                        aligned[i] = raw[i];
                        for (Type t : itfs) {
                            Type r = t instanceof ParameterizedType ? ((ParameterizedType) t).getRawType() : t;
                            if (r == raw[i]) {
                                aligned[i] = t;
                                break;
                            }
                        }
                    }
                    info.interfaces = aligned;
                } catch (RuntimeException e) {
                    TypeVariable<?>[] params = info.params;
                    info = new Info();
                    info.params = params != null ? params : info.params;
                }
            }
            INFOS.put(cls, info);
        }
        return info;
    }

    static Type genericSuperclass(Class<?> cls) {
        Info info = info(cls);
        return info.superType != null ? info.superType : cls.getSuperclass();
    }

    static Type[] genericInterfaces(Class<?> cls) {
        Info info = info(cls);
        if (info.superType != null) {
            return info.interfaces.clone();
        }
        Class<?>[] raw = cls.getInterfaces();
        Type[] result = new Type[raw.length];
        System.arraycopy(raw, 0, result, 0, raw.length);
        return result;
    }

    static TypeVariable<?>[] typeParameters(Class<?> cls) {
        return info(cls).params.clone();
    }

    /** A type variable declared by a class. */
    /**
     * The class declaring a type variable a signature refers to: the class itself, or an enclosing class (an
     * anonymous {@code new TypeToken<C>() {}} refers to its outer class's C). Guava matches type variables by
     * declaration, so this must be the declaring class, as on a JVM.
     */
    static Class<?> declarationOf(String name, Class<?> from) {
        for (Class<?> c = from; c != null; c = outerClass(c)) {
            for (TypeVariable<?> v : typeParameters(c)) {
                if (v.getName().equals(name)) {
                    return c;
                }
            }
        }
        return from;
    }

    private static Class<?> outerClass(Class<?> c) {
        String n = c.getName();
        int i = n.lastIndexOf('$');
        if (i <= 0) {
            return null;
        }
        try {
            return Class.forName(n.substring(0, i));
        } catch (ClassNotFoundException | RuntimeException e) {
            return null;
        }
    }

    static final class TVar implements TypeVariable<Class<?>> {
        private final String name;
        private final Class<?> declaration;

        TVar(String name, Class<?> declaration) {
            this.name = name;
            this.declaration = declaration;
        }

        @Override
        public Type[] getBounds() {
            return new Type[] { Object.class };
        }

        @Override
        public Class<?> getGenericDeclaration() {
            return declaration;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public java.lang.reflect.AnnotatedType[] getAnnotatedBounds() {
            return new java.lang.reflect.AnnotatedType[0];
        }

        @Override
        public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public java.lang.annotation.Annotation[] getAnnotations() {
            return new java.lang.annotation.Annotation[0];
        }

        @Override
        public java.lang.annotation.Annotation[] getDeclaredAnnotations() {
            return new java.lang.annotation.Annotation[0];
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TVar && ((TVar) o).name.equals(name) && ((TVar) o).declaration == declaration;
        }

        @Override
        public int hashCode() {
            return name.hashCode() ^ declaration.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class Parser {
        private final String s;
        private final Class<?> declaring;
        private int pos;

        Parser(String s, Class<?> declaring) {
            this.s = s;
            this.declaring = declaring;
        }

        boolean hasMore() {
            return pos < s.length();
        }

        /** {@code <T:Ljava/lang/Object;U::Ljava/lang/Comparable<TU;>;>} (the bounds are skipped). */
        TypeVariable<?>[] parseTypeParameters() {
            if (pos >= s.length() || s.charAt(pos) != '<') {
                return new TypeVariable<?>[0];
            }
            pos++;
            java.util.ArrayList<TypeVariable<?>> params = new java.util.ArrayList<>();
            while (s.charAt(pos) != '>') {
                int colon = s.indexOf(':', pos);
                params.add(new TVar(s.substring(pos, colon), declaring));
                pos = colon;
                while (pos < s.length() && s.charAt(pos) == ':') {
                    pos++;
                    if (s.charAt(pos) != ':') {
                        parseType();
                    }
                }
            }
            pos++;
            return params.toArray(new TypeVariable<?>[0]);
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
                    String name = s.substring(pos + 1, end);
                    pos = end + 1;
                    return new TVar(name, declarationOf(name, declaring));
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
