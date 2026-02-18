package es.iescarrillo.sneakerhub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.models.User;

public class LoginActivity extends AppCompatActivity {

    // UI Elements
    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoogle;
    private TextView tvForgotPassword;

    // Firebase & Google
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();

        // Configurar Google
        configureGoogleSignIn();

        // Vincular Vistas
        initViews();

        // Listeners (Botones)
        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
        tvForgotPassword.setOnClickListener(v -> resetPassword());
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    /**
     * Iniciar sesión con correo y contraseña
     */
    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    // Si entra con correo, asumimos que ya se registró bien y tiene datos.
                    goToMain();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show());
    }

    /**
     * Recuperar contraseña
     */
    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Escribe aquí tu correo válido primero");
            etEmail.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Correo de recuperación enviado", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Configurar Google Sign In
     */
    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    /**
     * Iniciar sesión con Google
     */
    private void signInWithGoogle() {
        // TRUCO: Desconectamos primero para forzar que salga el selector de cuentas siempre
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    /**
     * Launcher para Google Sign In
     */
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google falló: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    /**
     * Firebase Auth con Google
     * @param idToken
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    // EL GUARDIÁN: Verificamos si este usuario de Google ya tiene datos en Realtime DB
                    checkUserInDatabase(mAuth.getCurrentUser());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error Auth Google", Toast.LENGTH_SHORT).show());
    }

    /**
     * Verificar si el usuario ya tiene datos en Realtime DB
     * @param firebaseUser
     */
    private void checkUserInDatabase(FirebaseUser firebaseUser) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(firebaseUser.getUid());

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    // CASO 1: Ya existe -> Pa' dentro
                    goToMain();
                } else {
                    // CASO 2: Es nuevo (entró por Login en vez de Registro) -> Lo guardamos
                    saveUserToRealtimeDb(firebaseUser);
                }
            } else {
                // Si falla la lectura por internet, dejamos pasar por usabilidad
                goToMain();
            }
        });
    }

    /**
     * Guardar en Realtime Database
     * @param firebaseUser
     */
    private void saveUserToRealtimeDb(FirebaseUser firebaseUser) {
        User newUser = new User(
                firebaseUser.getUid(),
                firebaseUser.getDisplayName(),
                firebaseUser.getEmail(),
                "Sin teléfono" // Dato por defecto al venir de Google
        );

        FirebaseDatabase.getInstance().getReference("users")
                .child(firebaseUser.getUid())
                .setValue(newUser)
                .addOnSuccessListener(aVoid -> goToMain());

    }

    /**
     * Navegar a la tienda
     */
    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}