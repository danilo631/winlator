package com.winlator.optimization;

import java.util.ArrayDeque;

/**
 * Standalone archive checks, testable without Android.
 * Extraction must still recheck each member before writing (avoid TOCTOU).
 */
public final class WineImportPolicy {
    public enum Format { XZ, ZSTD, UNKNOWN }
    private WineImportPolicy() {}

    public static Format compression(byte[] header) {
        if (header == null || header.length < 6) return Format.UNKNOWN;
        if ((header[0] & 255) == 0xfd && header[1] == 0x37 && header[2] == 0x7a
                && header[3] == 0x58 && header[4] == 0x5a && header[5] == 0x00)
            return Format.XZ;
        if ((header[0] & 255) == 0x28 && (header[1] & 255) == 0xb5
                && (header[2] & 255) == 0x2f && (header[3] & 255) == 0xfd)
            return Format.ZSTD;
        return Format.UNKNOWN;
    }

    /** Reject all path escapes, regardless of the spelling of a member name. */
    public static boolean safeMember(String name) {
        return canonical(name) != null;
    }

    /** Relative links may not climb outside the archive root. */
    public static boolean safeLink(String member, String target, boolean hardLink) {
        String name = canonical(member);
        if (name == null || target == null || target.isEmpty()
                || target.startsWith("/") || target.indexOf('\\') >= 0 || target.indexOf('\0') >= 0)
            return false;
        String parent = hardLink ? "" : name.substring(0, Math.max(0, name.lastIndexOf('/') + 1));
        return canonical(parent + target) != null;
    }

    public static String canonical(String path) {
        if (path == null || path.isEmpty() || path.startsWith("/")
                || path.indexOf('\\') >= 0 || path.indexOf('\0') >= 0)
            return null;
        ArrayDeque<String> parts = new ArrayDeque<>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                if (parts.isEmpty()) return null;
                parts.removeLast();
            }
            else if (part.indexOf(':') >= 0 || part.length() > 240) return null;
            else parts.addLast(part);
        }
        if (parts.isEmpty()) return null;
        return String.join("/", parts);
    }

    /** Dynamic loader path, extracted from ELF64 little-endian PT_INTERP if present. */
    public static String elfInterpreter(byte[] header) {
        if (header == null || header.length < 64 || header[0] != 0x7f ||
                header[1] != 'E' || header[2] != 'L' || header[3] != 'F' ||
                header[4] != 2 || header[5] != 1) return "";
        long offset = leLong(header, 32);
        int size = leShort(header, 54);
        int count = leShort(header, 56);
        if (offset < 64 || size < 56 || count < 1 || count > 512 ||
                offset + (long) size * count > header.length) return "";
        for (int i = 0; i < count; i++) {
            int ph = (int) (offset + (long) size * i);
            if (leInt(header, ph) == 3) {
                long position = leLong(header, ph + 8);
                long length = leLong(header, ph + 32);
                if (length < 1 || length > 512 || position < 0 ||
                        position + length > header.length) return "";
                int end = (int)(position + length);
                for (int j=(int)position; j<end; j++) {
                    if (header[j] == 0) { end = j; break; }
                }
                return new String(header, (int)position, end-(int)position,
                    java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static int leShort(byte[] a, int p) {
        return (a[p]&255)|((a[p+1]&255)<<8);
    }
    private static int leInt(byte[] a, int p) {
        return leShort(a,p)|(leShort(a,p+2)<<16);
    }
    private static long leLong(byte[] a, int p) {
        return ((long)leInt(a,p)&0xffffffffL) |
                (((long)leInt(a,p+4)&0xffffffffL)<<32);
    }

    /** Read only the ELF header. Linux vs Bionic requires inspecting PT_INTERP later. */
    public static String elfArchitecture(byte[] prefix) {
        if (prefix == null || prefix.length < 20 || prefix[0] != 0x7f ||
                prefix[1] != 'E' || prefix[2] != 'L' || prefix[3] != 'F')
            return "não identificado";
        int machine = (prefix[18] & 255) | ((prefix[19] & 255) << 8);
        if (machine == 62) return "x86_64";
        if (machine == 183) return "aarch64";
        return "ELF arquitetura " + machine;
    }
}
