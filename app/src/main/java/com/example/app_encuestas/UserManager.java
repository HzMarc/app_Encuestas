package com.example.app_encuestas;

import android.content.Context;
import android.content.SharedPreferences;

public class UserManager {

    private static final String PREF_NAME = "user_session";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_EMAIL = "user_email";

    public static void saveUser(Context context, String name, String email) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit()
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public static String getName(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(KEY_NAME, "Administrador");
    }

    public static String getEmail(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(KEY_EMAIL, "admin@alcaldia.gob.sv");
    }

    public static String getFirstName(Context context) {
        String fullName = getName(context);
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Usuario";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    public static String getInitials(Context context) {
        String name = getName(context);
        if (name == null || name.trim().isEmpty()) {
            return "U";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            String first = parts[0].substring(0, 1).toUpperCase();
            String second = parts[1].substring(0, 1).toUpperCase();
            return first + second;
        } else if (parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase();
        } else {
            return parts[0].substring(0, 1).toUpperCase();
        }
    }

    public static void clearSession(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().clear().apply();
    }

    public static String deriveNameFromInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Administrador";
        }
        String trimmed = input.trim();
        if (trimmed.equalsIgnoreCase("admin")) {
            return "Administrador";
        }
        if (trimmed.contains("@")) {
            String username = trimmed.split("@")[0];
            String[] parts = username.split("[._-]");
            StringBuilder nameBuilder = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    nameBuilder.append(part.substring(0, 1).toUpperCase())
                            .append(part.substring(1).toLowerCase())
                            .append(" ");
                }
            }
            return nameBuilder.toString().trim();
        }
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }
}