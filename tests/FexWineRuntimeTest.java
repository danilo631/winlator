import com.winlator.optimization.FexWineRuntime;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FexWineRuntimeTest {
    private static void expect(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }

    private static byte[] binary(String interpreter, int machine) {
        byte[] elf = new byte[256];
        elf[0] = 0x7f; elf[1] = 'E'; elf[2] = 'L'; elf[3] = 'F';
        elf[4] = 2; elf[5] = 1;
        ByteBuffer buffer = ByteBuffer.wrap(elf).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(18, (short) machine);
        buffer.putLong(32, 64);   // e_phoff
        buffer.putShort(54, (short) 56);
        buffer.putShort(56, (short) 1);
        buffer.putInt(64, 3); // PT_INTERP
        buffer.putLong(72, 128);
        byte[] interp = (interpreter+"\0").getBytes(StandardCharsets.UTF_8);
        buffer.putLong(96, interp.length);
        System.arraycopy(interp, 0, elf, 128, interp.length);
        return elf;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("winlator-fex-test");
        File wineDir = dir.toFile();
        Path binDir = Files.createDirectories(dir.resolve("bin"));
        Path dllDir = Files.createDirectories(dir.resolve("lib/wine/aarch64-windows"));
        File bin = binDir.resolve("wine").toFile();

        Files.write(bin.toPath(), binary("/lib/ld-linux-aarch64.so.1", 183));
        expect(!FexWineRuntime.isNativeAndroidArm64(bin), "glibc must be refused");
        Files.write(bin.toPath(), binary("/system/bin/linker64", 62));
        expect(!FexWineRuntime.isNativeAndroidArm64(bin), "x86_64 must be refused");
        Files.write(bin.toPath(), binary("/system/bin/linker64", 183));
        expect(FexWineRuntime.isNativeAndroidArm64(bin), "Android AArch64 accepted");
        expect(FexWineRuntime.check(wineDir) != null, "no DLLs cannot be ready");

        for (String dll : new String[]{"libarm64ecfex.dll", "libwow64fex.dll"}) {
            byte[] pe = new byte[320];
            pe[0] = 'M'; pe[1] = 'Z';
            Files.write(dllDir.resolve(dll), pe);
        }
        expect(FexWineRuntime.ready(wineDir), "valid Bionic Wine + FEX DLLs");
        expect(FexWineRuntime.nativeWineCommand("wine explorer /desktop=shell,1280x720", wineDir)
            .equals(bin.getAbsolutePath()+" explorer /desktop=shell,1280x720"),
            "native command must bypass Box64");
        expect(FexWineRuntime.nativeWineCommand(bin.getAbsolutePath()+" --version", wineDir)
            .equals(bin.getAbsolutePath()+" --version"), "version probe");

        boolean blocked = false;
        try {
            FexWineRuntime.nativeWineCommand("sh -c something", wineDir);
        } catch (IllegalArgumentException expected) { blocked = true; }
        expect(blocked, "arbitrary native launch blocked");

        Files.delete(dllDir.resolve("libwow64fex.dll"));
        expect(!FexWineRuntime.ready(wineDir), "missing WoW64 DLL blocked");
        Files.write(bin.toPath(), binary("/apex/com.android.runtime/bin/linker64", 183));
        expect(FexWineRuntime.isNativeAndroidArm64(bin), "Android APEX linker accepted");
        System.out.println("FexWineRuntimeTest: PASS");
    }
}
