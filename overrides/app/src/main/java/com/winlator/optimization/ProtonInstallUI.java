package com.winlator.optimization;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.winlator.MainActivity;
import com.winlator.SettingsFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal release browser + verified download -> original Winlator Wine installer.
 * User explicitly confirms download/installation before networking the binary.
 */
public final class ProtonInstallUI {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private ProtonInstallUI() {}

    public static void attach(Fragment host, View root) {
        root.findViewById(com.winlator.R.id.BTAutoInstallProton).setOnClickListener(v -> search(host));
    }

    private static void search(Fragment host) {
        if (!RUNNING.compareAndSet(false, true)) {
            message(host, "Operação em andamento", "Conclua o download atual antes de iniciar outro.");
            return;
        }
        ProgressDialog indicator = new ProgressDialog(host.requireContext());
        indicator.setMessage("Buscando versões no GitHub…");
        indicator.setIndeterminate(true);
        indicator.setCancelable(false);
        indicator.show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ArrayList<ProtonReleaseClient.Asset> results = ProtonReleaseClient.catalog();
                MAIN.post(() -> {
                    RUNNING.set(false);
                    indicator.dismiss();
                    if (!host.isAdded()) return;
                    String[] labels = new String[results.size()];
                    for (int i = 0; i < results.size(); i++) labels[i] = results.get(i).toString();
                    new AlertDialog.Builder(host.requireContext())
                        .setTitle("Wine e Proton para Box64")
                        .setItems(labels, (dialog, index) -> confirm(host, results.get(index)))
                        .setNegativeButton(android.R.string.cancel, null).show();
                });
            }
            catch (Exception error) {
                MAIN.post(() -> {
                    RUNNING.set(false);
                    indicator.dismiss();
                    message(host, "Falha ao consultar GitHub",
                        error.getMessage() == null ? "Verifique sua conexão." : error.getMessage());
                });
            }
        });
    }

    private static void confirm(Fragment host, ProtonReleaseClient.Asset asset) {
        if (!host.isAdded()) return;
        new AlertDialog.Builder(host.requireContext())
            .setTitle("Baixar e instalar " + asset.title)
            .setMessage("Origem: github.com/Kron4ek/Wine-Builds\nArquivo: " + asset.filename
                + "\nTamanho: " + (asset.bytes / (1024 * 1024)) + " MB"
                + "\nSHA-256 publicado: " + asset.sha256.substring(0, 16) + "…"
                + "\n\nO pacote será verificado, extraído e encaminhado "
                + "ao instalador Wine do Winlator. A inicialização do prefixo "
                + "pode falhar caso faltem bibliotecas glibc. "
                + "Instale apenas versões de fontes em que você confie.")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Baixar e instalar", (d, which) -> download(host, asset))
            .show();
    }

    private static void download(Fragment host, ProtonReleaseClient.Asset asset) {
        if (!RUNNING.compareAndSet(false, true)) {
            message(host, "Operação em andamento", "Há outro download em curso.");
            return;
        }
        AtomicBoolean cancelled = new AtomicBoolean(false);
        ProgressDialog dialog = new ProgressDialog(host.requireContext());
        dialog.setTitle("Proton / Wine");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(100);
        dialog.setCancelable(false);
        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancelar",
            (d, which) -> cancelled.set(true));
        dialog.show();
        android.content.Context context = host.requireContext().getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File archive = ProtonReleaseClient.download(context, asset, cancelled,
                    percent -> MAIN.post(() -> {
                        if (host.isAdded() && !cancelled.get()) dialog.setProgress(percent);
                    }));
                MAIN.post(() -> {
                    dialog.dismiss();
                    RUNNING.set(false);
                    if (!host.isAdded() || cancelled.get()) {
                        archive.delete();
                        return;
                    }
                    SettingsFragment settings = new SettingsFragment();
                    Bundle args = new Bundle();
                    args.putString("winlator_verified_wine_file", archive.getAbsolutePath());
                    settings.setArguments(args);
                    ((MainActivity)host.requireActivity()).showFragment(settings);
                });
            }
            catch (Exception error) {
                MAIN.post(() -> {
                    dialog.dismiss();
                    RUNNING.set(false);
                    if (!cancelled.get()) message(host, "Instalação não iniciada",
                        error.getMessage() == null ? "Não foi possível baixar o arquivo." : error.getMessage());
                });
            }
        });
    }

    private static void message(Fragment host, String title, String details) {
        if (!host.isAdded()) return;
        new AlertDialog.Builder(host.requireContext())
            .setTitle(title).setMessage(details)
            .setPositiveButton(android.R.string.ok, null).show();
    }
}
