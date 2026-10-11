import com.winlator.optimization.ProfileRules;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProfileRulesTest {
    private static void expect(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void profiles() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("MY_GAME_OPTION", "keep-this");
        vars.put("TU_DEBUG", "noconform");
        vars.put("ZINK_DEBUG", "compact");

        expect("CONSERVATIVE".equals(ProfileRules.apply(ProfileRules.MALI_SAFE, vars)),
            "Mali Box64 fallback");
        expect(!vars.containsKey("TU_DEBUG") && !vars.containsKey("ZINK_DEBUG"),
            "Mali must clear unsupported driver flags");
        expect("keep-this".equals(vars.get("MY_GAME_OPTION")), "unrelated overrides preserved");

        expect("PERFORMANCE".equals(ProfileRules.apply(ProfileRules.PERFORMANCE, vars)),
            "performance preset");
        expect("true".equals(vars.get("mesa_glthread")), "performance GL threading");
        expect("512MB".equals(vars.get("MESA_SHADER_CACHE_MAX_SIZE")), "cache upper bound");

        expect("STABILITY".equals(ProfileRules.apply(ProfileRules.COMPATIBILITY, vars)),
            "compatibility preset");
        expect("0".equals(vars.get("WINEESYNC")), "compatibility Esync disabled");

        String oldPreset = ProfileRules.apply(ProfileRules.DXVK_DIAGNOSTICS, vars);
        expect(oldPreset == null, "diagnostic must not change CPU preset");
        expect("fps,frametimes".equals(vars.get("DXVK_HUD")), "diagnostic HUD");
        ProfileRules.apply(ProfileRules.DXVK_HUD_OFF, vars);
        expect(!vars.containsKey("DXVK_HUD"), "diagnostic rollback");

        ProfileRules.apply(ProfileRules.BALANCED, vars);
        expect("INTERMEDIATE".equals(ProfileRules.apply(ProfileRules.BALANCED, vars)),
            "profiles idempotent");
        expect("keep-this".equals(vars.get("MY_GAME_OPTION")), "unknown options unchanged");
    }

    private static void defaults() {
        String legacy = "TU_DEBUG=noconform WINEESYNC=1";
        expect(ProfileRules.newContainerEnv(legacy, true).equals(legacy),
            "Adreno defaults must remain unchanged");
        String generic = ProfileRules.newContainerEnv(legacy, false);
        expect(!generic.contains("TU_DEBUG") && generic.contains("MESA_SHADER_CACHE_MAX_SIZE=256MB"),
            "non-Adreno defaults should avoid Turnip variables");

        boolean thrown = false;
        try {
            ProfileRules.apply(999, new LinkedHashMap<>());
        } catch (IllegalArgumentException expected) {
            thrown = true;
        }
        expect(thrown, "unknown profiles rejected");
    }

    public static void main(String[] args) {
        profiles();
        defaults();
        System.out.println("ProfileRulesTest: PASS");
    }
}
