package com.example.app_encuestas;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;

import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieDataSet;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Applies the saved palette to screens whose XML colors are only fallback defaults. */
public final class PaletteTheme {
    private PaletteTheme() { }

    public static void apply(Activity activity) {
        // Hide the Android status bar (clock, signal and battery) across app screens.
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
            applyToView(root, activity);
            // The screen root is the page background, not a raised card surface.
            root.setBackgroundColor(background);
        }

        activity.getWindow().setStatusBarColor(principal);
        activity.getWindow().setNavigationBarColor(surface);
        WindowInsetsControllerCompat bars = new WindowInsetsControllerCompat(
                activity.getWindow(), content);
        bars.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        bars.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
    }

    /** Applies the currently saved palette to a dynamically-created view subtree. */
    public static void applyToView(View root, Activity activity) {
        String[] colors = PaletteManager.getActivePalette(activity);
        int background = PaletteManager.color(colors[0], Color.WHITE);
        int principal = PaletteManager.color(colors[1], Color.BLACK);
        int text = PaletteManager.color(colors[2], Color.GRAY);
        int accent = PaletteManager.color(colors[3], Color.BLUE);
        int surface = mix(background, text, isLight(background) ? 0.045f : 0.12f);
        styleView(root, activity, background, principal, text, accent, surface);
    }

    public static int textColor(Activity activity) {
        String[] colors = PaletteManager.getActivePalette(activity);
        return PaletteManager.color(colors[2], Color.GRAY);
    }

    public static int principalColor(Activity activity) {
        String[] colors = PaletteManager.getActivePalette(activity);
        return PaletteManager.color(colors[1], Color.BLACK);
    }

    public static int accentColor(Activity activity) {
        String[] colors = PaletteManager.getActivePalette(activity);
        return PaletteManager.color(colors[3], Color.BLUE);
    }

    public static int surfaceColor(Activity activity) {
        String[] colors = PaletteManager.getActivePalette(activity);
        int background = PaletteManager.color(colors[0], Color.WHITE);
        int text = PaletteManager.color(colors[2], Color.GRAY);
        return mix(background, text, isLight(background) ? 0.045f : 0.12f);
    }

    /** Dialog windows are outside android.R.id.content, so style them explicitly. */
    public static void applyToDialog(Dialog dialog, Activity activity) {
        String[] colors = PaletteManager.getActivePalette(activity);
        int background = PaletteManager.color(colors[0], Color.WHITE);
        int principal = PaletteManager.color(colors[1], Color.BLACK);
        int text = PaletteManager.color(colors[2], Color.GRAY);
        int accent = PaletteManager.color(colors[3], Color.BLUE);
        int surface = mix(background, text, isLight(background) ? 0.045f : 0.12f);

        View content = dialog.findViewById(android.R.id.content);
        if (content != null) {
            content.setBackgroundColor(surface);
            styleView(content, activity, background, principal, text, accent, surface);
        }
        View decor = dialog.getWindow() == null ? null : dialog.getWindow().getDecorView();
        if (decor instanceof ViewGroup) {
            styleView(decor, activity, background, principal, text, accent, surface);
        }
        Button positive = dialog.findViewById(android.R.id.button1);
        Button negative = dialog.findViewById(android.R.id.button2);
        Button neutral = dialog.findViewById(android.R.id.button3);
        styleDialogButton(positive, accent, readableTextColor(accent));
        styleDialogButton(negative, surface, principal);
        styleDialogButton(neutral, surface, principal);
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            WindowInsetsControllerCompat bars = new WindowInsetsControllerCompat(
                    dialog.getWindow(), dialog.getWindow().getDecorView());
            bars.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            bars.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
        }
    }

    private static void styleDialogButton(Button button, int background, int foreground) {
        if (button == null) return;
        button.setTextColor(foreground);
        button.setBackgroundTintList(ColorStateList.valueOf(background));
    }

    private static void styleView(View view, Activity activity, int background,
                                  int principal, int text, int accent, int surface) {
        if (view.getBackground() instanceof ColorDrawable) {
            int current = ((ColorDrawable) view.getBackground()).getColor();
            if (current == Color.WHITE || current == 0xFFF2F4F7 || current == 0xFFF6F8FA
                    || current == 0xFFF7F9FC) {
                view.setBackgroundColor(surface);
            }
        }
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
        if (view.getId() == R.id.usersHeader) view.setBackgroundColor(accent);
        if (view.getId() == R.id.surveyQuestionRow) {
            android.graphics.drawable.GradientDrawable rowBackground =
                    new android.graphics.drawable.GradientDrawable();
            rowBackground.setColor(surface);
            rowBackground.setCornerRadius(14 * activity.getResources()
                    .getDisplayMetrics().density);
            rowBackground.setStroke(Math.round(activity.getResources()
                    .getDisplayMetrics().density), mix(surface, text, 0.25f));
            view.setBackground(rowBackground);
        }
        if (view instanceof FloatingActionButton) {
            FloatingActionButton button = (FloatingActionButton) view;
            button.setBackgroundTintList(ColorStateList.valueOf(accent));
            button.setImageTintList(ColorStateList.valueOf(readableTextColor(accent)));
        }
        if (view instanceof ImageView && view.getId() == R.id.historyCategoryIcon) {
            ((ImageView) view).setColorFilter(accent);
        }
        if (view.getId() == R.id.paletteBottomSheetHandle
                || view.getId() == R.id.surveyDetailsHandle) {
            view.setBackgroundTintList(ColorStateList.valueOf(text));
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
            boolean exportTextButton = button.getId() == R.id.exportSurvey;
            boolean textButton = button.getId() == R.id.editSurvey
                    || button.getId() == R.id.deleteSurvey
                    || button.getId() == R.id.viewSurvey;
            boolean outlined = button.getStrokeWidth() > 0;
            if (exportTextButton) {
                button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                button.setTextColor(accent);
                button.setIconTint(ColorStateList.valueOf(accent));
            } else if (textButton) {
                button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                button.setTextColor(principal);
                button.setIconTint(ColorStateList.valueOf(principal));
            } else if (outlined) {
                button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                button.setStrokeColor(ColorStateList.valueOf(accent));
                button.setTextColor(principal);
            } else {
                button.setBackgroundTintList(ColorStateList.valueOf(accent));
                button.setTextColor(readableTextColor(accent));
            }
        } else if (view instanceof Button) {
            Button button = (Button) view;
            if (button.getId() == android.R.id.button1) {
                button.setBackgroundTintList(ColorStateList.valueOf(accent));
                button.setTextColor(readableTextColor(accent));
            } else {
                button.setBackgroundTintList(ColorStateList.valueOf(surface));
                button.setTextColor(principal);
            }
        } else if (view instanceof TextView) {
            TextView label = (TextView) view;
            boolean emphasized = label.getTypeface() != null
                    && label.getTypeface().getStyle() == Typeface.BOLD;
            emphasized |= label.getTextSize()
                    / activity.getResources().getDisplayMetrics().scaledDensity >= 20f;
            if (view.getId() == R.id.tvForgot) {
                label.setTextColor(principal);
            } else if (view.getId() == R.id.tvActiveCount
                    || view.getId() == R.id.tvResponsesCount) {
                label.setTextColor(principal);
            } else if (view.getId() == R.id.tvProfileAvatar
                    || "✓".contentEquals(label.getText())) {
                label.setBackgroundTintList(ColorStateList.valueOf(accent));
                label.setTextColor(readableTextColor(accent));
            } else if (view.getId() == R.id.surveyStatus) {
                // Keep the established green/gray state chip and its white label.
                label.setTextColor(Color.WHITE);
            } else if (view.getParent() instanceof View
                    && ((View) view.getParent()).getId() == R.id.usersHeader) {
                label.setTextColor(readableTextColor(accent));
            } else {
                if (label.getBackground() instanceof ColorDrawable) {
                    int color = ((ColorDrawable) label.getBackground()).getColor();
                    label.setTextColor(Color.alpha(color) < 16 ? (emphasized ? principal : text)
                            : readableTextColor(color));
                } else {
                    label.setTextColor(emphasized ? principal : text);
                }
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
                    new int[]{accent, principal});
            ColorStateList labelColors = ColorStateList.valueOf(principal);
            navigation.setBackgroundColor(surface);
            navigation.setItemIconTintList(itemColors);
            navigation.setItemTextColor(labelColors);
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
        if (view instanceof PieChart) {
            PieChart chart = (PieChart) view;
            chart.setHoleColor(surface);
            chart.setEntryLabelColor(text);
            chart.setCenterTextColor(principal);
            chart.getLegend().setTextColor(text);
            if (chart.getData() != null && chart.getData().getDataSetCount() > 0
                    && chart.getData().getDataSetByIndex(0) instanceof PieDataSet) {
                ((PieDataSet) chart.getData().getDataSetByIndex(0))
                        .setValueTextColor(readableTextColor(surface));
            }
            chart.invalidate();
        }
        if (view instanceof LineChart) {
            LineChart chart = (LineChart) view;
            if (chart.getData() != null && chart.getData().getDataSetCount() > 0
                    && chart.getData().getDataSetByIndex(0) instanceof LineDataSet) {
                LineDataSet set = (LineDataSet) chart.getData().getDataSetByIndex(0);
                set.setColor(accent);
                set.setCircleColor(accent);
                set.setValueTextColor(text);
                chart.getData().notifyDataChanged();
                chart.notifyDataSetChanged();
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
        return luminance(color) > 0.179 ? Color.BLACK : Color.WHITE;
    }

    private static boolean isLight(int color) {
        return luminance(color) > 0.179;
    }

    private static double luminance(int color) {
        double red = linear(Color.red(color) / 255.0);
        double green = linear(Color.green(color) / 255.0);
        double blue = linear(Color.blue(color) / 255.0);
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private static double linear(double channel) {
        return channel <= 0.04045 ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private static int mix(int base, int overlay, float amount) {
        float inverse = 1f - amount;
        return Color.rgb(
                Math.round(Color.red(base) * inverse + Color.red(overlay) * amount),
                Math.round(Color.green(base) * inverse + Color.green(overlay) * amount),
                Math.round(Color.blue(base) * inverse + Color.blue(overlay) * amount));
    }
}
