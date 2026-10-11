package com.winlator.optimization;

import android.content.Context;
import android.os.Build;

import com.winlator.core.WineInfo;
import com.winlator.core.WineInstaller;
import com.winlator.xenvironment.RootFS;

import java.io.File;
import java.util.ArrayList;

/** Local-only runtime diagnostics; does not require root, network or telemetry. */
public final class RuntimeDiagnostics {
    private RuntimeDiagnostics() {}

    public static String collect(Context context) {
        RootFS rootFS = RootFS.find(context);
        File root = rootFS.getRootDir();
        StringBuilder report = new StringBuilder("Winlator Enhanced — Diagnóstico\n");
        report.append("Android: ").append(Build.VERSION.RELEASE)
            .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        report.append("Arquiteturas: ").append(String.join(", ", Build.SUPPORTED_ABIS)).append("\n");
        report.append("RootFS: ").append(rootFS.isValid() ? "pronto" : "não instalado").append("\n");
        report.append("Box64: ").append(new File(root, "usr/local/bin/box64").isFile()
            ? "binário extraído" : "pode ser extraído ao iniciar").append("\n");
        ArrayList<WineInfo> wines = WineInstaller.getInstalledWineInfos(context);
        report.append("Wine detectado: ").append(wines.size()).append(" versão(ões)\n");
        for (WineInfo wine : wines) {
            report.append(" • ").append(wine.toString());
            if (wine.path == null) report.append(" — integrado\n");
            else {
                File bin = new File(wine.path, "bin/wine");
                if (!bin.isFile()) report.append(" — bin/wine ausente\n");
                else if (FexWineRuntime.ready(new File(wine.path)))
                    report.append(" — ARM64EC pré-verificado (execução não testada)\n");
                else if (FexWineRuntime.isNativeAndroidArm64(bin))
                    report.append(" — ARM64 Android sem FEXCore validado\n");
                else report.append(" — execução ainda não testada\n");
            }
        }
        report.append("\nEste relatório não inclui arquivos dos jogos, senhas nem informações da conta.\n");
        return report.toString();
    }
}
