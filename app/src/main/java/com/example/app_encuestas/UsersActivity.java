package com.example.app_encuestas;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class UsersActivity extends AppCompatActivity {
    private TableLayout table;
    private TextView empty;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);
        table = findViewById(R.id.usersTable);
        empty = findViewById(R.id.usersEmpty);
        MaterialToolbar toolbar = findViewById(R.id.usersToolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        PaletteTheme.apply(this);
        loadUsers();
    }

    @Override protected void onResume() {
        super.onResume();
        PaletteTheme.apply(this);
    }

    private void loadUsers() {
        LoginHistoryStore.loadUsers(this, this::showUsers);
    }

    private void showUsers(List<LoginHistoryStore.UserRecord> users) {
        while (table.getChildCount() > 1) table.removeViewAt(1);
        empty.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
        for (LoginHistoryStore.UserRecord user : users) {
            TableRow row = new TableRow(this);
            row.setBackgroundColor(PaletteTheme.surfaceColor(this));
            row.addView(cell(user.name, true));
            row.addView(cell(user.email, true));
            row.addView(cell(user.formattedDate(), false));
            row.addView(cell(String.valueOf(user.loginCount), false));
            row.addView(cell(user.active ? "Activo" : "Desactivado", false));
            MaterialButton toggle = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            toggle.setText(user.active ? "Desactivar" : "Activar");
            toggle.setAllCaps(false);
            toggle.setMinHeight(Math.round(42 * getResources().getDisplayMetrics().density));
            toggle.setOnClickListener(view -> {
                toggle.setEnabled(false);
                toggle.setText("Guardando…");
                LoginHistoryStore.updateUserActive(this, user, !user.active, saved -> {
                    if (saved) {
                        Toast.makeText(this, user.active
                                ? "Usuario desactivado" : "Usuario activado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "Estado guardado en este dispositivo; no se pudo sincronizar con la nube.",
                                Toast.LENGTH_LONG).show();
                    }
                    loadUsers();
                });
            });
            row.addView(toggle);
            table.addView(row, new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
            PaletteTheme.applyToView(row, this);
        }
    }

    private TextView cell(String value, boolean leftAligned) {
        TextView text = new TextView(this);
        text.setText(value == null ? "" : value);
        text.setTextSize(13);
        text.setTextColor(PaletteTheme.textColor(this));
        text.setGravity((leftAligned ? Gravity.START : Gravity.CENTER) | Gravity.CENTER_VERTICAL);
        int horizontal = Math.round(12 * getResources().getDisplayMetrics().density);
        int vertical = Math.round(10 * getResources().getDisplayMetrics().density);
        text.setPadding(horizontal, vertical, horizontal, vertical);
        text.setMaxWidth(Math.round(220 * getResources().getDisplayMetrics().density));
        return text;
    }
}
