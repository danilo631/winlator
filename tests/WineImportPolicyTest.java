import com.winlator.optimization.WineImportPolicy;

import java.nio.charset.StandardCharsets;

public class WineImportPolicyTest {
    private static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }
    private static byte[] elf(String loader, int cpu) {
        byte[] a = new byte[512];
        a[0] = 0x7f; a[1] = 'E'; a[2] = 'L'; a[3] = 'F';
        a[4] = 2; a[5] = 1; a[18] = (byte)cpu; a[19] = (byte)(cpu >>> 8);
        a[32] = 64; // ELF header points to program header
        a[54] = 56; a[56] = 1; // one 56-byte program header
        a[64] = 3; // PT_INTERP
        a[72] = (byte)160; // p_offset
        a[96] = (byte)(loader.length() + 1); // p_filesz
        byte[] bytes = loader.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(bytes, 0, a, 160, bytes.length);
        return a;
    }
    public static void main(String[] args) {
        check(WineImportPolicy.compression(new byte[] {
            (byte)0xfd, 0x37, 0x7a, 0x58, 0x5a, 0x00}) == WineImportPolicy.Format.XZ, "xz");
        check(WineImportPolicy.compression(new byte[] {
            0x28, (byte)0xb5, 0x2f, (byte)0xfd, 0, 0}) == WineImportPolicy.Format.ZSTD, "zstd");
        check(WineImportPolicy.compression(new byte[6]) == WineImportPolicy.Format.UNKNOWN, "unknown");
        check(WineImportPolicy.safeMember("lib/wine/ntdll.so"), "normal entry");
        check(WineImportPolicy.safeMember("lib/../bin/wine"), "inside path normalization");
        check(!WineImportPolicy.safeMember("../escape"), "parent escape");
        check(!WineImportPolicy.safeMember("/etc/passwd"), "absolute escape");
        check(!WineImportPolicy.safeMember("C:\\windows"), "backslash");
        check(!WineImportPolicy.safeMember("C:/windows"), "windows device");
        check(!WineImportPolicy.safeLink("lib/wine/lib.so", "../other.so", false), "symlink resolution rejected");
        check(WineImportPolicy.safeLink("lib/wine/a.so", "b.so", false), "safe symlink");
        check(WineImportPolicy.safeLink("lib/wine/a.so", "../unix/b.so", false), "safe nested symlink");
        check(!WineImportPolicy.safeLink("lib/wine/a.so", "../../../etc/shadow", false), "symlink escape");
        check(!WineImportPolicy.safeLink("lib/wine/a.so", "/system/bin/sh", false), "absolute symlink");
        byte[] arm = elf("/system/bin/linker64", 183);
        check(WineImportPolicy.elfArchitecture(arm).equals("aarch64"), "arm");
        check(WineImportPolicy.elfInterpreter(arm).equals("/system/bin/linker64"), "bionic");
        byte[] x64 = elf("/lib64/ld-linux-x86-64.so.2", 62);
        check(WineImportPolicy.elfArchitecture(x64).equals("x86_64"), "x86");
        check(WineImportPolicy.elfInterpreter(x64).contains("ld-linux"), "glibc");
        System.out.println("WineImportPolicyTest: PASS");
    }
}
