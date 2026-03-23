package es.iescarrillo.sneakerhub.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.adapters.BrandPillAdapter;
import es.iescarrillo.sneakerhub.adapters.TrendingSneakerAdapter; // Asegúrate de que este nombre coincida
import es.iescarrillo.sneakerhub.models.Sneaker;

public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvTrendingTitle;
    private RecyclerView rvHomeBrands, rvTrendingSneakers;

    // Variables para el carrusel de tendencias
    private TrendingSneakerAdapter trendingAdapter;
    private List<Sneaker> trendingSneakerList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Enlazamos las vistas del XML
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvTrendingTitle = view.findViewById(R.id.tvTrendingTitle);
        rvHomeBrands = view.findViewById(R.id.rvHomeBrands);
        rvTrendingSneakers = view.findViewById(R.id.rvTrendingSneakers);

        trendingSneakerList = new ArrayList<>();

        // Configuramos el RecyclerView de tendencias (carrusel horizontal)
        setupTrendingRecyclerView();

        // Cargamos los datos
        loadUserName();
        loadBrandPills();
    }

    private void setupTrendingRecyclerView() {
        // Configuramos la lista para que haga scroll horizontal
        rvTrendingSneakers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Inicializamos el nuevo adaptador premium
        trendingAdapter = new TrendingSneakerAdapter(trendingSneakerList, getContext(), sneaker -> {

            // Lógica al hacer clic en una zapatilla del carrusel: Ir a Detalles
            DetailFragment detailFragment = new DetailFragment();
            Bundle args = new Bundle();
            args.putString("NAME", sneaker.getName());
            args.putString("BRAND", sneaker.getBrand());
            args.putDouble("PRICE", sneaker.getPrice());
            args.putString("IMAGE", sneaker.getImageUrl());

            if (sneaker.getSizes() != null) {
                args.putStringArrayList("SIZES", new ArrayList<>(sneaker.getSizes()));
            }
            if (sneaker.getImages360() != null) {
                args.putStringArrayList("IMAGES_360", new ArrayList<>(sneaker.getImages360()));
            }

            detailFragment.setArguments(args);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });

        rvTrendingSneakers.setAdapter(trendingAdapter);
    }

    private void loadBrandPills() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("brands");
        rvHomeBrands.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Usamos addValueEventListener para que si el Admin añade una marca, salga sola en la app
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> marcasLista = new ArrayList<>();

                // Recorremos las marcas de tu nuevo JSON
                for (DataSnapshot data : snapshot.getChildren()) {
                    // Solo cogemos el "name" (ej: "Adidas", "Nike")
                    String brandName = data.child("name").getValue(String.class);

                    if (brandName != null && !brandName.trim().isEmpty()) {
                        marcasLista.add(brandName);
                    }
                }

                // Las ordenamos de la A a la Z (opcional, pero queda más pro)
                Collections.sort(marcasLista);

                // Se lo pasamos a tu adaptador que ya tenías hecho
                BrandPillAdapter adapter = new BrandPillAdapter(marcasLista, brand -> {
                    // Cuando el usuario toca una píldora, cargamos sus zapatillas en tendencia
                    loadTrendingSneakers(brand);
                });

                rvHomeBrands.setAdapter(adapter);

                // Por defecto, nada más entrar en la Home, cargamos las tendencias de la primera marca
                if (!marcasLista.isEmpty()) {
                    loadTrendingSneakers(marcasLista.get(0));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error cargando marcas", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadTrendingSneakers(String selectedBrand) {
        // Actualizamos el título dinámicamente
        tvTrendingTitle.setText("Tendencias de " + selectedBrand);

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("sneakers");

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                trendingSneakerList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Sneaker sneaker = data.getValue(Sneaker.class);

                    // Verificamos que no sea nulo, coincida la marca y además tenga isTrending a true
                    if (sneaker != null &&
                            sneaker.getBrand() != null &&
                            sneaker.getBrand().equalsIgnoreCase(selectedBrand)) {

                        // IMPORTANTE: Asegúrate de tener isTrending en Sneaker.java y en Firebase
                        if (sneaker.isTrending()) {
                            trendingSneakerList.add(sneaker);
                        }
                    }
                }

                // Si la marca seleccionada no tiene tendencias activas, cambiamos el texto
                if (trendingSneakerList.isEmpty()) {
                    tvTrendingTitle.setText("Próximamente tendencias de " + selectedBrand);
                }

                // Avisamos al adaptador de que hay nuevos datos para dibujar
                trendingAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String obtenerSaludo() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        if (timeOfDay >= 6 && timeOfDay < 12) return "Buenos Días";
        else if (timeOfDay >= 12 && timeOfDay < 20) return "Buenas Tardes";
        else return "Buenas Noches";
    }

    @SuppressLint("SetTextI18n")
    private void loadUserName() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String saludo = obtenerSaludo();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    DataSnapshot snapshot = task.getResult();
                    String fullName = snapshot.child("fullName").getValue(String.class);

                    if (fullName != null && !fullName.isEmpty()) {
                        tvGreeting.setText(saludo + "\n" + fullName + "...");
                    } else {
                        tvGreeting.setText(saludo + "...");
                    }
                } else {
                    tvGreeting.setText(saludo + "...");
                }
            });
        } else {
            tvGreeting.setText(saludo + "\nInvitado...");
        }
    }
}