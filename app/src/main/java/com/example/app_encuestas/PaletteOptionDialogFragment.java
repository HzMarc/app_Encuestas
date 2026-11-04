package com.example.app_encuestas;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.view.Gravity;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.color.MaterialColors;

public class PaletteOptionDialogFragment extends DialogFragment {
    private static final String ARG_NAME = "name";
    private static final String ARG_COLORS = "colors";
    private static final String ARG_DELETABLE = "deletable";

    public static PaletteOptionDialogFragment newInstance(String name, String[] colors,
                                                         boolean deletable) {
        PaletteOptionDialogFragment fragment = new PaletteOptionDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putStringArray(ARG_COLORS, colors);
        args.putBoolean(ARG_DELETABLE, deletable);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String name = requireArguments().getString(ARG_NAME, "Paleta");
        String[] colors = requireArguments().getStringArray(ARG_COLORS);
        if (colors == null || colors.length != 4) {
            colors = PaletteManager.getActivePalette(requireContext());
        }
        final String[] selectedColors = colors;
        boolean deletable = requireArguments().getBoolean(ARG_DELETABLE, false);

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int horizontal = dp(22);
        content.setPadding(horizontal, 0, horizontal, dp(8));

        TextView description = new TextView(requireContext());
        description.setText("Revisa cada color y su función dentro de la aplicación.");
        description.setTextColor(MaterialColors.getColor(requireContext(),
                com.google.android.material.R.attr.colorOnSurface, Color.DKGRAY));
        description.setTextSize(14);
        description.setPadding(0, 0, 0, dp(12));
        content.addView(description);

        for (int i = 0; i < selectedColors.length; i++) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(7), dp(10), dp(7));
            GradientDrawable rowBackground = new GradientDrawable();
            rowBackground.setColor(0xFFF6F8FA);
            rowBackground.setCornerRadius(dp(14));
            row.setBackground(rowBackground);

            View swatch = new View(requireContext());
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.OVAL);
            background.setColor(PaletteManager.color(selectedColors[i], Color.WHITE));
            swatch.setBackground(background);
            LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(dp(36), dp(36));
            row.addView(swatch, swatchParams);

            TextView role = new TextView(requireContext());
            role.setText(PaletteManager.ROLES[i]);
            role.setTextColor(MaterialColors.getColor(requireContext(),
                    com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
            role.setTypeface(null, android.graphics.Typeface.BOLD);
            role.setTextSize(14);
            LinearLayout.LayoutParams roleParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            roleParams.setMargins(dp(12), 0, 0, 0);
            row.addView(role, roleParams);

            TextView hex = new TextView(requireContext());
            hex.setText(selectedColors[i].toUpperCase());
            hex.setTextColor(0xFF657180);
            hex.setTextSize(12);
            hex.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(hex, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            content.addView(row);
            LinearLayout.LayoutParams rowParams = (LinearLayout.LayoutParams) row.getLayoutParams();
            rowParams.bottomMargin = dp(8);
            row.setLayoutParams(rowParams);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(name)
                .setView(content)
                .setNegativeButton("Cerrar", null)
                .setPositiveButton("Usar esta paleta", null);
        if (deletable) builder.setNeutralButton("Eliminar", null);

        android.app.Dialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            Button useButton = dialog.findViewById(android.R.id.button1);
            if (useButton != null) {
                useButton.setOnClickListener(v -> {
                    Bundle result = new Bundle();
                    result.putStringArray("colors", selectedColors);
                    getParentFragmentManager().setFragmentResult(
                            "palette_use_result", result);
                    dismiss();
                });
            }
            Button deleteButton = dialog.findViewById(android.R.id.button3);
            if (deleteButton != null) {
                deleteButton.setOnClickListener(v -> confirmDelete(name, selectedColors));
            }
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels
                        * 0.92f), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });
        return dialog;
    }

    private void confirmDelete(String name, String[] colors) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar paleta")
                .setMessage("¿Eliminar «" + name + "»? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (confirmation, which) -> {
                    if (PaletteManager.deleteCustomPalette(requireContext(), name, colors)) {
                        getParentFragmentManager().setFragmentResult(
                                "palette_deleted_result", new Bundle());
                        dismiss();
                    }
                })
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
