package dev.vortex.module;

/** Tiny flag that tells the config manager something changed and should be written to disk. */
public final class ConfigDirty {
    private static volatile boolean dirty;

    private ConfigDirty() {}

    public static void mark() { dirty = true; }

    public static boolean consume() {
        boolean was = dirty;
        dirty = false;
        return was;
    }
}
