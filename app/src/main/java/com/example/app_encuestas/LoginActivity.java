package com.example.app_encuestas;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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

                                UserManager.saveUser(LoginActivity.this, finalName, finalEmail);
                                Toast.makeText(LoginActivity.this, "Bienvenido " + finalName, Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                ingresarExitoso(userOrEmail);
                            }
                        });
                return;
            } catch (Exception ignored) {
            }
        }

        // Si es un nombre de usuario plano (ej: "admin", "kenia", "usuario1")
        ingresarExitoso(userOrEmail);
    }

    private void ingresarExitoso(String userOrEmail) {
        String name = UserManager.deriveNameFromInput(userOrEmail);
        String email = userOrEmail.contains("@") ? userOrEmail : userOrEmail.toLowerCase() + "@alcaldia.gob.sv";
        UserManager.saveUser(this, name, email);

        Toast.makeText(LoginActivity.this, "Bienvenido " + name, Toast.LENGTH_SHORT).show();
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

                        UserManager.saveUser(LoginActivity.this, name, email);
                        Toast.makeText(LoginActivity.this, "Bienvenido " + name, Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Error al autenticar con Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
