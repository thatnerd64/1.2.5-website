package org.teavm.classlib.java.awt;

public interface TLayoutManager {
    default void addLayoutComponent(String name, TComponent comp) {
    }

    default void removeLayoutComponent(TComponent comp) {
    }

    default void layoutContainer(TContainer parent) {
    }
}
