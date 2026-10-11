package com.winlator.optimization;

import android.app.AlertDialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.winlator.box64.Box64PresetManager;
import com.winlator.core.EnvVars;
import com.winlator.widget.EnvVarsView;

import java.util.LinkedHashMap;
import java.util.Map;

/** A one-tap game profile picker, inspired by launcher workflows, not GameHub code. */
public final class GameProfileUI {
    private GameProfileUI() {}

    public static void attach(Context context, LinearLayout envTab,
                              EnvVarsView envView, Spinner box64Spinner) {
        if (envTab == null || envView == null || box64Spinner == null) return;

        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText("Quick game profile (optional)");
        envTab.addView(button, 0, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        button.setOnClickListener(view -> new AlertDialog.Builder(context)
            .setTitle("Performance and compatibility")
            .setItems(ProfileRules.LABELS, (dialog, profile) -> {
                EnvVars current = new EnvVars(envView.getEnvVars());
                Map<String, String> values = new LinkedHashMap<>();
                for (String key : current) values.put(key, current.get(key));

                String box64Id = ProfileRules.apply(profile, values);
                EnvVars updated = new EnvVars();
                for (Map.Entry<String, String> item : values.entrySet())
                    updated.put(item.getKey(), item.getValue());
                envView.setEnvVars(updated);

                if (box64Id != null) Box64PresetManager.loadSpinner(box64Spinner, box64Id);
                new AlertDialog.Builder(context)
                    .setTitle(ProfileRules.LABELS[profile])
                    .setMessage(ProfileRules.DESCRIPTIONS[profile]
                        + "\n\nReview the settings, then tap Save/Confirm to persist them.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show());
    }
}
