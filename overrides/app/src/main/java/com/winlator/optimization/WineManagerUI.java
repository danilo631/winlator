package com.winlator.optimization;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.FileUtils;
import com.winlator.core.WineInfo;
import com.winlator.core.WineInstaller;
import com.winlator.xenvironment.RootFS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;

/** Actual installed Wine list and safe per-version removal; no UI debug flag. */
public final class WineManagerUI {
    private WineManagerUI() {}

    public static void render(Context context, LinearLayout holder, TextView status) {
        holder.removeAllViews();
        ArrayList<WineInfo> items = WineInstaller.getInstalledWineInfos(context);
        status.setText("Versões detectadas: " + items.size() + "  •  Toque em uma versão para gerenciar");
        for (WineInfo wine : items) {
            if (wine == WineInfo.MAIN_WINE_INFO) {
                Button builtIn = button(context, "Wine " + wine.version + "  •  Integrado");
                builtIn.setOnClickListener(v -> message(context, "Wine integrado",
                    "Essa versão faz parte dos arquivos de sistema e não pode ser removida. "
                    + "Crie um novo contêiner para selecionar outra versão."));
                holder.addView(builtIn);
                continue;
            }

            File dir = wine.path != null ? new File(wine.path) : null;
            if (dir == null || !dir.isDirectory() || !new File(dir, "bin/wine").isFile()) continue;

            Button row = button(context, wine.toString() + "    ·    Gerenciar");
            row.setOnClickListener(v -> showActions(context, wine, holder, status));
            holder.addView(row);
        }
        if (holder.getChildCount() == 0)
            status.setText("Nenhuma versão Wine reconhecida. Verifique a instalação do sistema.");
    }

    private static Button button(Context context, String title) {
        Button b = new Button(context);
        b.setText(title);
        b.setAllCaps(false);
        return b;
    }

    private static void showActions(Context context, WineInfo wine,
                                    LinearLayout holder, TextView status) {
        ArrayList<Container> containers = new ContainerManager(context).getContainers();
        int uses = 0;
        for (Container c : containers)
            if (wine.identifier().equals(c.getWineVersion())) uses++;
        int usage = uses;
        String details = "ID: " + wine.identifier()
            + "\nTipo: " + (FexWineRuntime.ready(new File(wine.path))
                ? "ARM64EC/FEXCore (pré-verificação)"
                : FexWineRuntime.isNativeAndroidArm64(new File(wine.path, "bin/wine"))
                    ? "ARM64 Android — bibliotecas ainda não validadas" : "x86_64 / a verificar")
            + "\nContêineres usando: " + usage
            + "\n\nA remoção é bloqueada se algum contêiner utiliza esta versão.";
        new AlertDialog.Builder(context)
            .setTitle(wine.toString()).setMessage(details)
            .setNegativeButton("Voltar", null)
            .setPositiveButton("Remover…", (d, which) ->
                requestRemoval(context, wine, usage, holder, status))
            .show();
    }

    private static void requestRemoval(Context context, WineInfo wine, int usage,
                                       LinearLayout holder, TextView status) {
        if (usage > 0) {
            message(context, "Wine em uso", "Esta versão é usada por " + usage
                + " contêiner(es). Não será removida.");
            return;
        }
        File source = new File(wine.path);
        File base = RootFS.find(context).getInstalledWineDir();
        File prefix = new File(base, "container-pattern-" + wine.fullVersion() + ".tzst");
        try {
            if (!source.getCanonicalFile().getParentFile().equals(base.getCanonicalFile())
                || !source.getName().startsWith("wine-") || !prefix.isFile()) {
                message(context, "Remoção bloqueada", "Instalação Wine inválida ou backup do prefixo ausente.");
                return;
            }
        }
        catch (IOException e) {
            message(context, "Remoção bloqueada", "Caminho Wine não pôde ser verificado.");
            return;
        }
        new AlertDialog.Builder(context)
            .setTitle("Excluir " + wine.toString() + "?")
            .setMessage("A pasta Wine instalada e seu modelo de prefixo serão apagados. "
                + "Essa operação não pode ser desfeita.")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Excluir versão", (d, w) -> {
                status.setText("Removendo versão Wine…");
                Executors.newSingleThreadExecutor().execute(() -> {
                    // Recheck right before deleting to avoid removing in-use Wine.
                    boolean used = false;
                    for (Container c : new ContainerManager(context).getContainers())
                        if (wine.identifier().equals(c.getWineVersion())) { used = true; break; }
                    final boolean blocked = used;
                    boolean success = false;
                    if (!blocked) {
                        FileUtils.delete(source);
                        FileUtils.delete(prefix);
                        success = !source.exists() && !prefix.exists();
                    }
                    final boolean result = success;
                    holder.post(() -> {
                        if (blocked) message(context, "Remoção cancelada",
                            "Um contêiner passou a usar esta versão.");
                        else if (!result) message(context, "Falha ao remover",
                            "Confira as permissões dos arquivos Wine.");
                        render(context, holder, status);
                    });
                });
            }).show();
    }

    private static void message(Context ctx, String title, String message) {
        new AlertDialog.Builder(ctx).setTitle(title).setMessage(message)
            .setPositiveButton(android.R.string.ok, null).show();
    }
}
