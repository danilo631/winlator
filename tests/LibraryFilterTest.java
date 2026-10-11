import com.winlator.optimization.LibraryFilter;

public final class LibraryFilterTest {
    private static void check(boolean ok) { if (!ok) throw new AssertionError("Search mismatch"); }
    public static void main(String[] args) {
        check(LibraryFilter.matches("", "Anything"));
        check(LibraryFilter.matches("ação", "Acao Zero"));
        check(LibraryFilter.matches("MALI", "turnip", "mali-g68", "wine"));
        check(LibraryFilter.matches("dxvk", "GPU", "Wine", "DXVK"));
        check(!LibraryFilter.matches("adreno", "mali", "gladio"));
        check(!LibraryFilter.matches("fex", (String[]) null));
        check(LibraryFilter.matches(" fex ", "FEXCore"));
        System.out.println("LibraryFilterTest: PASS");
    }
}
