import com.winlator.optimization.ProtonSyncRules;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ProtonSyncRulesTest {
    private static void yes(boolean c, String desc) { if (!c) throw new AssertionError(desc); }
    public static void main(String[] args) {
        Map<String,String> env = new LinkedHashMap<>();
        env.put("CUSTOM_TEST", "unchanged");
        ProtonSyncRules.apply(ProtonSyncRules.USERSPACE_NTSYNC, env);
        yes("1".equals(env.get("PROTON_NO_KERNEL_NTSYNC")), "user space");
        ProtonSyncRules.apply(ProtonSyncRules.DISABLE_NTSYNC, env);
        yes(!env.containsKey("PROTON_NO_KERNEL_NTSYNC"), "remove kernel override");
        yes("1".equals(env.get("PROTON_NO_NTSYNC")), "turn off NTSYNC");
        ProtonSyncRules.apply(ProtonSyncRules.ESYNC, env);
        yes("1".equals(env.get("WINEESYNC")), "Esync set");
        ProtonSyncRules.apply(ProtonSyncRules.AUTOMATIC, env);
        yes(!env.containsKey("PROTON_NO_NTSYNC"), "auto reset");
        yes("unchanged".equals(env.get("CUSTOM_TEST")), "preserve unrelated");
        boolean bad = false;
        try { ProtonSyncRules.apply(999, env); } catch (IllegalArgumentException ex) { bad = true; }
        yes(bad, "invalid presets rejected");
        System.out.println("ProtonSyncRulesTest: PASS");
    }
}
