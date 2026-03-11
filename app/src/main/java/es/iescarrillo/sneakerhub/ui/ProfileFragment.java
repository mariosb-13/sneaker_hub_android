package es.iescarrillo.sneakerhub.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
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

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail;
    private TextView btnEditProfile, btnOrderHistory, btnSettings, btnLogout;
    private ImageView ivAvatar;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private StorageReference storageRef;

    // Lanzador para seleccionar la foto de la galería
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
        // Inflamos el layout fragment_profile.xml
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // OCULTAR BARRA SUPERIOR
        ocultarTopBar(true);

        // INICIALIZAR FIREBASE Y VISTAS
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

        // COMPROBAR MODO: LOGUEADO vs INVITADO
        if (currentUser != null) {
            // USUARIO LOGUEADO
            tvProfileEmail.setText(currentUser.getEmail());
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            cargarDatosUsuario();

            btnEditProfile.setVisibility(View.VISIBLE);
            btnOrderHistory.setVisibility(View.VISIBLE);
            btnLogout.setText("Cerrar Sesión");
            btnLogout.setOnClickListener(v -> cerrarSesion());

            // Permitir cambiar foto solo si está logueado
            ivAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                imagePickerLauncher.launch(intent);
            });

        } else {
            // MODO INVITADO
            tvProfileName.setText("¡Hola, Invitado!");
            tvProfileEmail.setText("Regístrate para ver tus pedidos y más.");
            ivAvatar.setImageResource(R.drawable.ic_person);

            // Ocultamos botones que no tienen sentido para invitados
            btnEditProfile.setVisibility(View.GONE);
            btnOrderHistory.setVisibility(View.GONE);

            btnLogout.setText("Iniciar Sesión / Registrarse");
            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), WelcomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });

            // Quitamos el listener del avatar para invitados
            ivAvatar.setOnClickListener(null);
        }

        // 4. LISTENERS COMUNES
        btnEditProfile.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ProfileEditFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void cargarDatosUsuario() {
        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                // Nombre
                String name = snapshot.child("username").getValue(String.class);
                if (name != null) tvProfileName.setText(name);

                // Foto de perfil con Glide
                String photoUrl = snapshot.child("profileImageUrl").getValue(String.class);
                if (photoUrl != null && !photoUrl.isEmpty() && isAdded()) {
                    Glide.with(this)
                            .load(photoUrl)
                            .circleCrop()
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    Log.e("GLIDE_ERROR", "Error al cargar imagen: " + e.getMessage());
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    return false;
                                }
                            })
                            .into(ivAvatar);
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show());
    }

    private void subirFotoAFirebase(Uri imageUri) {
        if (mAuth.getCurrentUser() == null) return;

        Toast.makeText(getContext(), "Subiendo imagen...", Toast.LENGTH_SHORT).show();
        String uid = mAuth.getCurrentUser().getUid();
        StorageReference fileRef = storageRef.child(uid + ".jpg");

        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                // Actualizamos el nuevo atributo en la base de datos
                userRef.child("profileImageUrl").setValue(downloadUrl)
                        .addOnSuccessListener(unused -> {
                            if (isAdded()) {
                                Glide.with(this).load(downloadUrl).circleCrop().into(ivAvatar);
                            }
                            Toast.makeText(getContext(), "Foto actualizada", Toast.LENGTH_SHORT).show();
                        });
            });
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Fallo al subir: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void cerrarSesion() {
        // Firebase SignOut
        mAuth.signOut();

        // Google SignOut
        if (getContext() != null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getContext(), gso);
            googleSignInClient.signOut();
        }

        // Volver al Welcome y limpiar pilas
        Intent intent = new Intent(getActivity(), WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // Lógica para mostrar/ocultar la barra superior global
    private void ocultarTopBar(boolean ocultar) {
        if (getActivity() != null) {
            View topBar = getActivity().findViewById(R.id.topBar);
            if (topBar != null) {
                topBar.setVisibility(ocultar ? View.GONE : View.VISIBLE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ocultarTopBar(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        //ocultarTopBar(false);
    }
}