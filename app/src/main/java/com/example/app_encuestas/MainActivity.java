package com.example.app_encuestas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private TextView tvGreeting;
    private TextView tvHeaderName;
    private TextView tvHeaderEmail;
    private TextView tvHeaderAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        NavigationView navView = findViewById(R.id.navView);
        tvGreeting = findViewById(R.id.tvGreeting);

        // Encabezado del menú lateral
        View header = navView.getHeaderView(0);
        if (header != null) {
            tvHeaderName = header.findViewById(R.id.tvHeaderName);
            tvHeaderEmail = header.findViewById(R.id.tvHeaderEmail);
            tvHeaderAvatar = header.findViewById(R.id.tvHeaderAvatar);
        }

        actualizarDatosUsuario();

        // Abrir menú con el icono de hamburguesa
        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        // Opciones del menú lateral
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                openProfile();
            } else if (id == R.id.nav_logout) {
                logout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Tarjetas del dashboard
        findViewById(R.id.cardPerfil).setOnClickListener(v -> openProfile());
        findViewById(R.id.cardNueva).setOnClickListener(v ->
                Toast.makeText(this, "Nueva encuesta pendiente", Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardLista).setOnClickListener(v ->
                Toast.makeText(this, "Lista de encuestas pendiente", Toast.LENGTH_SHORT).show());

        // Cerrar sesión
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        // Botón atrás: si el menú está abierto, lo cierra primero
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void actualizarDatosUsuario() {
        if (tvGreeting != null) {
            tvGreeting.setText("Hola, " + UserManager.getFirstName(this));
        }
        if (tvHeaderName != null) {
            tvHeaderName.setText(UserManager.getName(this));
        }
        if (tvHeaderEmail != null) {
            tvHeaderEmail.setText(UserManager.getEmail(this));
        }
        if (tvHeaderAvatar != null) {
            tvHeaderAvatar.setText(UserManager.getInitials(this));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarDatosUsuario();
    }

    private void openProfile() {
        Intent i = new Intent(this, PerfilActivity.class);
        startActivity(i);
    }

    private void logout() {
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
    }
}