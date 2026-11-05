package com.example.app_encuestas;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class ConfiguracionActivity extends AppCompatActivity {
    private String[] activePalette;
    private TextInputEditText nameInput;
    private ImageView logoPreview;
    private ActivityResultLauncher<String[]> logoPicker;

    @Override
    protected void onResume() {
        super.onResume();
        applyConfigurationPalette();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logoPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (RuntimeException ignored) { }
            LogoManager.saveLogoUri(this, uri);
            refreshLogoPreview();
            Toast.makeText(this, "Logo actualizado", Toast.LENGTH_SHORT).show();
        });
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_configuracion);

        View root = findViewById(R.id.settingsRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), bars.top, view.getPaddingRight(), 0);
            View scroll = findViewById(R.id.settingsScroll);
            scroll.setPadding(scroll.getPaddingLeft(), scroll.getPaddingTop(),
                    scroll.getPaddingRight(), bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        nameInput = findViewById(R.id.etSystemName);
        logoPreview = findViewById(R.id.systemLogoPreview);
        findViewById(R.id.btnChangeLogo).setOnClickListener(view ->
                logoPicker.launch(new String[]{"image/*"}));
        refreshLogoPreview();
        nameInput.setText(PaletteManager.getName(this));
        PaletteManager.removeCustomPalettesNamed(this, "Atardecer");
        activePalette = PaletteManager.getActivePalette(this);

        getSupportFragmentManager().setFragmentResultListener(
                "palette_use_result", this, (requestKey, result) -> {
                    String[] selected = result.getStringArray("colors");
                    if (selected != null && selected.length == 4) {
                        activePalette = selected;
                        PaletteManager.saveSettings(this, currentSystemName(), activePalette);
                        applyConfigurationPalette();
                        Toast.makeText(this, "Paleta aplicada", Toast.LENGTH_SHORT).show();
                    }
                });
        getSupportFragmentManager().setFragmentResultListener(
                "new_palette_result", this, (requestKey, result) -> {
                    String[] created = result.getStringArray("colors");
                    if (created != null && created.length == 4) {
                        String paletteName = result.getString("name", "Mi paleta");
                        PaletteManager.saveCustomPalette(this, paletteName, created);
                        activePalette = created;
                        PaletteManager.saveSettings(this, currentSystemName(), activePalette);
                        applyConfigurationPalette();
                        Toast.makeText(this, "Paleta guardada y aplicada", Toast.LENGTH_SHORT).show();
                    }
                });
        getSupportFragmentManager().setFragmentResultListener(
                "palette_deleted_result", this, (requestKey, result) -> {
                    activePalette = PaletteManager.getActivePalette(this);
                    applyConfigurationPalette();
                    Toast.makeText(this, "Paleta eliminada", Toast.LENGTH_SHORT).show();
                });

        findViewById(R.id.btnNewPalette).setOnClickListener(v -> openNewPalette());
        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> saveAndApply());
        refreshPalettes();
    }

    private void refreshPalettes() {
        LinearLayout container = findViewById(R.id.customPaletteContainer);
        container.removeAllViews();

        addPaletteCard(container, "Paleta actual", activePalette, true, false);
        String[] original = PaletteManager.getDefaultPalette();
        if (!Arrays.equals(activePalette, original)) {
            addPaletteCard(container, "Paleta original", original, false, false);
        }
        String[] builtIn = PaletteManager.getBuiltInPalette();
        addPaletteCard(container, "Paleta Cálida", builtIn,
                Arrays.equals(activePalette, builtIn), false);
        String[][] saved = PaletteManager.getCustomPalettes(this);
        String[] savedNames = PaletteManager.getCustomPaletteNames(this);
        for (int i = 0; i < saved.length; i++) {
            addPaletteCard(container, savedNames[i], saved[i],
                    Arrays.equals(activePalette, saved[i]), true);
        }
    }

    private void applyConfigurationPalette() {
        PaletteTheme.apply(this);
        refreshLogoPreview();
        refreshPalettes();
    }

    private void refreshLogoPreview() {
        if (logoPreview != null) LogoManager.loadInto(this, logoPreview);
    }

    private void addPaletteCard(LinearLayout container, String name, String[] palette,
                                boolean selected, boolean deletable) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardElevation(0);
        card.setRadius(dp(18));
        card.setStrokeWidth(dp(selected ? 2 : 1));
        int accent = PaletteManager.color(activePalette[3], 0xFF57A4DB);
        card.setStrokeColor(selected ? accent : 0xFFE5E9EF);
        card.setCardBackgroundColor(PaletteTheme.surfaceColor(this));
        card.setClickable(true);
        card.setFocusable(true);
        card.setRippleColor(android.content.res.ColorStateList.valueOf(0x180F1115));

        LinearLayout content = new LinearLayout(this);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView title = new TextView(this);
        title.setText(name);
        title.setTextColor(PaletteManager.color(activePalette[1], Color.BLACK));
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout titleColumn = new LinearLayout(this);
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        titleColumn.setGravity(Gravity.CENTER_VERTICAL);
        titleColumn.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(selected ? "En uso · 4 colores" : "4 colores · Ver detalles");
        subtitle.setTextColor(PaletteManager.color(activePalette[2], Color.GRAY));
        subtitle.setTextSize(12);
        titleColumn.addView(subtitle);
        content.addView(titleColumn, new LinearLayout.LayoutParams(0, dp(48), 1));

        for (String value : palette) {
            View swatch = new View(this);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(PaletteManager.color(value, Color.WHITE));
            swatch.setBackground(drawable);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(30), dp(30));
            params.setMargins(dp(5), 0, 0, 0);
            drawable.setStroke(dp(1), 0x220F1115);
            swatch.setContentDescription(value);
            content.addView(swatch, params);
        }

        card.addView(content);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        container.addView(card, cardParams);

        card.setOnClickListener(v -> {
            androidx.fragment.app.FragmentManager manager = getSupportFragmentManager();
            if (manager.isStateSaved() || manager.findFragmentByTag("palette_options") != null) {
                return;
            }
            PaletteOptionDialogFragment.newInstance(name, palette, deletable)
                    .show(manager, "palette_options");
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void saveAndApply() {
        PaletteManager.saveSettings(this,
                currentSystemName(),
                activePalette);
        setResult(RESULT_OK, new Intent());
        Toast.makeText(this, "Configuración aplicada", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void openNewPalette() {
        androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.isStateSaved()
                || fragmentManager.findFragmentByTag("new_palette") != null) {
            return;
        }
        NewPaletteBottomSheetFragment.newInstance(activePalette)
                .show(fragmentManager, "new_palette");
    }

    private String currentSystemName() {
        return nameInput.getText() == null ? "" : nameInput.getText().toString();
    }
}
