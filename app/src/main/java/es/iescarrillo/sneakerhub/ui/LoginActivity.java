package es.iescarrillo.sneakerhub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;

import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.models.User;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoogle;
    private TextView tvForgotPassword;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        configureGoogleSignIn();
        initViews();

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

    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Rellena los campos", Toast.LENGTH_SHORT).show(); return;
        }

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> goToMain())
                .addOnFailureListener(e -> Toast.makeText(this, "Error de acceso", Toast.LENGTH_SHORT).show());
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Introduce tu correo"); return;
        }
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Email de recuperación enviado", Toast.LENGTH_LONG).show());
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
                        Toast.makeText(this, "Fallo Google", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        if (authResult.getAdditionalUserInfo().isNewUser()) {
                            sendWelcomeEmail(user.getEmail(), user.getDisplayName());
                        }
                        checkUserInDatabase(user);
                    }
                });
    }

    private void checkUserInDatabase(FirebaseUser firebaseUser) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(firebaseUser.getUid());
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                saveUserToRealtimeDb(firebaseUser);
            } else {
                goToMain();
            }
        });
    }

    private void saveUserToRealtimeDb(FirebaseUser firebaseUser) {
        User newUser = new User(firebaseUser.getUid(), firebaseUser.getDisplayName(), firebaseUser.getEmail(), "Sin teléfono");
        FirebaseDatabase.getInstance().getReference("users").child(firebaseUser.getUid())
                .setValue(newUser).addOnSuccessListener(aVoid -> goToMain());
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void sendWelcomeEmail(String userEmail, String userName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance("firestore");
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false).build();
        db.setFirestoreSettings(settings);

        String welcomeHtml =
                "<div style=\"background-color: #f9f9f9; padding: 40px 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;\">" +
                        "  <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; border: 1px solid #eeeeee;\">" +
                        "    <div style=\"padding: 40px 20px 20px 20px; text-align: center;\">" +
                        "      <img src=\"https://firebasestorage.googleapis.com/v0/b/sneakerhub-3862d.firebasestorage.app/o/Logo_Negro.png?alt=media&token=ab6c0d4d-7a2a-4692-b0e2-81acf957796d\" alt=\"Logo\" style=\"width: 180px; height: auto; display: block; margin: 0 auto;\">" +
                        "    </div>" +
                        "    <div style=\"padding: 0 50px 40px 50px; text-align: center; color: #111111;\">" +
                        "      <h2>¡Bienvenido, " + userName + "!</h2>" +
                        "      <p>Ya eres parte de SneakerHub.</p>" +
                        "    </div>" +
                        "  </div>" +
                        "</div>";

        Map<String, Object> emailData = new HashMap<>();
        emailData.put("to", userEmail);
        Map<String, String> message = new HashMap<>();
        message.put("subject", "¡Bienvenido a SneakerHub! 🎉");
        message.put("html", welcomeHtml);
        emailData.put("message", message);

        db.collection("mail").add(emailData);
    }
}