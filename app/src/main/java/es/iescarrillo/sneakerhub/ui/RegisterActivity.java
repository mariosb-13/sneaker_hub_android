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
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build();
        db.setFirestoreSettings(settings);

        String logoUrl = "https://firebasestorage.googleapis.com/v0/b/sneakerhub-3862d.firebasestorage.app/o/SneakerHub.png?alt=media&token=a42e0979-51b2-4a72-ad48-b8a9974ad37a";
        String webUrl = "https://sneaker-hub-web.onrender.com";

        String welcomeHtml =
                "<div style=\"background-color: #f4f4f4; padding: 40px 0; font-family: Arial, sans-serif;\">" +
                        "  <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e0e0e0;\">" +
                        "    <div style=\"padding: 40px 20px; text-align: center;\">" +
                        "      <img src=\"" + logoUrl + "\" alt=\"SneakerHub\" style=\"width: 200px; height: auto;\">" +
                        "    </div>" +
                        "    <div style=\"padding: 0 40px 40px; text-align: center; color: #333333;\">" +
                        "      <h1 style=\"font-size: 26px;\">¡Bienvenido, " + userName + "!</h1>" +
                        "      <p>Gracias por unirte a SneakerHub.</p>" +
                        "      <div style=\"margin-top: 25px;\">" +
                        "        <a href=\"" + webUrl + "\" style=\"background-color: #000000; color: #ffffff; padding: 16px 32px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;\">IR A LA TIENDA</a>" +
                        "      </div>" +
                        "    </div>" +
                        "  </div>" +
                        "</div>";

        Map<String, Object> emailData = new HashMap<>();
        emailData.put("to", userEmail);
        Map<String, String> message = new HashMap<>();
        message.put("subject", "¡Bienvenido a la familia SneakerHub! 🎉");
        message.put("html", welcomeHtml);
        emailData.put("message", message);

        db.collection("mail").add(emailData);
    }
}