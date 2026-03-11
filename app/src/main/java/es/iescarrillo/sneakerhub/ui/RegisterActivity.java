package es.iescarrillo.sneakerhub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;

import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.models.User;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etUser, etPassword, etConfirmPassword, etPhone;
    private Button btnRegister, btnGoogleRegister;

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        configureGoogleSignIn();

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
                        if (firebaseUser != null) {
                            saveUserToRealtimeDb(firebaseUser.getUid(), username, email, phone);
                            sendWelcomeEmail(email, username);
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

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

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToRealtimeDb(user.getUid(), user.getDisplayName(), user.getEmail(), "Sin teléfono");
                            if (task.getResult().getAdditionalUserInfo().isNewUser()) {
                                sendWelcomeEmail(user.getEmail(), user.getDisplayName());
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error auth", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToRealtimeDb(String uid, String username, String email, String phone) {
        User newUser = new User(uid, username, email, phone);
        DatabaseReference myRef = database.getReference("users").child(uid);
        myRef.setValue(newUser)
                .addOnSuccessListener(aVoid -> goToMain())
                .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar datos", Toast.LENGTH_SHORT).show());
    }

    private void goToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void sendWelcomeEmail(String userEmail, String userName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance("firestore");
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

        String welcomeHtml =
                "<div style=\"background-color: #f9f9f9; padding: 40px 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;\">" +
                        "  <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; border: 1px solid #eeeeee;\">" +
                        "    <div style=\"padding: 40px 20px 20px 20px; text-align: center;\">" +
                        "      <img src=\"https://firebasestorage.googleapis.com/v0/b/sneakerhub-3862d.firebasestorage.app/o/Logo_Negro.png?alt=media&token=ab6c0d4d-7a2a-4692-b0e2-81acf957796d\" alt=\"SneakerHub Logo\" style=\"width: 180px; height: auto; display: block; margin: 0 auto;\">" +
                        "    </div>" +
                        "    <div style=\"padding: 0 50px 40px 50px; text-align: center; color: #111111;\">" +
                        "      <h2 style=\"font-size: 24px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 10px;\">¡Bienvenido a la familia, " + userName + "!</h2>" +
                        "      <p style=\"color: #666666; font-size: 16px; line-height: 1.5; margin-bottom: 30px;\">Ya eres parte de <strong>SneakerHub</strong>. Tu viaje por el mundo de las zapatillas más exclusivas empieza aquí mismo.</p>" +
                        "      <div style=\"margin: 40px 0;\"><a href=\"#\" style=\"background-color: #000000; color: #ffffff; padding: 18px 35px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 15px; display: inline-block; letter-spacing: 1px; text-transform: uppercase;\">Explorar la Tienda</a></div>" +
                        "      <p style=\"font-size: 13px; color: #999999; margin-top: 40px;\">Registrado con: <strong>" + userEmail + "</strong></p>" +
                        "    </div>" +
                        "    <div style=\"background-color: #000000; padding: 30px; text-align: center;\">" +
                        "      <p style=\"color: #ffffff; font-size: 12px; margin: 0; font-weight: bold; letter-spacing: 2px; text-transform: uppercase;\">SneakerHub &copy; 2026</p>" +
                        "    </div>" +
                        "  </div>" +
                        "</div>";

        Map<String, Object> emailData = new HashMap<>();
        emailData.put("to", userEmail);
        Map<String, String> message = new HashMap<>();
        message.put("subject", "¡Bienvenido a SneakerHub! 🎉");
        message.put("html", welcomeHtml);
        emailData.put("message", message);

        db.collection("mail").add(emailData)
                .addOnSuccessListener(doc -> Log.d("EMAIL", "Email enviado: " + doc.getId()))
                .addOnFailureListener(e -> Log.e("EMAIL", "Error: " + e.getMessage()));
    }
}