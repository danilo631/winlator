package com.winlator.optimization;

import android.view.View;
import android.widget.TextView;

import com.winlator.R;
import com.winlator.container.Container;
import com.winlator.core.EnvVars;
import com.winlator.core.WineInfo;

import java.util.Locale;

/** Material-inspired library metadata, using only installed/runtime configuration. */
public final class EnhancedLibraryUI {
    private EnhancedLibraryUI() {}

    public static void bindContainer(View card, Container item) {
        TextView runtime = card.findViewById(R.id.TVEngine);
        TextView driver = card.findViewById(R.id.TVDriver);
        TextView wine = card.findViewById(R.id.TVWineLabel);
        EnvVars vars = new EnvVars(item.getEnvVars());
        String backend = vars.get(FexWineRuntime.ENV_BACKEND);
        boolean fex = FexWineRuntime.BACKEND_FEXCORE.equals(backend);
        runtime.setText(fex ? "FEXCore • experimental" : "Box64 • padrão");
        driver.setText(item.getGraphicsDriver().replace(",", " + ")
            + "  ·  " + item.getScreenSize());
        String selectedWine = item.getWineVersion();
        wine.setText(WineInfo.isMainWineVersion(selectedWine)
            ? "Wine integrado" : "Wine selecionado: " + selectedWine);
        card.setContentDescription(item.getName() + ". "
            + runtime.getText() + ". " + driver.getText());
    }

    public static void summary(View root, int visible, int total) {
        TextView status = root.findViewById(R.id.TVLibrarySummary);
        if (status != null) {
            status.setText(String.format(Locale.getDefault(),
                "%d de %d contêineres  •  CPU Box64 ou FEXCore", visible, total));
        }
    }
}
