package es.iescarrillo.sneakerhub.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

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
    private LinearLayout llNoResults;

    // AHORA LAS MARCAS Y TALLAS SON LISTAS, NO STRINGS SIMPLES
    private List<String> brandFilters = new ArrayList<>();
    private List<String> sizeFilters = new ArrayList<>();
    private String genderFilter = null;
    private int priceFilter = 10000;

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
        llNoResults = view.findViewById(R.id.llNoResults);

        sneakerList = new ArrayList<>();
        fullFilteredList = new ArrayList<>();
        rvSneakers.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        // CAPTURAMOS LOS FILTROS CORRECTAMENTE
        if (getArguments() != null) {
            ArrayList<String> brands = getArguments().getStringArrayList("filterBrands");
            if (brands != null) brandFilters = brands;

            ArrayList<String> sizes = getArguments().getStringArrayList("filterSizes");
            if (sizes != null) sizeFilters = sizes;

            genderFilter = getArguments().getString("filterGender");
            priceFilter = getArguments().getInt("filterPrice", 10000);
        }

        adapter = new SneakerAdapter(sneakerList, requireContext(), sneaker -> {
            DetailFragment detail = new DetailFragment();
            // Aseguramos que se pase la zapatilla al detalle
            Bundle bundle = new Bundle();
            bundle.putSerializable("sneaker", sneaker);
            detail.setArguments(bundle);

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

            // Calcular precio final (con descuento si lo hay)
            double sPrice = s.getPrice();
            if (s.getDiscount() != null && s.getDiscount().isActive()) {
                sPrice = sPrice * (1 - (s.getDiscount().getPercentage() / 100.0));
            }

            // 1. TALLAS: Comprobamos si la zapatilla tiene ALGUNA de las tallas marcadas
            boolean coincideTalla = true;
            if (!sizeFilters.isEmpty()) {
                coincideTalla = false;
                if (s.getSizes() != null) {
                    for (String size : sizeFilters) {
                        String safeSize = size.replace(".", "_");
                        // Si tiene stock de cualquiera de las tallas seleccionadas, nos vale
                        if (s.getSizes().containsKey(safeSize) && s.getSizes().get(safeSize) != null && s.getSizes().get(safeSize) > 0) {
                            coincideTalla = true;
                            break;
                        }
                    }
                }
            }

            // 2. MARCAS: Comprobamos si es de ALGUNA de las marcas marcadas
            boolean coincideMarca = true;
            if (!brandFilters.isEmpty()) {
                coincideMarca = false;
                for (String b : brandFilters) {
                    if (sBrand.equalsIgnoreCase(b.trim())) {
                        coincideMarca = true;
                        break;
                    }
                }
            }

            // Filtros clásicos
            boolean coincideTexto = query.isEmpty() || sName.contains(query) || sBrand.toLowerCase().contains(query);
            boolean coincideGenero = (genderFilter == null || genderFilter.isEmpty() || sGender.equalsIgnoreCase(genderFilter.trim()));
            boolean coincidePrecio = (sPrice <= priceFilter);

            // Si cumple todas las condiciones, se añade
            if (coincideTexto && coincideMarca && coincideGenero && coincidePrecio && coincideTalla) {
                sneakerList.add(s);
            }
        }

        adapter.notifyDataSetChanged();

        // Controlar pantalla vacía
        if (llNoResults != null) {
            if (sneakerList.isEmpty()) {
                llNoResults.setVisibility(View.VISIBLE);
                rvSneakers.setVisibility(View.GONE);
            } else {
                llNoResults.setVisibility(View.GONE);
                rvSneakers.setVisibility(View.VISIBLE);
            }
        }
    }
}