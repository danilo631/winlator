package com.winlator.optimization;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.View;

/** Explicit upstream open-source sources, grouped by Linux vs Android ABI. */
public final class ProtonSourcesUI {
    private ProtonSourcesUI() {}

    public static void attach(Context context, View root) {
        LinearLayout wine = root.findViewById(com.winlator.R.id.LLWineInstallation);
        if (wine == null) return;
        Button help = new Button(context);
        help.setAllCaps(false);
        help.setText("Fontes Wine e Proton");
        wine.addView(help);
        help.setOnClickListener(v -> new AlertDialog.Builder(context)
            .setTitle("Escolher pacote Wine")
            .setMessage("Winlator padrão: prefira Wine Linux x86_64 compatível com o rootfs glibc. "
                + "Kron4ek oferece Wine Staging e versões derivadas do Proton para Linux. "
                + "As builds GameNative usam Android/Bionic para outros ambientes, "
                + "e podem falhar neste Winlator mesmo em TAR XZ.")
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton("Proton Android", (dialog, which) ->
                open(context, "https://github.com/GameNative/proton-wine/releases"))
            .setPositiveButton("Wine Linux", (dialog, which) ->
                open(context, "https://github.com/Kron4ek/Wine-Builds/releases"))
            .show());
    }
    private static void open(Context context, String url) {
        try { context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) {
            new AlertDialog.Builder(context).setMessage(url)
                .setPositiveButton(android.R.string.ok, null).show();
        }
    }
}
