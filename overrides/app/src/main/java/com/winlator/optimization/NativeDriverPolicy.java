package com.winlator.optimization;

import java.util.Locale;

/** Device-only native Vulkan policy. No root, ADB, frequency changes or external drivers. */
public final class NativeDriverPolicy {
    public static final String PREFERENCE = "winlator_safe_native_driver";
    private NativeDriverPolicy() {}

    public enum Family { ADRENO, MALI, XCLIPSE, POWERVR, OTHER }

    public static Family classify(String renderer) {
        if (renderer == null) return Family.OTHER;
        String name = renderer.toLowerCase(Locale.ROOT);
        if (name.contains("adreno")) return Family.ADRENO;
        if (name.contains("mali") || name.contains("immortalis")) return Family.MALI;
        if (name.contains("xclipse") || name.contains("samsung xclipse")) return Family.XCLIPSE;
        if (name.contains("powervr") || name.contains("rogue")) return Family.POWERVR;
        return Family.OTHER;
    }

    /** Guard only confirmed non-Adreno GPUs, not unknown/emulated hardware. */
    public static boolean shouldPreferSystem(String renderer) {
        Family family = classify(renderer);
        return family == Family.MALI || family == Family.XCLIPSE || family == Family.POWERVR;
    }

    public static boolean needsVortek(String renderer, String vulkanDriver) {
        return shouldPreferSystem(renderer) && !"vortek".equals(vulkanDriver);
    }

    /** Use native Vulkan through Winlator's existing Vortek bridge; not direct DX11 calls. */
    public static String recommendation(String renderer) {
        switch (classify(renderer)) {
            case MALI: return "Mali: Vortek + driver System. Turnip não é compatível.";
            case XCLIPSE: return "Xclipse: Vortek + driver System (teste Vulkan necessário).";
            case POWERVR: return "PowerVR: Vortek + driver System, ou VirGL se falhar.";
            case ADRENO: return "Adreno: Turnip se compatível; Vortek + System como alternativa.";
            default: return "GPU não identificada. Preserve o driver atual e faça diagnóstico.";
        }
    }
}
