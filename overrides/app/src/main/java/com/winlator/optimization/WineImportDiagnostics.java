package com.winlator.optimization;

import android.content.Context;
import android.net.Uri;

import com.winlator.core.TarCompressorUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

/** Explains import failure rather than generic "Unable to install Wine". */
public final class WineImportDiagnostics {
    private static volatile String lastError = "";
    private WineImportDiagnostics() {}

    public static void clear() { lastError = ""; }
    public static String lastError() { return lastError; }
    public static void fail(String message) { lastError = message; }

    public static TarCompressorUtils.Type detectFormat(Context context, Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) { fail("Não foi possível abrir o arquivo selecionado."); return null; }
            byte[] bytes = new byte[6];
            int read = 0;
            while (read < bytes.length) {
                int n = in.read(bytes, read, bytes.length - read);
                if (n < 0) break;
                if (n == 0) break;
                read += n;
            }
            if (read != bytes.length) {
                fail("Arquivo vazio ou incompleto.");
                return null;
            }
            WineImportPolicy.Format type = WineImportPolicy.compression(bytes);
            if (type == WineImportPolicy.Format.XZ) return TarCompressorUtils.Type.XZ;
            if (type == WineImportPolicy.Format.ZSTD) return TarCompressorUtils.Type.ZSTD;
            fail("Formato desconhecido. Use Wine TAR XZ (.tar.xz/.wcp.xz) ou TAR Zstandard (.wcp).");
            return null;
        }
        catch (Exception error) {
            fail("Não foi possível acessar o arquivo: " + error.getClass().getSimpleName());
            return null;
        }
    }

    /** Prefix/wine architecture is not in itself proof of Wine compatibility. */
    public static String inspectExtracted(File wineDir) {
        if (wineDir == null || !wineDir.isDirectory()) return "Arquivo não extraído.";
        File base = wineDir;
        File bin = new File(base, "bin/wine");
        if (!bin.isFile()) {
            File[] children = base.listFiles();
            if (children != null && children.length == 1 && children[0].isDirectory()) {
                base = children[0];
                bin = new File(base, "bin/wine");
            }
        }
        if (!bin.isFile()) return "Pacote não contém bin/wine. Extraia uma distribuição Wine, não o Proton Steam launcher.";
        try (InputStream in = new FileInputStream(bin)) {
            byte[] header = new byte[32768];
            int bytes = in.read(header);
            if (bytes < 64) return "Executável bin/wine inválido ou truncado.";
            header = Arrays.copyOf(header, bytes);
            String cpu = WineImportPolicy.elfArchitecture(header);
            if (cpu.equals("não identificado"))
                return "bin/wine não é um executável ELF. Este instalador requer Wine Android/Linux pré-compilado.";
            String interp = WineImportPolicy.elfInterpreter(header);
            boolean bionic = interp.equals("/system/bin/linker64")
                || interp.equals("/apex/com.android.runtime/bin/linker64");
            if (bionic && cpu.equals("x86_64"))
                return "Este Proton Wine x86_64 foi compilado para Android/Bionic. "
                    + "O Winlator 11.2 usa Wine x86_64 Linux/glibc com Box64; apenas extrair "
                    + "o arquivo não adapta as bibliotecas. Use uma build glibc compatível "
                    + "ou um runtime Android especializado.";
            if (bionic && cpu.equals("aarch64") && !FexWineRuntime.ready(base))
                return "Wine ARM64EC Android detectado, mas faltam os módulos FEXCore "
                    + "compatíveis ou a instalação Wine não está preparada. "
                    + "Monte o pacote Proton/FEX e confira as dependências Unix.";
            if (cpu.equals("aarch64") && !bionic)
                return "Wine AArch64 Linux/glibc não pode ser executado pelo launcher nativo Bionic.";
            if (!cpu.equals("x86_64") && !bionic)
                return "Arquitetura Wine não reconhecida: " + cpu + ".";
            return null;  // can attempt, but an actual runtime probe may still fail
        }
        catch (Exception error) {
            return "Não foi possível ler bin/wine: " + error.getClass().getSimpleName();
        }
    }
}
