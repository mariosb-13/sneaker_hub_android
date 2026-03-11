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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import es.iescarrillo.sneakerhub.R;

public class ChangeAddressFragment extends Fragment {

    private EditText etZipCode, etCity, etStreet, etDoor;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_address, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ocultarTopBar(true);

        etZipCode = view.findViewById(R.id.etZipCode);
        etCity = view.findViewById(R.id.etCity);
        etStreet = view.findViewById(R.id.etStreet);
        etDoor = view.findViewById(R.id.etDoor);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            cargarDireccion(); // Carga los datos si ya existen
        }

        // Botón Cancelar
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Botón Guardar
        view.findViewById(R.id.btnSave).setOnClickListener(v -> guardarDireccion());
    }

    private void cargarDireccion() {
        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                // Recuperamos los datos usando los nombres exactos de los atributos de User.java
                String zip = snapshot.child("zipCode").getValue(String.class);
                String city = snapshot.child("city").getValue(String.class);
                String street = snapshot.child("street").getValue(String.class);
                String door = snapshot.child("door").getValue(String.class);

                // Los ponemos en los EditText si no son nulos
                if (zip != null) etZipCode.setText(zip);
                if (city != null) etCity.setText(city);
                if (street != null) etStreet.setText(street);
                if (door != null) etDoor.setText(door);
            }
        });
    }

    private void guardarDireccion() {
        String zip = etZipCode.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String street = etStreet.getText().toString().trim();
        String door = etDoor.getText().toString().trim();

        if (zip.isEmpty() || city.isEmpty() || street.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, rellena los campos principales", Toast.LENGTH_SHORT).show();
            return;
        }

        // Usamos un Map para actualizar SOLO estos campos sin borrar el nombre, la foto o el email
        Map<String, Object> updates = new HashMap<>();
        updates.put("zipCode", zip);
        updates.put("city", city);
        updates.put("street", street);
        updates.put("door", door);

        userRef.updateChildren(updates).addOnSuccessListener(unused -> {
            Toast.makeText(getContext(), "Dirección actualizada", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack(); // Volver atrás tras guardar
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
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