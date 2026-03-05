package es.iescarrillo.sneakerhub.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import es.iescarrillo.sneakerhub.R;

public class HomeFragment extends Fragment {

    private TextView tvGreeting;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflamos el layout del fragmento
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvGreeting = view.findViewById(R.id.tvGreeting);

        loadUserName();
    }

    private void loadUserName() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    DataSnapshot snapshot = task.getResult();

                    // Sacamos el nombre del JSON de la base de datos
                    String username = snapshot.child("username").getValue(String.class);

                    if (username != null && !username.isEmpty()) {
                        // Actualizamos el texto
                        tvGreeting.setText("Buenas Tardes\n" + username + "...");
                    }
                } else {
                    // Si falla algo o no hay internet, dejamos un mensaje por defecto
                    tvGreeting.setText("Buenas Tardes");
                }
            });
        }
    }
}