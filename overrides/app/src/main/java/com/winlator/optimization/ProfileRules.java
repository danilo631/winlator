package com.winlator.optimization;

import java.util.Map;

/**
 * Pure-Java, game-scoped compatibility settings.
 *
 * This is deliberately NOT a replacement for Box64, FEX, Wine or Proton.
 * Settings are applied to the existing per-game / per-container UI.
 */
public final class ProfileRules {
    public static final int BALANCED = 0;
    public static final int PERFORMANCE = 1;
    public static final int COMPATIBILITY = 2;
    public static final int LOW_HEAT = 3;
    public static final int MALI_SAFE = 4;
    public static final int DXVK_DIAGNOSTICS = 5;
    public static final int DXVK_HUD_OFF = 6;

    public static final String[] LABELS = {
        "Balanced - Box64 intermediate",
        "Performance - experimental",
        "Compatibility - safer Box64",
        "Lower heat - conservative",
        "Mali - safe graphics environment",
        "DXVK - FPS / frametime diagnostic",
        "DXVK - hide diagnostic HUD"
    };

    public static final String[] DESCRIPTIONS = {
        "Moderate Box64 translation settings and a persistent shader cache. Results depend on the game.",
        "More aggressive Box64 translation and GL threading. May increase performance or cause visual bugs.",
        "Conservative CPU emulation. Disables Esync and GL threading for troubleshooting.",
        "Conservative CPU emulation and less aggressive GL threading; not a guaranteed power saving.",
        "Removes Turnip/Zink-specific user overrides and uses conservative CPU emulation. Choose Vortek + Gladio or VirGL manually.",
        "Enables DXVK FPS/frametime display. Only affects games actually using DXVK.",
        "Removes the DXVK_HUD environment override, preserving other settings."
    };

    private ProfileRules() {}

    /**
     * Mutates only documented settings; unrelated user overrides are preserved.
     * Returns an existing Box64 preset ID or null to keep the current selection.
     */
    public static String apply(int profile, Map<String, String> vars) {
        if (vars == null) throw new IllegalArgumentException("vars cannot be null");
        switch (profile) {
            case BALANCED:
                shaderCache(vars, "256MB");
                vars.put("WINEESYNC", "1");
                vars.remove("mesa_glthread");
                return "INTERMEDIATE";
            case PERFORMANCE:
                shaderCache(vars, "512MB");
                vars.put("WINEESYNC", "1");
                vars.put("mesa_glthread", "true");
                return "PERFORMANCE";
            case COMPATIBILITY:
                shaderCache(vars, "256MB");
                vars.put("WINEESYNC", "0");
                vars.put("mesa_glthread", "false");
                return "STABILITY";
            case LOW_HEAT:
                shaderCache(vars, "128MB");
                vars.put("WINEESYNC", "1");
                vars.put("mesa_glthread", "false");
                return "CONSERVATIVE";
            case MALI_SAFE:
                shaderCache(vars, "256MB");
                vars.remove("TU_DEBUG");
                vars.remove("TU_OVERRIDE_HEAP_SIZE");
                vars.remove("ZINK_DEBUG");
                vars.remove("ZINK_DESCRIPTORS");
                vars.remove("MESA_VK_WSI_PRESENT_MODE");
                vars.remove("MESA_VK_WSI_DEBUG");
                vars.remove("mesa_glthread");
                return "CONSERVATIVE";
            case DXVK_DIAGNOSTICS:
                vars.put("DXVK_HUD", "fps,frametimes");
                vars.put("DXVK_LOG_LEVEL", "warn");
                return null;
            case DXVK_HUD_OFF:
                vars.remove("DXVK_HUD");
                return null;
            default:
                throw new IllegalArgumentException("Unknown profile: " + profile);
        }
    }

    private static void shaderCache(Map<String, String> vars, String size) {
        vars.put("MESA_SHADER_CACHE_DISABLE", "false");
        vars.put("MESA_SHADER_CACHE_MAX_SIZE", size);
    }

    public static String newContainerEnv(String currentDefaults, boolean adreno) {
        if (adreno) return currentDefaults;
        // No TU_DEBUG/ZINK-specific knobs in a new generic (Mali) container.
        return "MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=256MB WINEESYNC=1";
    }
}
