package es.iescarrillo.sneakerhub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.models.User;


public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etUser, etPassword, etConfirmPassword, etPhone;
    private Button btnRegister, btnGoogleRegister;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase database; // Usamos Realtime Database
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializar Firebase Auth y Database
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(); // Coge la URL de google-services.json

        configureGoogleSignIn();

        // Ajuste de barras
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        btnRegister.setOnClickListener(v -> validateAndRegister());
        btnGoogleRegister.setOnClickListener(v -> signInWithGoogle());
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etPhone = findViewById(R.id.etPhone);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogleRegister = findViewById(R.id.btnGoogleRegister);
    }

    /**
     * Validar y registrar usuario
     */
    private void validateAndRegister() {
        String email = etEmail.getText().toString().trim();
        String username = etUser.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (email.isEmpty() || username.isEmpty() || pass.isEmpty() || confirmPass.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show(); return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email inválido"); return;
        }
        if (!pass.equals(confirmPass)) {
            etConfirmPassword.setError("No coinciden"); return;
        }
        if (pass.length() < 6) {
            etPassword.setError("Mínimo 6 caracteres"); return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        // Guardar en Realtime Database
                        if (firebaseUser != null) {
                            saveUserToRealtimeDb(firebaseUser.getUid(), username, email, phone);
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
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
        // Primero desconectamos al cliente de Google para borrar la memoria caché
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {

            // Una vez desconectado, lanzamos el Intent.
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
                        Toast.makeText(this, "Fallo Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        // Guardamos en RTDB (si es nuevo, lo crea; si existe, lo machaca/actualiza)
                        if (user != null) {
                            saveUserToRealtimeDb(user.getUid(), user.getDisplayName(), user.getEmail(), "Sin teléfono");
                        }
                    } else {
                        Toast.makeText(this, "Error auth", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Guardar en Realtime Database
     * @param uid
     * @param username
     * @param email
     * @param phone
     */
    private void saveUserToRealtimeDb(String uid, String username, String email, String phone) {
        User newUser = new User(uid, username, email, phone);

        // Estructura JSON: raiz -> users -> [UID] -> {datos}
        DatabaseReference myRef = database.getReference("users").child(uid);

        myRef.setValue(newUser)
                .addOnSuccessListener(aVoid -> goToMain())
                .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar datos", Toast.LENGTH_SHORT).show());
    }

    /**
     * Navegar a la tienda
     */
    private void goToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}