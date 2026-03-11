package es.iescarrillo.sneakerhub.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import es.iescarrillo.sneakerhub.R;

public class ChangePasswordFragment extends Fragment {

    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Escondemos el menú superior
        ocultarTopBar(true);

        mAuth = FirebaseAuth.getInstance();

        // Instanciamos los campos
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);

        // Botón Cancelar (Volver atrás)
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Botón Guardar
        view.findViewById(R.id.btnSave).setOnClickListener(v -> cambiarContrasena());
    }

    private void cambiarContrasena() {
        String currentPass = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        // Validaciones básicas
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(getContext(), "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(getContext(), "Las nuevas contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null && user.getEmail() != null) {
            // Re-autenticar al usuario por seguridad
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

            user.reauthenticate(credential).addOnSuccessListener(unused -> {
                // Si la contraseña actual es correcta, actualizamos a la nueva
                user.updatePassword(newPass).addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "¡Contraseña actualizada con éxito!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack(); // Volvemos atrás
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                // La contraseña actual que ha puesto está mal
                Toast.makeText(getContext(), "La contraseña actual es incorrecta", Toast.LENGTH_SHORT).show();
            });
        }
    }

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
        ocultarTopBar(true); // Nos aseguramos de que siga oculto
    }
}