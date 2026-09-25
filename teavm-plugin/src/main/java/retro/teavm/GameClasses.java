package retro.teavm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Class lists written by the prepare step, passed to TeaVM through system properties. */
final class GameClasses {
    private static Set<String> all;
    private static List<String> byName;
    private static List<String> reflectMethods;

    private GameClasses() {
    }

    static synchronized Set<String> all() {
        if (all == null) {
            all = new HashSet<>(read("retro.classes"));
        }
        return all;
    }

    static synchronized List<String> byName() {
        if (byName == null) {
            byName = read("retro.byname");
        }
        return byName;
    }

    static synchronized List<String> reflectMethods() {
        if (reflectMethods == null) {
            reflectMethods = read("retro.methods");
        }
        return reflectMethods;
    }

    private static List<String> read(String property) {
        String file = System.getProperty(property);
        if (file == null) {
            return List.of();
        }
        try {
            return Files.readAllLines(Path.of(file)).stream().filter(s -> !s.isBlank()).toList();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + file, e);
        }
    }
}
