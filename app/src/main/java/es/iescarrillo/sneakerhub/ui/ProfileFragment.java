package es.iescarrillo.sneakerhub.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.ui.WelcomeActivity; // Asegúrate de tener este import

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail;
    private TextView btnEditProfile, btnOrderHistory, btnSettings, btnLogout;
    private ImageView ivAvatar;
    private View layoutLoadingProfile;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private StorageReference storageRef;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    subirFotoAFirebase(imageUri);
                }
            }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ocultarTopBar(true);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        storageRef = FirebaseStorage.getInstance().getReference("profile_pics");

        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnOrderHistory = view.findViewById(R.id.btnOrderHistory);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnLogout = view.findViewById(R.id.btnLogout);
        layoutLoadingProfile = view.findViewById(R.id.layoutLoadingProfile);

        if (currentUser != null) {
            tvProfileEmail.setText(currentUser.getEmail());
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            cargarDatosUsuario();

            btnEditProfile.setVisibility(View.VISIBLE);
            btnOrderHistory.setVisibility(View.VISIBLE);
            btnLogout.setText("Cerrar Sesión");
            btnLogout.setOnClickListener(v -> cerrarSesion());

            ivAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                imagePickerLauncher.launch(intent);
            });

            // --- NAVEGACIÓN AL HISTORIAL DE PEDIDOS ---
            btnOrderHistory.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragmentContainer, new OrderHistoryFragment())
                        .addToBackStack(null)
                        .commit();
            });

        } else {
            tvProfileName.setText("¡Hola, Invitado!");
            tvProfileEmail.setText("Regístrate para ver tus pedidos y más.");
            ivAvatar.setImageResource(R.drawable.ic_person);
            btnEditProfile.setVisibility(View.GONE);
            btnOrderHistory.setVisibility(View.GONE);
            btnLogout.setText("Iniciar Sesión / Registrarse");
            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), WelcomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
            ivAvatar.setOnClickListener(null);

            layoutLoadingProfile.setVisibility(View.GONE);
        }

        btnEditProfile.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ProfileEditFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void cargarDatosUsuario() {
        userRef.get().addOnSuccessListener(snapshot -> {
            if (isAdded() && snapshot.exists()) {
                String name = snapshot.child("fullName").getValue(String.class);
                if (name != null) tvProfileName.setText(name);

                String photoUrl = snapshot.child("profileImageUrl").getValue(String.class);
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(this).load(photoUrl).circleCrop().into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_person);
                }
            }
            if (layoutLoadingProfile != null) layoutLoadingProfile.setVisibility(View.GONE);
        }).addOnFailureListener(e -> {
            if (layoutLoadingProfile != null) layoutLoadingProfile.setVisibility(View.GONE);
        });
    }

    private void subirFotoAFirebase(Uri imageUri) {
        if (mAuth.getCurrentUser() == null) return;

        Toast.makeText(getContext(), "Subiendo imagen...", Toast.LENGTH_SHORT).show();
        String uid = mAuth.getCurrentUser().getUid();
        StorageReference fileRef = storageRef.child(uid);

        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                userRef.child("profileImageUrl").setValue(downloadUrl)
                        .addOnSuccessListener(unused -> {
                            if (isAdded()) Glide.with(this).load(downloadUrl).circleCrop().into(ivAvatar);
                            Toast.makeText(getContext(), "Foto actualizada", Toast.LENGTH_SHORT).show();
                        });
            });
        }).addOnFailureListener(e -> {
            if (isAdded()) Toast.makeText(getContext(), "Fallo al subir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void cerrarSesion() {
        mAuth.signOut();
        if (getContext() != null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getContext(), gso);
            googleSignInClient.signOut();
        }
        Intent intent = new Intent(getActivity(), WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void ocultarTopBar(boolean ocultar) {
        if (getActivity() != null) {
            View topBar = getActivity().findViewById(R.id.topBar);
            if (topBar != null) topBar.setVisibility(ocultar ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ocultarTopBar(true);
    }
}