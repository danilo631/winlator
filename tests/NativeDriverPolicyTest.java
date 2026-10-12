import com.winlator.optimization.NativeDriverPolicy;

public final class NativeDriverPolicyTest {
    private static void check(boolean v) { if (!v) throw new AssertionError("GPU policy regression"); }

    public static void main(String[] args) {
        check(NativeDriverPolicy.classify("Mali-G78 MP14") == NativeDriverPolicy.Family.MALI);
        check(NativeDriverPolicy.classify("ARM Immortalis-G715") == NativeDriverPolicy.Family.MALI);
        check(NativeDriverPolicy.classify("Samsung Xclipse 920") == NativeDriverPolicy.Family.XCLIPSE);
        check(NativeDriverPolicy.classify("PowerVR Rogue") == NativeDriverPolicy.Family.POWERVR);
        check(NativeDriverPolicy.classify("Adreno (TM) 740") == NativeDriverPolicy.Family.ADRENO);
        check(NativeDriverPolicy.classify("") == NativeDriverPolicy.Family.OTHER);
        check(NativeDriverPolicy.shouldPreferSystem("Mali-G57"));
        check(NativeDriverPolicy.needsVortek("Mali-G57", "turnip"));
        check(!NativeDriverPolicy.needsVortek("Mali-G57", "vortek"));
        check(!NativeDriverPolicy.needsVortek("Adreno 740", "turnip"));
        check(!NativeDriverPolicy.shouldPreferSystem("software renderer"));
        check(NativeDriverPolicy.recommendation("Mali-G78").contains("System"));
        System.out.println("NativeDriverPolicyTest: PASS");
    }
}
