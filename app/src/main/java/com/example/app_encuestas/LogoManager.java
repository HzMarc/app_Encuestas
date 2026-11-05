package com.example.app_encuestas;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

/** Stores and displays the optional user-selected system logo. */
public final class LogoManager {
    private static final String KEY_LOGO_URI = "system_logo_uri";

    private LogoManager() { }

    public static String getLogoUri(Context context) {
        return PaletteManager.prefs(context).getString(KEY_LOGO_URI, null);
    }

    public static void saveLogoUri(Context context, Uri uri) {
        PaletteManager.prefs(context).edit().putString(KEY_LOGO_URI, uri.toString()).apply();
    }

    public static void clearLogo(Context context) {
        PaletteManager.prefs(context).edit().remove(KEY_LOGO_URI).apply();
    }

    public static void loadInto(Context context, ImageView target) {
        String saved = getLogoUri(context);
        if (saved != null) {
            try {
                target.setImageURI(Uri.parse(saved));
                target.setImageTintList(null);
                if (target.getDrawable() != null) return;
            } catch (RuntimeException ignored) { }
        }
        target.setImageResource(R.drawable.logo_encuestas);
        target.setImageTintList(null);
    }
}
