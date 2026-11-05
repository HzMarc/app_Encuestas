package com.example.app_encuestas;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private TextInputEditText etLoginEmail;
    private TextInputEditText etLoginPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        PaletteTheme.apply(this);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        ImageView logo = findViewById(R.id.ivLoginLogo);
        LogoManager.loadInto(this, logo);
        String systemName = PaletteManager.getName(this);
        ((TextView) findViewById(R.id.tvLoginAppName)).setText(systemName);
        ((TextView) findViewById(R.id.tvLoginSubtitle)).setText(
                getString(R.string.login_subtitle, systemName));

        // Configurar Google Sign-In
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Evento del botón Iniciar Sesión (Usuario/Correo y Contraseña)
        findViewById(R.id.btnLogin).setOnClickListener(v -> iniciarSesionConEmail());

        // Evento del botón de Google
        findViewById(R.id.btnGoogle).setOnClickListener(v -> iniciarSesionConGoogle());

        // Evento de ¿Olvidaste tu contraseña?
        findViewById(R.id.tvForgot).setOnClickListener(v -> recuperarContrasena());
    }

    private void iniciarSesionConEmail() {
        String userInput = etLoginEmail.getText() != null ? etLoginEmail.getText().toString().trim() : "";
        String password = etLoginPassword.getText() != null ? etLoginPassword.getText().toString().trim() : "";

        // Validaciones de formulario
        if (TextUtils.isEmpty(userInput)) {
            etLoginEmail.setError("Ingresa tu usuario o correo electrónico");
            etLoginEmail.requestFocus();
            return;
        }

        if (!hasValidatedInternetConnection()) {
            if (!Patterns.EMAIL_ADDRESS.matcher(userInput).matches()) {
                Toast.makeText(this, "Sin internet, ingresa el correo de la sesión verificada en este dispositivo.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            continueWithCachedSession(userInput);
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etLoginPassword.setError("Ingresa tu contraseña");
            etLoginPassword.requestFocus();
            return;
        }

        if (password.length() < 4) {
            etLoginPassword.setError("La contraseña debe tener al menos 4 caracteres");
            etLoginPassword.requestFocus();
            return;
        }

        final String userOrEmail = userInput;

        // Si es un correo válido, intenta primero autenticar con Firebase
        if (Patterns.EMAIL_ADDRESS.matcher(userOrEmail).matches()) {
            try {
                mAuth.signInWithEmailAndPassword(userOrEmail, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                String finalEmail = user != null && user.getEmail() != null ? user.getEmail() : userOrEmail;
                                String finalName = user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                                        ? user.getDisplayName()
                                        : UserManager.deriveNameFromInput(finalEmail);

                                completeLogin(finalName, finalEmail,
                                        user == null ? null : user.getUid());
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "No se pudo iniciar sesión. Revisa el correo y la contraseña.",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                return;
            } catch (Exception error) {
                Toast.makeText(this, "No se pudo iniciar sesión con Firebase.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Si es un nombre de usuario plano (ej: "admin", "kenia", "usuario1")
        ingresarExitoso(userOrEmail);
    }

    private boolean hasValidatedInternetConnection() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (manager == null) return false;
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void continueWithCachedSession(String email) {
        FirebaseUser user = mAuth.getCurrentUser();
        String authenticatedEmail = user == null ? null : user.getEmail();
        if (user == null || authenticatedEmail == null
                || !email.equalsIgnoreCase(authenticatedEmail)
                || !LoginHistoryStore.hasLocallyVerifiedActiveAccount(this, authenticatedEmail)) {
            Toast.makeText(this,
                    "No hay una sesión activa verificada para ese correo. Conéctate a internet e inicia sesión.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String name = user.getDisplayName() == null || user.getDisplayName().trim().isEmpty()
                ? UserManager.deriveNameFromInput(authenticatedEmail) : user.getDisplayName();
        UserManager.saveUser(this, name, authenticatedEmail);
        LoginHistoryStore.recordLogin(this, user.getUid(), name, authenticatedEmail);
        Toast.makeText(this, "Sesión sin conexión · Bienvenido " + name, Toast.LENGTH_SHORT).show();
        openMainActivity();
    }

    private void ingresarExitoso(String userOrEmail) {
        String name = UserManager.deriveNameFromInput(userOrEmail);
        String email = userOrEmail.contains("@") ? userOrEmail : userOrEmail.toLowerCase() + "@alcaldia.gob.sv";
        completeLogin(name, email, null);
    }

    private void completeLogin(String name, String email, String uid) {
        LoginHistoryStore.checkAccountActive(this, uid, email, active -> {
            if (isFinishing() || isDestroyed()) return;
            if (!active) {
                if (uid != null && !uid.trim().isEmpty()) FirebaseAuth.getInstance().signOut();
                Toast.makeText(this,
                        "La cuenta está desactivada o no se pudo verificar. Contacta al administrador.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            UserManager.saveUser(this, name, email);
            LoginHistoryStore.recordLogin(this, uid, name, email);
            Toast.makeText(LoginActivity.this, "Bienvenido " + name, Toast.LENGTH_SHORT).show();
            openMainActivity();
        });
    }

    private void openMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void recuperarContrasena() {
        String userInput = etLoginEmail.getText() != null ? etLoginEmail.getText().toString().trim() : "";
        if (TextUtils.isEmpty(userInput)) {
            etLoginEmail.setError("Escribe tu usuario o correo para restablecer contraseña");
            etLoginEmail.requestFocus();
            return;
        }

        if (Patterns.EMAIL_ADDRESS.matcher(userInput).matches()) {
            try {
                mAuth.sendPasswordResetEmail(userInput)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Se envió un correo para restablecer tu contraseña", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Si el correo está registrado, recibirás un correo.", Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (Exception e) {
                Toast.makeText(LoginActivity.this, "Instrucción procesada correctamente.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(LoginActivity.this, "Solicitud recibida para el usuario: " + userInput, Toast.LENGTH_LONG).show();
        }
    }

    private void iniciarSesionConGoogle() {
        if (mGoogleSignInClient == null) {
            Toast.makeText(this, "Error al inicializar Google Sign-In", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } catch (Exception e) {
            Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Error de autenticación Google: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String email = user != null && user.getEmail() != null ? user.getEmail() : "usuario.google@alcaldia.gob.sv";
                        String name = user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                                ? user.getDisplayName()
                                : UserManager.deriveNameFromInput(email);

                        completeLogin(name, email, user == null ? null : user.getUid());
                    } else {
                        Toast.makeText(LoginActivity.this, "Error al autenticar con Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
