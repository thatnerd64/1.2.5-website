package org.teavm.classlib.java.util.logging;

import java.io.Serializable;

public class TLevel implements Serializable {
    public static final TLevel OFF = new TLevel("OFF", Integer.MAX_VALUE);
    public static final TLevel SEVERE = new TLevel("SEVERE", 1000);
    public static final TLevel WARNING = new TLevel("WARNING", 900);
    public static final TLevel INFO = new TLevel("INFO", 800);
    public static final TLevel CONFIG = new TLevel("CONFIG", 700);
    public static final TLevel FINE = new TLevel("FINE", 500);
    public static final TLevel FINER = new TLevel("FINER", 400);
    public static final TLevel FINEST = new TLevel("FINEST", 300);
    public static final TLevel ALL = new TLevel("ALL", Integer.MIN_VALUE);
    private static final TLevel[] KNOWN = { OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL };

    private final String name;
    private final int value;
    private final String resourceBundleName;

    protected TLevel(String name, int value) {
        this(name, value, null);
    }

    protected TLevel(String name, int value, String resourceBundleName) {
        this.name = name;
        this.value = value;
        this.resourceBundleName = resourceBundleName;
    }

    public String getName() {
        return name;
    }

    public String getLocalizedName() {
        return name;
    }

    public String getResourceBundleName() {
        return resourceBundleName;
    }

    public final int intValue() {
        return value;
    }

    public static TLevel parse(String name) {
        for (TLevel level : KNOWN) {
            if (level.name.equals(name)) {
                return level;
            }
        }
        try {
            int v = Integer.parseInt(name);
            for (TLevel level : KNOWN) {
                if (level.value == v) {
                    return level;
                }
            }
            return new TLevel(name, v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad level \"" + name + "\"");
        }
    }

    @Override
    public final String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TLevel && ((TLevel) o).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
