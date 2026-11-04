package com.example.app_encuestas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PerfilActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        PaletteTheme.apply(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TextView tvProfileName = findViewById(R.id.tvProfileName);
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        TextView tvProfileAvatar = findViewById(R.id.tvProfileAvatar);
        TextInputEditText etName = findViewById(R.id.etName);
        TextInputEditText etEmail = findViewById(R.id.etEmail);
        TextInputEditText etCurrent = findViewById(R.id.etCurrent);
        TextInputEditText etNew = findViewById(R.id.etNew);

        // Cargar datos del usuario actual desde UserManager
        tvProfileName.setText(UserManager.getName(this));
        tvProfileEmail.setText(UserManager.getEmail(this));
        tvProfileAvatar.setText(UserManager.getInitials(this));
        etName.setText(UserManager.getName(this));
        etEmail.setText(UserManager.getEmail(this));

        // Flecha para volver al dashboard
        toolbar.setNavigationOnClickListener(v -> finish());

        // Guardar datos personales
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String newEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

            if (name.isEmpty()) {
                etName.setError("Escribe tu nombre");
                return;
            }
            if (newEmail.isEmpty()) {
                etEmail.setError("Escribe tu correo o usuario");
                return;
            }

            // Guardar cambios en la sesión local
            UserManager.saveUser(this, name, newEmail);

            // Actualizar interfaz inmediatamente
            tvProfileName.setText(UserManager.getName(this));
            tvProfileEmail.setText(UserManager.getEmail(this));
            tvProfileAvatar.setText(UserManager.getInitials(this));

            Toast.makeText(this, "Cambios guardados con éxito", Toast.LENGTH_SHORT).show();
        });

        // Cambiar contraseña
        findViewById(R.id.btnUpdatePass).setOnClickListener(v -> {
            String current = etCurrent.getText() != null ? etCurrent.getText().toString() : "";
            String nueva = etNew.getText() != null ? etNew.getText().toString() : "";

            if (current.isEmpty()) {
                etCurrent.setError("Escribe tu contraseña actual");
                return;
            }
            if (nueva.length() < 6) {
                etNew.setError("Mínimo 6 caracteres");
                return;
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                currentUser.updatePassword(nueva).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        etCurrent.setText("");
                        etNew.setText("");
                        Toast.makeText(PerfilActivity.this, "Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show();
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Error al actualizar";
                        Toast.makeText(PerfilActivity.this, "Error: " + err, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                etCurrent.setText("");
                etNew.setText("");
                Toast.makeText(this, "Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show();
            }
        });

        // Cerrar sesión
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            UserManager.clearSession(this);
            FirebaseAuth.getInstance().signOut();
            try {
                GoogleSignIn.getClient(
                        this,
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                ).signOut();
            } catch (Exception ignored) {
            }

            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }
}
