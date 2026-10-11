package com.winlator.optimization;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.winlator.ContainerDetailFragment;
import com.winlator.MainActivity;
import com.winlator.R;
import com.winlator.SettingsFragment;
import com.winlator.core.WineInfo;
import com.winlator.core.WineInstaller;
import com.winlator.xenvironment.RootFS;

import java.io.File;
import java.util.ArrayList;

/**
 * Dedicated entry point for Wine runtime administration.
 *
 * Only official upstream URLs; never silently downloads, installs, or
 * executes third-party binaries. Runtime changes are per new container.
 */
public final class RuntimeCenterFragment extends Fragment {
    private static final String RELEASES = "https://github.com/GameNative/proton-wine/releases";
    private static final String RELEASE_BASE = RELEASES
        + "/download/proton-11.0-2-20261010/";
    private static final String X86_WINE = "proton-wine-11.0-2-x86_64.wcp.xz";
    private static final String ARM_WINE = "proton-wine-11.0-2-arm64ec.wcp.xz";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.runtime_center_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        if (((MainActivity)requireActivity()).getSupportActionBar() != null)
            ((MainActivity)requireActivity()).getSupportActionBar().setTitle("Wine & Proton");

        root.findViewById(R.id.BTOpenProtonReleases).setOnClickListener(v -> open(RELEASES));
        root.findViewById(R.id.BTDownloadProtonBox).setOnClickListener(
            v -> confirmDownload("Proton Wine x86_64 (Bionic)", X86_WINE,
                "Este pacote é para forks compatíveis com Android/Bionic (CMOD/Ludashi). O Winlator padrão usa Wine Linux/glibc e pode recusar a importação."));
        root.findViewById(R.id.BTDownloadProtonFex).setOnClickListener(
            v -> confirmDownload("Proton Wine ARM64EC — FEXCore", ARM_WINE,
                "Exige FEXCore compatível e bibliotecas Unix Android. O Wine x86_64 não substitui o ARM64EC."));
        root.findViewById(R.id.BTImportWine).setOnClickListener(v -> openSettings(true));
        root.findViewById(R.id.BTAllWineSettings).setOnClickListener(v -> openSettings(false));
        root.findViewById(R.id.BTCreateWineContainer).setOnClickListener(v -> {
            if (!RootFS.find(requireContext()).isValid()) {
                message("Sistema ainda não instalado",
                    "Abra o Winlator, conclua a instalação dos arquivos de sistema e tente novamente.");
            }
            else ((MainActivity)requireActivity()).showFragment(new ContainerDetailFragment());
        });
        root.findViewById(R.id.BTDiagnostics).setOnClickListener(v -> {
            final String report = RuntimeDiagnostics.collect(requireContext());
            new AlertDialog.Builder(requireContext())
                .setTitle("Diagnóstico de execução")
                .setMessage(report)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Compartilhar", (d, which) -> {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, report);
                    startActivity(Intent.createChooser(share, "Compartilhar diagnóstico"));
                })
                .show();
        });
        root.findViewById(R.id.BTRuntimeHelp).setOnClickListener(v ->
            message("Como selecionar Wine e tradutor",
                "1. Baixe um pacote Wine Type .wcp.xz (TAR XZ) de uma fonte oficial.\n"
                + "2. Toque em Importar Wine. O seletor abre seus arquivos locais.\n"
                + "3. Depois de instalar, crie um NOVO contêiner e escolha a versão na opção Wine.\n"
                + "4. Em Variáveis de ambiente, escolha Box64 para Wine x86_64 ou FEXCore "
                + "somente para Wine Android ARM64EC preparado com suas DLLs.\n\n"
                + "Contêineres existentes mantêm sua versão original de Wine para proteger o prefixo. "
                + "O Wine ARM64EC/FEXCore permanece experimental."));
        updateInstalledVersions(root);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) updateInstalledVersions(getView());
    }

    private void updateInstalledVersions(View root) {
        TextView status = root.findViewById(R.id.TVRuntimeRootfs);
        TextView list = root.findViewById(R.id.TVRuntimeInstalledWine);
        boolean ready = RootFS.find(requireContext()).isValid();
        status.setText(ready ? "Sistema Winlator instalado" :
            "Sistema não instalado: conclua a configuração inicial antes de importar Wine.");
        ArrayList<WineInfo> versions = WineInstaller.getInstalledWineInfos(requireContext());
        StringBuilder report = new StringBuilder();
        for (WineInfo info : versions) {
            if (report.length() > 0) report.append("\n");
            report.append("• ").append(info.toString());
            if (info.path != null) {
                File dir = new File(info.path);
                if (FexWineRuntime.ready(dir))
                    report.append("  —  ARM64EC / FEXCore (pré-verificação OK)");
                else if (FexWineRuntime.isNativeAndroidArm64(new File(dir, "bin/wine")))
                    report.append("  —  ARM64 nativo; faltam componentes FEXCore");
                else report.append("  —  Box64 ou ABI não verificada");
            }
            else report.append("  —  integrado");
        }
        list.setText(report.length() == 0 ? "Nenhuma versão instalada" : report.toString());
    }

    private void confirmDownload(String name, String file, String requirement) {
        new AlertDialog.Builder(requireContext())
            .setTitle(name)
            .setMessage("Será aberta a URL oficial de um release Wine Type (.wcp.xz), "
                + "não o pacote .wcp Zstandard.\n\n" + requirement
                + "\n\nDepois do download, volte aqui e toque em Importar Wine."
                + "\n\nO download não instala automaticamente o runtime.")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Abrir download", (dialog, which) -> open(RELEASE_BASE + file))
            .show();
    }

    private void openSettings(boolean importFile) {
        SettingsFragment settings = new SettingsFragment();
        Bundle args = new Bundle();
        args.putBoolean(importFile ? "winlator_open_wine_import" : "winlator_focus_wine", true);
        settings.setArguments(args);
        ((MainActivity)requireActivity()).showFragment(settings);
    }

    private void open(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE));
        }
        catch (ActivityNotFoundException error) {
            message("Link oficial", url);
        }
    }

    private void message(String title, String detail) {
        new AlertDialog.Builder(requireContext()).setTitle(title)
            .setMessage(detail).setPositiveButton(android.R.string.ok, null).show();
    }
}
