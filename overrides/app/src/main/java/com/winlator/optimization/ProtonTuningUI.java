package com.winlator.optimization;

import android.app.AlertDialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.winlator.core.EnvVars;
import com.winlator.widget.EnvVarsView;

import java.util.LinkedHashMap;
import java.util.Map;

/** Separate Proton knobs: transparent opt-in, not a fake Proton installation. */
public final class ProtonTuningUI {
    private ProtonTuningUI() {}

    public static void attach(Context context, LinearLayout tab, EnvVarsView envView) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText("Proton Wine: sincronização e compatibilidade");
        tab.addView(button, 3, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(v -> new AlertDialog.Builder(context)
            .setTitle("Proton / Esync / NTsync")
            .setItems(ProtonSyncRules.LABELS, (dialog, index) -> {
                EnvVars current = new EnvVars(envView.getEnvVars());
                Map<String, String> vars = new LinkedHashMap<>();
                for (String key : current) vars.put(key, current.get(key));
                ProtonSyncRules.apply(index, vars);
                EnvVars next = new EnvVars();
                for (Map.Entry<String, String> e : vars.entrySet())
                    next.put(e.getKey(), e.getValue());
                envView.setEnvVars(next);
                new AlertDialog.Builder(context)
                    .setTitle(ProtonSyncRules.LABELS[index])
                    .setMessage(ProtonSyncRules.DETAILS[index]
                        + "\n\nChoose a compatible Wine build. These flags do not install NTsync, Proton, or FEX. Save the container/shortcut after selecting.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show());
    }
}
