package com.example.app_encuestas;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;

public final class PaletteManager {
    public static final String[] ROLES = {"Fondo", "Principal", "Texto", "Acento"};
    private static final String PREFS = "app_settings";
    private static final String NAME = "system_name";
    private static final String ACTIVE = "active_palette";
    private static final String ACTIVE_MIGRATED = "active_palette_v2_migrated";
    private static final String CUSTOM = "custom_palettes";
    private static final String[] DEFAULTS = {"#FFFFFF", "#0F1115", "#6B7280", "#57A4DB"};
    private static final String[] BUILT_IN = {"#FFF8F1", "#5B2C3F", "#62545A", "#E39A43"};
    private static final String[] ATARDECER = {"#FFF7ED", "#7C2D12", "#44403C", "#F97316"};

    private PaletteManager() { }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getName(Context context) {
        return prefs(context).getString(NAME, "App Encuestas");
    }

    public static String[] getActivePalette(Context context) {
        SharedPreferences preferences = prefs(context);
        String stored = preferences.getString(ACTIVE, null);
        if (stored != null && stored.equals(join(ATARDECER))) {
            stored = join(DEFAULTS);
            preferences.edit()
                    .putString(ACTIVE, stored)
                    .putBoolean(ACTIVE_MIGRATED, true)
                    .apply();
            return DEFAULTS.clone();
        }
        if (!preferences.getBoolean(ACTIVE_MIGRATED, false)) {
            stored = join(DEFAULTS);
            preferences.edit()
                    .putString(ACTIVE, stored)
                    .putBoolean(ACTIVE_MIGRATED, true)
                    .apply();
        }
        return stored == null ? DEFAULTS.clone() : parsePalette(stored);
    }

    public static String[] getBuiltInPalette() {
        return BUILT_IN.clone();
    }

    public static String[] getDefaultPalette() {
        return DEFAULTS.clone();
    }

    public static void saveSettings(Context context, String name, String[] colors) {
        prefs(context).edit()
                .putString(NAME, name.trim().isEmpty() ? "App Encuestas" : name.trim())
                .putString(ACTIVE, join(colors))
                .putBoolean(ACTIVE_MIGRATED, true)
                .apply();
    }

    public static void saveCustomPalette(Context context, String[] colors) {
        saveCustomPalette(context, "Mi paleta", colors);
    }

    public static void saveCustomPalette(Context context, String name, String[] colors) {
        if (colors == null || colors.length != 4) return;
        String safeName = name == null || name.trim().isEmpty() ? "Mi paleta" : name.trim();
        safeName = safeName.replace("~", " ").replace(";", " ");
        String old = prefs(context).getString(CUSTOM, "");
        String entry = safeName + "~" + join(colors);
        String value = old.isEmpty() ? entry : old + ";" + entry;
        prefs(context).edit().putString(CUSTOM, value).apply();
    }

    public static String[][] getCustomPalettes(Context context) {
        String[] entries = getCustomPaletteEntries(context);
        String[][] result = new String[entries.length][4];
        for (int i = 0; i < entries.length; i++) {
            result[i] = parsePalette(paletteData(entries[i]));
        }
        return result;
    }

    public static String[] getCustomPaletteNames(Context context) {
        String[] entries = getCustomPaletteEntries(context);
        String[] names = new String[entries.length];
        for (int i = 0; i < entries.length; i++) {
            String[] parts = entries[i].split("~", 2);
            names[i] = parts.length == 2 ? parts[0] : "Mi paleta " + (i + 1);
        }
        return names;
    }

    public static boolean deleteCustomPalette(Context context, String name, String[] colors) {
        if (colors == null || colors.length != 4) return false;
        String[] entries = getCustomPaletteEntries(context);
        ArrayList<String> remaining = new ArrayList<>(Arrays.asList(entries));
        boolean removed = false;
        for (int i = 0; i < remaining.size(); i++) {
            String entry = remaining.get(i);
            String[] parts = entry.split("~", 2);
            String entryName = parts.length == 2 ? parts[0] : "Mi paleta " + (i + 1);
            if (entryName.equals(name) && Arrays.equals(parsePalette(paletteData(entry)), colors)) {
                remaining.remove(i);
                removed = true;
                break;
            }
        }
        if (!removed) return false;

        String remainingRaw = String.join(";", remaining);
        prefs(context).edit().putString(CUSTOM, remainingRaw).apply();

        boolean duplicateRemains = false;
        for (String entry : remaining) {
            if (Arrays.equals(parsePalette(paletteData(entry)), colors)) {
                duplicateRemains = true;
                break;
            }
        }
        if (!duplicateRemains && Arrays.equals(getActivePalette(context), colors)) {
            saveSettings(context, getName(context), DEFAULTS);
        }
        return true;
    }

    public static int removeCustomPalettesNamed(Context context, String name) {
        String[] entries = getCustomPaletteEntries(context);
        ArrayList<String> remaining = new ArrayList<>(Arrays.asList(entries));
        ArrayList<String[]> removedColors = new ArrayList<>();
        for (int i = remaining.size() - 1; i >= 0; i--) {
            String entry = remaining.get(i);
            String[] parts = entry.split("~", 2);
            String entryName = parts.length == 2 ? parts[0] : "Mi paleta " + (i + 1);
            if (entryName.equalsIgnoreCase(name)) {
                removedColors.add(parsePalette(paletteData(entry)));
                remaining.remove(i);
            }
        }
        if (removedColors.isEmpty()) return 0;

        prefs(context).edit().putString(CUSTOM, String.join(";", remaining)).apply();
        String[] active = getActivePalette(context);
        boolean activeWasRemoved = false;
        for (String[] removed : removedColors) {
            if (!Arrays.equals(active, removed)) continue;
            boolean duplicateRemains = false;
            for (String entry : remaining) {
                if (Arrays.equals(parsePalette(paletteData(entry)), removed)) {
                    duplicateRemains = true;
                    break;
                }
            }
            if (!duplicateRemains) activeWasRemoved = true;
        }
        if (activeWasRemoved) saveSettings(context, getName(context), DEFAULTS);
        return removedColors.size();
    }

    private static String[] getCustomPaletteEntries(Context context) {
        String raw = prefs(context).getString(CUSTOM, "");
        return raw.isEmpty() ? new String[0] : raw.split(";");
    }

    private static String paletteData(String entry) {
        String[] parts = entry.split("~", 2);
        return parts.length == 2 ? parts[1] : entry;
    }

    public static boolean isValidHex(String value) {
        try {
            Color.parseColor(value);
            return value.matches("#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static int roleIndex(String role) {
        for (int i = 0; i < ROLES.length; i++) {
            if (ROLES[i].equals(role)) return i;
        }
        return 0;
    }

    public static int color(String value, int fallback) {
        try {
            return Color.parseColor(value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static String join(String[] values) {
        return String.join(",", values);
    }

    private static String[] parsePalette(String value) {
        String[] values = value.split(",");
        if (values.length != 4) return DEFAULTS.clone();
        return values;
    }
}
