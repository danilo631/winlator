package com.winlator.optimization;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.winlator.MainActivity;
import com.winlator.R;
import com.winlator.core.WineInstaller;

/**
 * Simple discoverability panel injected into the top of Winlator Settings.
 * Doesn't replace the upstream settings or change stored preferences.
 */
public final class SettingsOverviewUI {
    private SettingsOverviewUI() {}

    private static int dp(Context context, int value) {
        return Math.round(context.getResources().getDisplayMetrics().density * value);
    }

    private static TextView line(Context context, String value, int size, int color) {
        TextView label = new TextView(context);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setPadding(0, dp(context, 3), 0, dp(context, 5));
        return label;
    }

    public static void attach(Context context, View settingsRoot) {
        if (!(settingsRoot instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) settingsRoot;
        if (group.getChildCount() == 0 || !(group.getChildAt(0) instanceof ScrollView)) return;
        ScrollView scroll = (ScrollView) group.getChildAt(0);
        if (!(scroll.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout form = (LinearLayout) scroll.getChildAt(0);

        LinearLayout panel = new LinearLayout(context);
        panel.setId(View.generateViewId());
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 16));

        GradientDrawable background = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.rgb(25, 54, 86), Color.rgb(23, 41, 64)});
        background.setCornerRadius(dp(context, 18));
        background.setStroke(dp(context, 1), Color.rgb(55, 94, 129));
        panel.setBackground(background);

        panel.addView(line(context, "WINLATOR ENHANCED  •  ACESSO RÁPIDO",
            11, Color.rgb(125, 211, 252)));
        panel.addView(line(context, "Wine, Proton e compatibilidade",
            20, Color.WHITE));
        int count = WineInstaller.getInstalledWineInfos(context).size();
        panel.addView(line(context,
            "Versões Wine detectadas: " + count
                + "  •  Box64 padrão  •  FEXCore experimental",
            12, Color.rgb(205, 220, 241)));

        Button center = new Button(context);
        center.setAllCaps(false);
        center.setText("Abrir Wine & Proton");
        center.setOnClickListener(v ->
            ((MainActivity) context).showFragment(new RuntimeCenterFragment()));
        panel.addView(center, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 48)));

        Button importWine = new Button(context);
        importWine.setAllCaps(false);
        importWine.setText("Instalar Wine / Proton baixado");
        importWine.setOnClickListener(v -> {
            View originalPicker = settingsRoot.findViewById(R.id.BTInstallWine);
            if (originalPicker != null) originalPicker.performClick();
        });
        LinearLayout.LayoutParams next = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 48));
        next.topMargin = dp(context, 4);
        panel.addView(importWine, next);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(context, 14);
        form.addView(panel, 0, params);
    }
}
