package com.winlator.optimization;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import java.util.ArrayList;
import java.util.List;

/** Read-only power/thermal hints. Does not change CPU clocks, thermal policy or governor. */
public final class ThermalAdvisor {
    private ThermalAdvisor() {}

    public static String warning(Context context, int profile) {
        if (profile != ProfileRules.PERFORMANCE && profile != ProfileRules.BOX64_CACHE_ON)
            return "";
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (power == null) return "";

        List<String> warnings = new ArrayList<>();
        if (power.isPowerSaveMode())
            warnings.add("Battery saver is enabled; performance comparisons may be misleading.");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int status = power.getCurrentThermalStatus();
            if (status >= PowerManager.THERMAL_STATUS_MODERATE)
                warnings.add("Current thermal status: " + thermalName(status)
                    + ". Try a lower render resolution or a frame cap.");
        }
        return String.join("\n", warnings);
    }

    private static String thermalName(int status) {
        switch (status) {
            case PowerManager.THERMAL_STATUS_NONE: return "none";
            case PowerManager.THERMAL_STATUS_LIGHT: return "light";
            case PowerManager.THERMAL_STATUS_MODERATE: return "moderate";
            case PowerManager.THERMAL_STATUS_SEVERE: return "severe";
            case PowerManager.THERMAL_STATUS_CRITICAL: return "critical";
            case PowerManager.THERMAL_STATUS_EMERGENCY: return "emergency";
            case PowerManager.THERMAL_STATUS_SHUTDOWN: return "shutdown";
            default: return "unknown (" + status + ")";
        }
    }
}
