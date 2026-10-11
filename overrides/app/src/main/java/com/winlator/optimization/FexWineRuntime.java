package com.winlator.optimization;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/**
 * Preflight for Wine ARM64EC with FEXCore DLLs.
 *
 * A conventional Linux FEXLoader is NOT an Android CPU backend. For FEXCore
 * we must launch a native Android AArch64 Wine binary with PE translator DLLs.
 * This class does not download or trust unknown binaries and never switches a
 * normal x86_64 Wine build to the native launcher.
 */
public final class FexWineRuntime {
    public static final String BACKEND_BOX64 = "box64";
    public static final String BACKEND_FEXCORE = "fexcore";
    public static final String ENV_BACKEND = "WINLATOR_CPU_BACKEND";
    private static final int ELF_HEADER_SIZE = 64;

    private FexWineRuntime() {}

    /** Minimal verified layout: a native Wine binary and WoW64 / ARM64EC modules. */
    public static String check(File wineDir) {
        if (wineDir == null || !wineDir.isDirectory())
            return "Wine directory is missing";
        File wine = new File(wineDir, "bin/wine");
        if (!isNativeAndroidArm64(wine))
            return "Wine must be Android/Bionic ARM64 ELF64, not glibc or x86_64 Wine";
        File pe = new File(wineDir, "lib/wine/aarch64-windows");
        File arm64ec = new File(pe, "libarm64ecfex.dll");
        File wow64 = new File(pe, "libwow64fex.dll");
        if (!isPE(arm64ec) || !isPE(wow64))
            return "Missing FEXCore libarm64ecfex.dll or libwow64fex.dll";
        return null;
    }

    public static boolean ready(File wineDir) {
        return check(wineDir) == null;
    }

    /** Distinguishes Android Bionic dynamic executables from Linux/glibc. */
    public static boolean isNativeAndroidArm64(File path) {
        if (path == null || !path.isFile()) return false;
        try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
            if (file.length() < ELF_HEADER_SIZE) return false;
            byte[] header = new byte[ELF_HEADER_SIZE];
            file.readFully(header);
            // ELF64, little endian, AArch64 (e_machine 0xB7)
            if (header[0] != 0x7f || header[1] != 'E' || header[2] != 'L'
                || header[3] != 'F' || header[4] != 2 || header[5] != 1
                || (header[18] & 0xff) != 183 || (header[19] & 0xff) != 0)
                return false;
            long phoff = le64(header, 32);
            int entrySize = le16(header, 54);
            int count = le16(header, 56);
            if (phoff < ELF_HEADER_SIZE || entrySize < 56 || entrySize > 512
                || count < 1 || count > 1024 || phoff > file.length()
                || count * (long) entrySize > file.length() - phoff)
                return false;
            for (int i = 0; i < count; i++) {
                file.seek(phoff + (long) i * entrySize);
                byte[] ph = new byte[56];
                file.readFully(ph);
                if (le32(ph, 0) != 3) continue; // PT_INTERP
                long off = le64(ph, 8);
                long size = le64(ph, 32);
                if (size < 3 || size > 512 || off < 0 || off > file.length()
                    || size > file.length() - off) return false;
                byte[] interpreter = new byte[(int) size];
                file.seek(off);
                file.readFully(interpreter);
                String interp = new String(interpreter, StandardCharsets.UTF_8);
                // Never execute a glibc interpreter via the Android loader.
                return interp.startsWith("/system/bin/linker64")
                    || interp.startsWith("/apex/com.android.runtime/bin/linker64");
            }
            // Static FEX binaries are not accepted without an ABI contract.
            return false;
        }
        catch (IOException | RuntimeException error) {
            return false;
        }
    }

    public static boolean isPE(File path) {
        if (path == null || !path.isFile()) return false;
        try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
            return file.length() > 256 && file.readUnsignedByte() == 'M'
                && file.readUnsignedByte() == 'Z';
        }
        catch (IOException error) {
            return false;
        }
    }

    /** Native launcher only handles Wine; arbitrary ELF guests still use Box64. */
    public static String nativeWineCommand(String guestExecutable, File wineDir) {
        if (guestExecutable == null) throw new IllegalArgumentException("Missing Wine command");
        String bin = new File(wineDir, "bin/wine").getAbsolutePath();
        if (guestExecutable.equals("wine")) return bin;
        if (guestExecutable.startsWith("wine ")) return bin + guestExecutable.substring(4);
        String absolute = guestExecutable;
        if (absolute.equals(bin) || absolute.startsWith(bin + " ")) return absolute;
        String wine64 = new File(wineDir, "bin/wine64").getAbsolutePath();
        if (absolute.equals(wine64) || absolute.startsWith(wine64 + " ")) return absolute;
        throw new IllegalArgumentException("FEXCore requires a Wine executable");
    }

    private static int le16(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off+1] & 0xff) << 8);
    }

    private static long le32(byte[] b, int off) {
        return (b[off] & 0xffL) | ((b[off+1] & 0xffL) << 8)
            | ((b[off+2] & 0xffL) << 16) | ((b[off+3] & 0xffL) << 24);
    }

    private static long le64(byte[] b, int off) {
        return le32(b, off) | (le32(b, off + 4) << 32);
    }
}
