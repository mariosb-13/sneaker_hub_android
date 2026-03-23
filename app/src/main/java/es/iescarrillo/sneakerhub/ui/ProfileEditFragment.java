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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import es.iescarrillo.sneakerhub.R;

public class ProfileEditFragment extends Fragment {

    private EditText etName, etEmail, etPhone, etNavPassword, etNavAddress;
    private DatabaseReference userRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View topMenu = getActivity().findViewById(R.id.topBar);
        if (topMenu != null) topMenu.setVisibility(View.GONE);

        etName = view.findViewById(R.id.etEditName);
        etEmail = view.findViewById(R.id.etEditEmail);
        etPhone = view.findViewById(R.id.etEditPhone);
        etNavPassword = view.findViewById(R.id.btnNavPassword);
        etNavAddress = view.findViewById(R.id.btnNavAddress);

        String uid = FirebaseAuth.getInstance().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        // Cargar datos usando fullName
        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                etName.setText(snapshot.child("fullName").getValue(String.class));
                etPhone.setText(snapshot.child("phone").getValue(String.class));
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    etEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                }
            }
        });

        etNavPassword.setOnClickListener(v -> navegarA(new ChangePasswordFragment()));
        etNavAddress.setOnClickListener(v -> navegarA(new ChangeAddressFragment()));
        view.findViewById(R.id.btnSave).setOnClickListener(v -> guardarDatos());
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> volver());
    }

    private void guardarDatos() {
        String nuevoNombre = etName.getText().toString().trim();
        String nuevoTel = etPhone.getText().toString().trim();

        if (nuevoNombre.isEmpty()) {
            Toast.makeText(getContext(), "Nombre obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardar en fullName
        userRef.child("fullName").setValue(nuevoNombre);
        userRef.child("phone").setValue(nuevoTel).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show();
                volver();
            }
        });
    }

    private void navegarA(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null).commit();
    }

    private void volver() {
        if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}