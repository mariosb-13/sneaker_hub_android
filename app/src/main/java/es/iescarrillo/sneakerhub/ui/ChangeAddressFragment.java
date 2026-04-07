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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;
import es.iescarrillo.sneakerhub.R;

public class ChangeAddressFragment extends Fragment {

    private EditText etZipCode, etCity, etStreet, etDoor;
    private DatabaseReference userRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_address, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etZipCode = view.findViewById(R.id.etZipCode);
        etCity = view.findViewById(R.id.etCity);
        etStreet = view.findViewById(R.id.etStreet);
        etDoor = view.findViewById(R.id.etDoor);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            cargarDireccion();
        }

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> getParentFragmentManager().popBackStack());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> guardarDireccion());
    }

    private void cargarDireccion() {
        userRef.child("address").get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                etZipCode.setText(snapshot.child("zipCode").getValue(String.class));
                etCity.setText(snapshot.child("city").getValue(String.class));
                etStreet.setText(snapshot.child("street").getValue(String.class));
                etDoor.setText(snapshot.child("door").getValue(String.class));
            }
        });
    }

    private void guardarDireccion() {
        String zip = etZipCode.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String street = etStreet.getText().toString().trim();
        String door = etDoor.getText().toString().trim();

        if (zip.isEmpty() || city.isEmpty() || street.isEmpty()) {
            Toast.makeText(getContext(), "Rellena los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("zipCode", zip);
        addressMap.put("city", city);
        addressMap.put("street", street);
        addressMap.put("door", door);

        userRef.child("address").setValue(addressMap).addOnSuccessListener(unused -> {
            Toast.makeText(getContext(), "Dirección guardada", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        });
    }
}