package com.example.app_encuestas;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.view.WindowInsetsControllerCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarDataSet;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Applies the saved palette to screens whose XML colors are only fallback defaults. */
public final class PaletteTheme {
    private PaletteTheme() { }

    public static void apply(Activity activity) {
        String[] colors = PaletteManager.getActivePalette(activity);
        int background = PaletteManager.color(colors[0], Color.WHITE);
        int principal = PaletteManager.color(colors[1], Color.BLACK);
        int text = PaletteManager.color(colors[2], Color.GRAY);
        int accent = PaletteManager.color(colors[3], Color.BLUE);
        int surface = mix(background, text, isLight(background) ? 0.045f : 0.12f);

        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content != null && content.getChildCount() > 0) {
            View root = content.getChildAt(0);
            root.setBackgroundColor(background);
            styleView(root, activity, background, principal, text, accent, surface);
        }

        activity.getWindow().setStatusBarColor(principal);
        activity.getWindow().setNavigationBarColor(surface);
        WindowInsetsControllerCompat bars = new WindowInsetsControllerCompat(
                activity.getWindow(), content);
        bars.setAppearanceLightStatusBars(isLight(principal));
        bars.setAppearanceLightNavigationBars(isLight(surface));
    }

    private static void styleView(View view, Activity activity, int background,
                                  int principal, int text, int accent, int surface) {
        if (view instanceof MaterialToolbar) {
            MaterialToolbar toolbar = (MaterialToolbar) view;
            if (activity instanceof MainActivity) {
                toolbar.setTitle(PaletteManager.getName(activity));
            }
            toolbar.setBackgroundColor(principal);
            toolbar.setTitleTextColor(readableTextColor(principal));
            toolbar.setNavigationIconTint(readableTextColor(principal));
            if (toolbar.getOverflowIcon() != null) {
                toolbar.getOverflowIcon().setTint(readableTextColor(principal));
            }
            for (int i = 0; i < toolbar.getMenu().size(); i++) {
                if (toolbar.getMenu().getItem(i).getIcon() != null) {
                    toolbar.getMenu().getItem(i).getIcon().setTint(readableTextColor(principal));
                }
            }
            return;
        }
        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            card.setCardBackgroundColor(surface);
            card.setStrokeColor(mix(surface, text, 0.45f));
        }
        if (view instanceof TextInputLayout) {
            TextInputLayout input = (TextInputLayout) view;
            input.setBoxStrokeColor(accent);
            input.setDefaultHintTextColor(ColorStateList.valueOf(text));
        }
        if (view instanceof TextInputEditText) {
            TextInputEditText input = (TextInputEditText) view;
            input.setTextColor(principal);
            input.setHintTextColor(text);
        } else if (view instanceof MaterialButton) {
            MaterialButton button = (MaterialButton) view;
            boolean outlined = button.getStrokeWidth() > 0;
            if (outlined) {
                button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                button.setStrokeColor(ColorStateList.valueOf(accent));
                button.setTextColor(accent);
            } else {
                button.setBackgroundTintList(ColorStateList.valueOf(accent));
                button.setTextColor(readableTextColor(accent));
            }
        } else if (view instanceof TextView) {
            TextView label = (TextView) view;
            boolean emphasized = label.getTypeface() != null
                    && label.getTypeface().getStyle() == Typeface.BOLD;
            emphasized |= label.getTextSize()
                    / activity.getResources().getDisplayMetrics().scaledDensity >= 20f;
            if (view.getId() == R.id.tvForgot) {
                label.setTextColor(accent);
            } else if (view.getId() == R.id.tvActiveCount
                    || view.getId() == R.id.tvResponsesCount) {
                label.setTextColor(accent);
            } else if (view.getId() == R.id.tvProfileAvatar
                    || "✓".contentEquals(label.getText())) {
                label.setBackgroundTintList(ColorStateList.valueOf(accent));
                label.setTextColor(readableTextColor(accent));
            } else {
                label.setTextColor(emphasized ? principal : text);
            }
        }
        if (view instanceof TabLayout) {
            TabLayout tabs = (TabLayout) view;
            tabs.setBackgroundColor(surface);
            tabs.setTabTextColors(text, principal);
            tabs.setSelectedTabIndicatorColor(accent);
        }
        if (view instanceof BottomNavigationView) {
            BottomNavigationView navigation = (BottomNavigationView) view;
            ColorStateList itemColors = new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{accent, text});
            navigation.setBackgroundColor(surface);
            navigation.setItemIconTintList(itemColors);
            navigation.setItemTextColor(itemColors);
        }
        if (view instanceof BarChart) {
            BarChart chart = (BarChart) view;
            if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
                if (chart.getData().getDataSetByIndex(0) instanceof BarDataSet) {
                    ((BarDataSet) chart.getData().getDataSetByIndex(0)).setColor(accent);
                    chart.getData().notifyDataChanged();
                    chart.notifyDataSetChanged();
                }
            }
            chart.getAxisLeft().setTextColor(text);
            chart.getAxisLeft().setGridColor(mix(surface, text, 0.35f));
            chart.getXAxis().setTextColor(text);
            chart.getLegend().setTextColor(text);
            chart.invalidate();
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                styleView(group.getChildAt(i), activity, background,
                        principal, text, accent, surface);
            }
        }
    }

    private static int readableTextColor(int color) {
        return isLight(color) ? Color.BLACK : Color.WHITE;
    }

    private static boolean isLight(int color) {
        return (0.2126 * Color.red(color) + 0.7152 * Color.green(color)
                + 0.0722 * Color.blue(color)) / 255.0 > 0.58;
    }

    private static int mix(int base, int overlay, float amount) {
        float inverse = 1f - amount;
        return Color.rgb(
                Math.round(Color.red(base) * inverse + Color.red(overlay) * amount),
                Math.round(Color.green(base) * inverse + Color.green(overlay) * amount),
                Math.round(Color.blue(base) * inverse + Color.blue(overlay) * amount));
    }
}
