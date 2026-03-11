package es.iescarrillo.sneakerhub.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

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

    // Método para calcular el saludo según la hora del dispositivo
    private String obtenerSaludo() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        if (timeOfDay >= 6 && timeOfDay < 12) {
            return "Buenos Días";
        } else if (timeOfDay >= 12 && timeOfDay < 20) {
            return "Buenas Tardes";
        } else {
            return "Buenas Noches";
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadUserName() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        String saludo = obtenerSaludo();

        if (currentUser != null) {
            // USUARIO LOGUEADO
            String uid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    DataSnapshot snapshot = task.getResult();

                    // Sacamos el nombre del JSON de la base de datos
                    String username = snapshot.child("username").getValue(String.class);

                    if (username != null && !username.isEmpty()) {
                        // Actualizamos el texto con el saludo dinámico
                        tvGreeting.setText(saludo + "\n" + username + "...");
                    }
                } else {
                    // Si falla algo o no hay internet, dejamos el saludo por defecto sin nombre
                    tvGreeting.setText(saludo + "...");
                }
            });
        } else {
            // INVITADO (no hay usuario logueado)
            tvGreeting.setText(saludo + "\nInvitado...");
        }
    }
}