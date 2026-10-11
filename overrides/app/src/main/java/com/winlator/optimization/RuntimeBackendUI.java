package com.winlator.optimization;

import android.app.AlertDialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.winlator.core.EnvVars;
import com.winlator.widget.EnvVarsView;

/**
 * Opt-in backend selector shared by the container and shortcut settings.
 * FEXCore is offered only as a guarded mode for a separate ARM64EC Wine.
 */
public final class RuntimeBackendUI {
    private RuntimeBackendUI() {}

    public static void attach(Context context, LinearLayout tab, EnvVarsView varsView) {
        if (tab == null || varsView == null) return;
        Button button = new Button(context);
        button.setText("CPU / Wine runtime: Box64 or FEXCore");
        button.setAllCaps(false);
        tab.addView(button, 1, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(view -> {
            EnvVars vars = new EnvVars(varsView.getEnvVars());
            int selected = FexWineRuntime.BACKEND_FEXCORE.equals(
                vars.get(FexWineRuntime.ENV_BACKEND)) ? 1 : 0;
            new AlertDialog.Builder(context)
                .setTitle("CPU translator for this game/container")
                .setSingleChoiceItems(new String[]{
                    "Box64 (default, bundled)",
                    "FEXCore ARM64EC (requires separate Wine + FEX DLLs)"
                }, selected, (dialog, which) -> {
                    EnvVars updated = new EnvVars(varsView.getEnvVars());
                    if (which == 1)
                        updated.put(FexWineRuntime.ENV_BACKEND, FexWineRuntime.BACKEND_FEXCORE);
                    else
                        updated.remove(FexWineRuntime.ENV_BACKEND);
                    varsView.setEnvVars(updated);
                    dialog.dismiss();
                    new AlertDialog.Builder(context)
                        .setTitle(which == 0 ? "Box64 selected" : "FEXCore selected (experimental)")
                        .setMessage(which == 0
                            ? "Default runtime restored. Review and Save."
                            : "This mode only works with an Android/Bionic ARM64EC Wine "
                              + "installation containing libarm64ecfex.dll and "
                              + "libwow64fex.dll in lib/wine/aarch64-windows. "
                              + "Ordinary Linux FEXLoader and Linux Proton archives "
                              + "are NOT compatible. The game will refuse to launch "
                              + "instead of silently switching back to Box64 if "
                              + "the required runtime is missing. Review and Save.")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        });
        Button protonHelp = new Button(context);
        protonHelp.setText("Proton Wine: installation guidance");
        protonHelp.setAllCaps(false);
        tab.addView(protonHelp, 2, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        protonHelp.setOnClickListener(view -> new AlertDialog.Builder(context)
            .setTitle("Proton compatibility")
            .setMessage("Winlator already supports DXVK and VKD3D. To use a Proton-derived "
                + "Wine build, import a compatible Wine64 tar.xz under Settings > Wine "
                + "and select that Wine version in the container. Linux Steam Proton "
                + "cannot be dropped into Android directly. For ARM64EC/FEXCore "
                + "you must use a specifically built Android/Bionic ARM64EC Wine "
                + "with matched FEXCore DLLs. Avoid mixing glibc and Bionic packages.")
            .setPositiveButton(android.R.string.ok, null)
            .show());
    }
}
