package com.winlator.optimization;

/** Pure comparison used to skip repeated DXVK configuration writes on launch. */
public final class ConfigFilePolicy {
    private ConfigFilePolicy() {}

    public static boolean needsWrite(boolean exists, String existing, String requested) {
        if (!exists) return true;
        return !requested.equals(existing);
    }
}
