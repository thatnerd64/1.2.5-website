package org.apache.logging.log4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replacement for log4j's ThreadContext: one global map and stack (the browser has a single logical thread of
 * interest).
 */
public final class ThreadContext {
    public static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
    public static final org.apache.logging.log4j.spi.ThreadContextStack EMPTY_STACK =
            new org.apache.logging.log4j.spi.MutableThreadContextStack(new ArrayList<String>());

    private static final Map<String, String> MAP = new HashMap<>();
    private static final Stack STACK = new Stack(new ArrayList<>());

    private ThreadContext() {
    }

    public static void put(String key, String value) {
        MAP.put(key, value);
    }

    public static String get(String key) {
        return MAP.get(key);
    }

    public static void remove(String key) {
        MAP.remove(key);
    }

    public static void clear() {
        MAP.clear();
    }

    public static boolean containsKey(String key) {
        return MAP.containsKey(key);
    }

    public static Map<String, String> getContext() {
        return new HashMap<>(MAP);
    }

    public static Map<String, String> getImmutableContext() {
        return Collections.unmodifiableMap(new HashMap<>(MAP));
    }

    public static boolean isEmpty() {
        return MAP.isEmpty();
    }

    public static void clearStack() {
        STACK.clear();
    }

    public static ContextStack cloneStack() {
        return STACK.copy();
    }

    public static ContextStack getImmutableStack() {
        return new Stack(Collections.unmodifiableList(new ArrayList<>(STACK.items)));
    }

    public static void setStack(Collection<String> stack) {
        STACK.items.clear();
        STACK.items.addAll(stack);
    }

    public static int getDepth() {
        return STACK.getDepth();
    }

    public static String pop() {
        return STACK.pop();
    }

    public static String peek() {
        return STACK.peek();
    }

    public static void push(String message) {
        STACK.push(message);
    }

    public static void push(String message, Object... args) {
        STACK.push(new org.apache.logging.log4j.message.ParameterizedMessage(message, args).getFormattedMessage());
    }

    public static void removeStack() {
        STACK.clear();
    }

    public static void trim(int depth) {
        STACK.trim(depth);
    }

    /** log4j's nested-diagnostic-context stack (the game jar's copy is replaced along with this class). */
    public interface ContextStack extends java.io.Serializable {
        void clear();

        String pop();

        String peek();

        void push(String message);

        int getDepth();

        List<String> asList();

        void trim(int depth);

        ContextStack copy();
    }

    private static final class Stack implements ContextStack {
        final List<String> items;

        Stack(List<String> items) {
            this.items = items;
        }

        @Override
        public void clear() {
            items.clear();
        }

        @Override
        public String pop() {
            return items.isEmpty() ? "" : items.remove(items.size() - 1);
        }

        @Override
        public String peek() {
            return items.isEmpty() ? "" : items.get(items.size() - 1);
        }

        @Override
        public void push(String message) {
            items.add(message);
        }

        @Override
        public int getDepth() {
            return items.size();
        }

        @Override
        public List<String> asList() {
            return Collections.unmodifiableList(items);
        }

        @Override
        public void trim(int depth) {
            while (items.size() > Math.max(depth, 0)) {
                items.remove(items.size() - 1);
            }
        }

        @Override
        public ContextStack copy() {
            return new Stack(new ArrayList<>(items));
        }
    }
}
