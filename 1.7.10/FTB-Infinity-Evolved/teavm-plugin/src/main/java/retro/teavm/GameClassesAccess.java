package retro.teavm;

/** Public view of GameClasses for the patched TeaVM compiler classes in other packages. */
public final class GameClassesAccess {
    private GameClassesAccess() {
    }

    public static boolean isGameClass(String className) {
        return GameClasses.all().contains(className);
    }

    public static boolean isStub(String className) {
        return GameClasses.stubs().contains(className);
    }
}
