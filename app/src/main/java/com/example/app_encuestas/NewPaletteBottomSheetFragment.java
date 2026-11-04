package com.example.app_encuestas;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class NewPaletteBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String ARG_COLORS = "colors";
    private final TextInputEditText[] inputs = new TextInputEditText[4];
    private final View[] swatches = new View[4];
    private final View[] previewSwatches = new View[4];
    private TextInputEditText nameInput;
    private LinearLayout preview;
    private String[] palette;

    public static NewPaletteBottomSheetFragment newInstance(String[] colors) {
        NewPaletteBottomSheetFragment fragment = new NewPaletteBottomSheetFragment();
        Bundle args = new Bundle();
        args.putStringArray(ARG_COLORS, colors);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.bottom_sheet_new_palette, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        String[] initial = requireArguments().getStringArray(ARG_COLORS);
        palette = initial == null || initial.length != 4
                ? new String[]{"#FFFFFF", "#0F1115", "#6B7280", "#57A4DB"}
                : initial.clone();
        nameInput = view.findViewById(R.id.newPaletteName);
        nameInput.setText("Mi paleta " + (PaletteManager.getCustomPalettes(requireContext()).length + 1));
        preview = view.findViewById(R.id.palettePreview);
        for (int i = 0; i < previewSwatches.length; i++) {
            View previewSwatch = new View(requireContext());
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1);
            previewParams.setMargins(dp(2), dp(2), dp(2), dp(2));
            preview.addView(previewSwatch, previewParams);
            previewSwatches[i] = previewSwatch;
        }

        LinearLayout fields = view.findViewById(R.id.newPaletteFields);
        for (int role = 0; role < PaletteManager.ROLES.length; role++) {
            addColorField(fields, role);
        }

        MaterialButton save = view.findViewById(R.id.saveNewPalette);
        updateActionStyle(view);
        save.setOnClickListener(v -> savePalette());
    }

    private void addColorField(LinearLayout parent, int role) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(8), dp(8));
        GradientDrawable rowBackground = new GradientDrawable();
        rowBackground.setColor(0xFFF7F9FC);
        rowBackground.setCornerRadius(dp(16));
        rowBackground.setStroke(dp(1), 0xFFE8EDF3);
        row.setBackground(rowBackground);

        View swatch = new View(requireContext());
        swatches[role] = swatch;
        LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        swatchParams.setMargins(0, 0, dp(10), 0);
        row.addView(swatch, swatchParams);

        TextInputLayout inputLayout = new TextInputLayout(requireContext());
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setBoxCornerRadii(12, 12, 12, 12);
        inputLayout.setHintTextColor(android.content.res.ColorStateList.valueOf(
                PaletteManager.color(palette[2], Color.DKGRAY)));
        inputLayout.setHint(PaletteManager.ROLES[role] + " · HEX");
        inputLayout.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setTextColor(0xFF0F1115);
        input.setHintTextColor(0xFF6B7280);
        input.setText(palette[role]);
        inputs[role] = input;
        inputLayout.addView(input);
        row.addView(inputLayout);

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String value = s.toString().trim().toUpperCase();
                if (PaletteManager.isValidHex(value)) {
                    palette[role] = value;
                    input.setError(null);
                    updateSwatch(role);
                    updateActionStyle(requireView());
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) { }
        });

        input.setOnFocusChangeListener((v, focused) -> {
            if (!focused) updateColor(role);
        });
        updateSwatch(role);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(10);
        parent.addView(row, rowParams);
    }

    private boolean updateColor(int role) {
        String value = inputs[role].getText() == null
                ? "" : inputs[role].getText().toString().trim().toUpperCase();
        if (PaletteManager.isValidHex(value)) {
            palette[role] = value;
            inputs[role].setError(null);
            updateSwatch(role);
            updateActionStyle(requireView());
            return true;
        } else {
            inputs[role].setError("Código HEX inválido");
            return false;
        }
    }

    private void updateSwatch(int role) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(PaletteManager.color(palette[role], Color.WHITE));
        background.setStroke(dp(1), 0x220F1115);
        swatches[role].setBackground(background);

        GradientDrawable previewBackground = new GradientDrawable();
        previewBackground.setColor(PaletteManager.color(palette[role], Color.WHITE));
        previewBackground.setCornerRadius(dp(8));
        previewSwatches[role].setBackground(previewBackground);
    }

    private void savePalette() {
        boolean valid = true;
        for (int role = 0; role < palette.length; role++) {
            if (!updateColor(role)) valid = false;
        }
        if (!valid) return;
        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            nameInput.setError("Escribe un nombre para la paleta");
            nameInput.requestFocus();
            return;
        }
        Bundle result = new Bundle();
        result.putStringArray("colors", palette);
        result.putString("name", name);
        getParentFragmentManager().setFragmentResult("new_palette_result", result);
        dismiss();
    }

    private void updateActionStyle(View root) {
        MaterialButton save = root.findViewById(R.id.saveNewPalette);
        if (save == null || palette == null) return;
        int accent = PaletteManager.color(palette[3], Color.BLUE);
        save.setBackgroundTintList(ColorStateList.valueOf(accent));
        save.setTextColor(contrastColor(accent));
    }

    private int contrastColor(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255;
        return luminance > 0.58 ? Color.BLACK : Color.WHITE;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
