package com.example.app_encuestas;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

/** Shared toolbar, overflow menu, edge-to-edge insets, and bottom navigation. */
public final class ScreenChrome {
    private ScreenChrome() { }

    public static void setup(Activity activity, View root, MaterialToolbar toolbar, int selectedItem) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), bars.top, view.getPaddingRight(), 0);
            BottomNavigationView navigation = activity.findViewById(R.id.bottomNavigation);
            if (navigation != null) {
                navigation.setPadding(navigation.getPaddingLeft(), navigation.getPaddingTop(),
                        navigation.getPaddingRight(), bars.bottom);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationIconTint(activity.getColor(R.color.white));
        toolbar.setNavigationOnClickListener(v -> activity.finish());
        toolbar.inflateMenu(R.menu.toolbar_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                activity.startActivity(new Intent(activity, ConfiguracionActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_logout) {
                logout(activity);
                return true;
            }
            return false;
        });

        BottomNavigationView navigation = activity.findViewById(R.id.bottomNavigation);
        navigation.setSelectedItemId(selectedItem);
        navigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItem) return true;
            if (id == R.id.nav_home) {
                navigate(activity, new Intent(activity, MainActivity.class));
            } else if (id == R.id.nav_surveys) {
                navigate(activity, new Intent(activity, SurveyManagementActivity.class));
            } else if (id == R.id.nav_history) {
                navigate(activity, new Intent(activity, SurveyHistoryActivity.class));
            } else if (id == R.id.nav_profile) {
                navigate(activity, new Intent(activity, PerfilActivity.class));
            } else if (id == R.id.nav_statistics) {
                navigate(activity, new Intent(activity, StatisticsActivity.class));
            }
            return true;
        });
    }

    public static void navigate(Activity activity, Intent intent) {
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    private static void logout(Activity activity) {
        UserManager.clearSession(activity);
        FirebaseAuth.getInstance().signOut();
        try {
            GoogleSignIn.getClient(activity,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()).signOut();
        } catch (Exception ignored) { }
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }
}
