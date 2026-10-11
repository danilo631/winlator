package com.winlator.optimization;

import java.util.Map;

/** User-controlled compatibility knobs recognized by some Android Proton Wine builds. */
public final class ProtonSyncRules {
    public static final int AUTOMATIC = 0;
    public static final int USERSPACE_NTSYNC = 1;
    public static final int DISABLE_NTSYNC = 2;
    public static final int ESYNC = 3;
    public static final String[] LABELS = {
        "Proton Wine - Automatic (recommended)",
        "Proton Wine - Force userspace NTsync",
        "Proton Wine - Disable NTsync (troubleshooting)",
        "Wine - ESYNC fallback"
    };
    public static final String[] DETAILS = {
        "Remove only our NTsync overrides. Let a compatible Proton Wine determine the backend.",
        "For supported Proton Wine Bionic builds only. Sets PROTON_NO_KERNEL_NTSYNC=1. May increase CPU usage.",
        "For supported Proton Wine Bionic builds only. Disable NTsync to compare compatibility.",
        "Use Wine ESYNC where supported, explicitly disabling Proton NTsync. Not all Wine builds support these variables."
    };
    private ProtonSyncRules() {}

    public static void apply(int choice, Map<String, String> vars) {
        if (vars == null) throw new IllegalArgumentException("Null environment");
        switch (choice) {
            case AUTOMATIC:
                vars.remove("PROTON_NO_KERNEL_NTSYNC");
                vars.remove("PROTON_NO_NTSYNC");
                break;
            case USERSPACE_NTSYNC:
                vars.remove("PROTON_NO_NTSYNC");
                vars.put("PROTON_NO_KERNEL_NTSYNC", "1");
                break;
            case DISABLE_NTSYNC:
                vars.remove("PROTON_NO_KERNEL_NTSYNC");
                vars.put("PROTON_NO_NTSYNC", "1");
                break;
            case ESYNC:
                vars.put("PROTON_NO_NTSYNC", "1");
                vars.remove("PROTON_NO_KERNEL_NTSYNC");
                vars.put("WINEESYNC", "1");
                break;
            default:
                throw new IllegalArgumentException("Unknown Proton synchronization mode");
        }
    }
}
