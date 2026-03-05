package es.iescarrillo.sneakerhub.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.adapters.SneakerAdapter;
import es.iescarrillo.sneakerhub.models.Sneaker;

public class SearchFragment extends Fragment {

    private RecyclerView rvSneakers;
    private SneakerAdapter adapter;
    private List<Sneaker> sneakerList;

    // CAMBIO: Ahora usamos DatabaseReference
    private DatabaseReference dbRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvSneakers = view.findViewById(R.id.rvSneakers);
        sneakerList = new ArrayList<>();

        // 1. Inicializar Realtime Database
        dbRef = FirebaseDatabase.getInstance().getReference("sneakers");

        setupRecyclerView();

        // Botón Menú
        ImageView ivMenuSearch = view.findViewById(R.id.ivMenu);
        if (ivMenuSearch != null) {
            ivMenuSearch.setOnClickListener(v -> {
                if (getActivity() != null) {
                    DrawerLayout drawer = getActivity().findViewById(R.id.drawerLayout);
                    if (drawer != null) drawer.openDrawer(GravityCompat.START);
                }
            });
        }

        loadSneakersFromRealtime();
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        rvSneakers.setLayoutManager(layoutManager);

        adapter = new SneakerAdapter(sneakerList, requireContext(), sneaker -> {
            DetailFragment detailFragment = new DetailFragment();
            Bundle args = new Bundle();
            args.putString("NAME", sneaker.getName());
            args.putString("BRAND", sneaker.getBrand());
            args.putDouble("PRICE", sneaker.getPrice());
            args.putString("IMAGE", sneaker.getImageUrl());

            if (sneaker.getSizes() != null) {
                args.putStringArrayList("SIZES", new ArrayList<>(sneaker.getSizes()));
            } else {
                args.putStringArrayList("SIZES", new ArrayList<>());
            }

            detailFragment.setArguments(args);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
        rvSneakers.setAdapter(adapter);
    }

    private void loadSneakersFromRealtime() {
        // Recogemos los filtros
        String brandFilter = null;
        String genderFilter = null;
        if (getArguments() != null) {
            brandFilter = getArguments().getString("BRAND");
            genderFilter = getArguments().getString("GENDER");
        }

        // Variables finales para usar dentro del listener
        final String finalBrand = brandFilter;
        final String finalGender = genderFilter;

        // 2. LEER DATOS DE REALTIME (ValueEventListener)
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sneakerList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    // Convertir JSON a Objeto Sneaker
                    Sneaker sneaker = data.getValue(Sneaker.class);

                    if (sneaker != null) {
                        // --- FILTRADO MANUAL (JAVA) ---
                        boolean matchesBrand = (finalBrand == null || finalBrand.equals(sneaker.getBrand()));
                        boolean matchesGender = (finalGender == null || finalGender.equals(sneaker.getGender()));

                        // Si cumple AMBOS filtros, lo añadimos
                        if (matchesBrand && matchesGender) {
                            sneakerList.add(sneaker);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}