package com.winlator.optimization;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import com.winlator.core.GPUHelper;

/** Small toggle in Settings, opt-out for safe native driver guard. */
public final class NativeDriverSettingsUI {
    private NativeDriverSettingsUI() {}

    public static boolean guardEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(NativeDriverPolicy.PREFERENCE, true);
    }

    public static void attach(Context context, LinearLayout panel) {
        TextView info = new TextView(context);
        String renderer = GPUHelper.glGetRenderer(context);
        info.setText("GPU: " + (renderer.isEmpty() ? "Não identificada" : renderer)
            + "\n" + NativeDriverPolicy.recommendation(renderer));
        info.setTextSize(12);
        info.setTextColor(Color.rgb(185, 194, 207));
        info.setPadding(0, 12, 0, 8);
        panel.addView(info);

        Switch guard = new Switch(context);
        guard.setText("Proteção automática do driver");
        guard.setTextColor(Color.WHITE);
        guard.setTextSize(13);
        guard.setChecked(guardEnabled(context));
        guard.setOnCheckedChangeListener((button, checked) ->
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(NativeDriverPolicy.PREFERENCE, checked).apply());
        panel.addView(guard);

        TextView note = new TextView(context);
        note.setText("Em Mali/Xclipse/PowerVR, substitui Turnip e drivers Vulkan "
            + "externos por Vortek/System somente na execução. "
            + "Não muda as opções salvas, nem frequência da GPU.");
        note.setTextSize(11);
        note.setTextColor(Color.rgb(148, 156, 167));
        panel.addView(note);
    }
}
