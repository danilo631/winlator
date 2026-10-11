package com.winlator.optimization;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

/** Clear official download route, SAF-import hook and ABI guidance. */
public final class ProtonSourcesUI {
    private static final String PROTON_RELEASES = "https://github.com/GameNative/proton-wine/releases";
    private ProtonSourcesUI() {}

    public static void attach(Context context, View root) {
        LinearLayout wineSection = root.findViewById(com.winlator.R.id.LLWineInstallation);
        if (wineSection == null) return;
        Button release = new Button(context);
        release.setAllCaps(false);
        release.setText("Proton Wine 11 • versões para Android");
        wineSection.addView(release);
        release.setOnClickListener(v -> new AlertDialog.Builder(context)
            .setTitle("Baixar Proton Wine oficial")
            .setMessage("Os releases GameNative/proton-wine possuem builds x86_64 e ARM64EC. "
                + "Para importar pelo Winlator 11.2, escolha o arquivo Wine Type .wcp.xz "
                + "(tar.xz) — não o Proton Type .wcp (tar.zst). "
                + "x86_64 funciona via Box64 se o Wine tiver bibliotecas compatíveis; "
                + "ARM64EC exige Wine Android/Bionic + FEXCore e bibliotecas correspondentes. "
                + "O instalador do Winlator possui suporte a arquivos tar.xz; "
                + "a execução e compatibilidade precisam ser testadas.")
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton("Escolher arquivo", (d, w) -> {
                View pick = root.findViewById(com.winlator.R.id.BTInstallWine);
                if (pick != null) pick.performClick();
            })
            .setPositiveButton("Ver releases", (d, w) -> {
                Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(PROTON_RELEASES));
                try { context.startActivity(browser); }
                catch (Exception ignored) {
                    new AlertDialog.Builder(context).setMessage(PROTON_RELEASES)
                        .setPositiveButton(android.R.string.ok, null).show();
                }
            }).show());
    }
}
