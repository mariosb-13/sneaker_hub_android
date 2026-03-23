package es.iescarrillo.sneakerhub.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
    private List<Sneaker> fullFilteredList; // Lista "maestra" filtrada por marca/género

    private DatabaseReference dbRef;
    private EditText etSearch; // Usamos EditText según tu XML

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inicializar vistas
        rvSneakers = view.findViewById(R.id.rvSneakers);
        etSearch = view.findViewById(R.id.etSearch); // ID de tu XML

        sneakerList = new ArrayList<>();
        fullFilteredList = new ArrayList<>();

        dbRef = FirebaseDatabase.getInstance().getReference("sneakers");

        setupRecyclerView();

        // 2. Configurar el buscador con TextWatcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filtramos cada vez que el usuario escribe una letra
                filterByName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

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

            if (sneaker.getImages360() != null) {
                args.putStringArrayList("IMAGES_360", new ArrayList<>(sneaker.getImages360()));
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
        // Recogemos los filtros de marca/género que vienen de otras pantallas
        String brandFilter = null;
        String genderFilter = null;
        if (getArguments() != null) {
            brandFilter = getArguments().getString("BRAND");
            genderFilter = getArguments().getString("GENDER");
        }

        final String finalBrand = brandFilter;
        final String finalGender = genderFilter;

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullFilteredList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Sneaker sneaker = data.getValue(Sneaker.class);

                    if (sneaker != null) {
                        // Primero aplicamos los filtros maestros (Marca/Género)
                        boolean matchesBrand = (finalBrand == null || finalBrand.equalsIgnoreCase(sneaker.getBrand()));
                        boolean matchesGender = (finalGender == null || finalGender.equalsIgnoreCase(sneaker.getGender()));

                        if (matchesBrand && matchesGender) {
                            fullFilteredList.add(sneaker);
                        }
                    }
                }
                // Mostramos el resultado aplicando lo que haya escrito en el EditText
                filterByName(etSearch.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para filtrar localmente sin volver a llamar a Firebase
    private void filterByName(String text) {
        sneakerList.clear();

        if (text.isEmpty()) {
            // Si el buscador está vacío, mostramos todo lo que pasó el filtro de marca/género
            sneakerList.addAll(fullFilteredList);
        } else {
            String query = text.toLowerCase().trim();
            for (Sneaker sneaker : fullFilteredList) {
                // Buscamos coincidencia en nombre o marca
                if (sneaker.getName().toLowerCase().contains(query) ||
                        sneaker.getBrand().toLowerCase().contains(query)) {
                    sneakerList.add(sneaker);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}