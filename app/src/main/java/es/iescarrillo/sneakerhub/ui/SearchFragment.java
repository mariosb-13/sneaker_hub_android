package es.iescarrillo.sneakerhub.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
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
    private List<Sneaker> fullFilteredList;
    private View layoutLoadingSearch;
    private EditText etSearch;

    // CORRECCIÓN 3: Declaramos TODAS las variables de filtro aquí arriba
    private String brandFilter = null;
    private String genderFilter = null;
    private int priceFilter = 10000; // Valor alto por defecto para que muestre todo
    private String sizeFilter = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvSneakers = view.findViewById(R.id.rvSneakers);
        etSearch = view.findViewById(R.id.etSearch);
        layoutLoadingSearch = view.findViewById(R.id.layoutLoadingSearch);

        sneakerList = new ArrayList<>();
        fullFilteredList = new ArrayList<>();
        rvSneakers.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        // Capturamos TODOS los filtros enviados desde MainActivity
        if (getArguments() != null) {
            brandFilter = getArguments().getString("filterBrand");
            genderFilter = getArguments().getString("filterGender");
            priceFilter = getArguments().getInt("filterPrice", 10000);
            sizeFilter = getArguments().getString("filterSize");
        }

        adapter = new SneakerAdapter(sneakerList, requireContext(), sneaker -> {
            DetailFragment detail = new DetailFragment(sneaker);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, detail)
                    .addToBackStack(null).commit();
        });
        rvSneakers.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltrosGlobales(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        ImageView ivMenuSearch = view.findViewById(R.id.ivMenu);
        if (ivMenuSearch != null) {
            ivMenuSearch.setOnClickListener(v -> {
                if (getActivity() != null) {
                    DrawerLayout drawer = getActivity().findViewById(R.id.drawerLayout);
                    if (drawer != null) drawer.openDrawer(GravityCompat.START);
                }
            });
        }

        loadSneakers();
    }

    private void loadSneakers() {
        FirebaseDatabase.getInstance().getReference("sneakers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullFilteredList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Sneaker s = data.getValue(Sneaker.class);
                    if (s != null) {
                        s.setId(data.getKey());
                        fullFilteredList.add(s);
                    }
                }
                aplicarFiltrosGlobales(etSearch.getText().toString());
                if (layoutLoadingSearch != null) layoutLoadingSearch.setVisibility(View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (layoutLoadingSearch != null) layoutLoadingSearch.setVisibility(View.GONE);
            }
        });
    }

    private void aplicarFiltrosGlobales(String text) {
        sneakerList.clear();
        String query = text.toLowerCase().trim();

        for (Sneaker s : fullFilteredList) {
            String sName = (s.getName() != null) ? s.getName().toLowerCase() : "";
            String sBrand = (s.getBrand() != null) ? s.getBrand() : "";
            String sGender = (s.getGender() != null) ? s.getGender() : "";

            // CORRECCIÓN 1: Leemos el precio como double
            double sPrice = s.getPrice();

            // CORRECCIÓN 2: Forma segura de comprobar tallas ignorando si es List<Double> o List<String>
            boolean coincideTalla = true;
            if (sizeFilter != null && !sizeFilter.isEmpty() && s.getSizes() != null) {
                // Pasamos la lista a texto para buscar la talla (ej: "42" dentro de "[38.0, 42.0, 45.0]")
                String stringTallas = s.getSizes().toString();
                coincideTalla = stringTallas.contains(sizeFilter);
            }

            boolean coincideTexto = query.isEmpty() || sName.contains(query) || sBrand.toLowerCase().contains(query);
            boolean coincideMarca = (brandFilter == null || brandFilter.isEmpty() || sBrand.equalsIgnoreCase(brandFilter.trim()));
            boolean coincideGenero = (genderFilter == null || genderFilter.isEmpty() || sGender.equalsIgnoreCase(genderFilter.trim()));

            // Comparamos el double con el int del filtro
            boolean coincidePrecio = (sPrice <= priceFilter);

            if (coincideTexto && coincideMarca && coincideGenero && coincidePrecio && coincideTalla) {
                sneakerList.add(s);
            }
        }
        adapter.notifyDataSetChanged();
    }
}