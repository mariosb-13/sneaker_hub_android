package es.iescarrillo.sneakerhub.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.adapters.BrandPillAdapter;
import es.iescarrillo.sneakerhub.adapters.TrendingSneakerAdapter;
import es.iescarrillo.sneakerhub.models.Sneaker;
// IMPORTANTE: Asegúrate de tener un modelo Brand o cambiar el import si se llama diferente
import es.iescarrillo.sneakerhub.models.Brand;

public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvTrendingTitle, tvEmptyTrending;
    private RecyclerView rvHomeBrands, rvTrendingSneakers;
    private View layoutLoadingHome;
    private TrendingSneakerAdapter trendingAdapter;
    private List<Sneaker> trendingSneakerList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvTrendingTitle = view.findViewById(R.id.tvTrendingTitle);
        rvHomeBrands = view.findViewById(R.id.rvHomeBrands);
        rvTrendingSneakers = view.findViewById(R.id.rvTrendingSneakers);
        layoutLoadingHome = view.findViewById(R.id.layoutLoadingHome);
        tvEmptyTrending = view.findViewById(R.id.tvEmptyTrending);
        trendingSneakerList = new ArrayList<>();

        if (tvGreeting != null) tvGreeting.setText("");

        setupTrendingRecyclerView();
        loadUserName();
        loadBrandPills();
    }

    private void setupTrendingRecyclerView() {
        if (rvTrendingSneakers == null) return;

        rvTrendingSneakers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        if (getContext() != null) {
            trendingAdapter = new TrendingSneakerAdapter(trendingSneakerList, getContext(), sneaker -> {
                DetailFragment detailFragment = new DetailFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable("sneaker", sneaker);
                detailFragment.setArguments(bundle);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, detailFragment)
                        .addToBackStack(null)
                        .commit();
            });
            rvTrendingSneakers.setAdapter(trendingAdapter);
        }
    }

    private void loadBrandPills() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("brands");
        rvHomeBrands.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Guardamos objetos Brand enteros
                List<Brand> marcasLista = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Brand brand = data.getValue(Brand.class);
                    if (brand != null && brand.getName() != null) {
                        marcasLista.add(brand);
                    }
                }

                // Ordenamos alfabéticamente por nombre
                Collections.sort(marcasLista, (b1, b2) -> b1.getName().compareToIgnoreCase(b2.getName()));

                // Le pasamos la lista de objetos al Adapter. Al hacer clic, le pasamos el nombre para filtrar las zapas
                rvHomeBrands.setAdapter(new BrandPillAdapter(marcasLista, brand -> loadTrendingSneakers(brand.getName())));

                if (!marcasLista.isEmpty()) {
                    loadTrendingSneakers(marcasLista.get(0).getName());
                } else {
                    if (layoutLoadingHome != null) layoutLoadingHome.setVisibility(View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTrendingSneakers(String selectedBrand) {
        if (tvTrendingTitle != null) tvTrendingTitle.setText("Tendencias de " + selectedBrand);

        FirebaseDatabase.getInstance().getReference("sneakers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                trendingSneakerList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Sneaker sneaker = data.getValue(Sneaker.class);
                    if (sneaker != null) {
                        sneaker.setId(data.getKey());
                        if (sneaker.getBrand() != null &&
                                sneaker.getBrand().equalsIgnoreCase(selectedBrand) &&
                                sneaker.isTrending()) {
                            trendingSneakerList.add(sneaker);
                        }
                    }
                }
                trendingAdapter.notifyDataSetChanged();

                if (trendingSneakerList.isEmpty()) {
                    if (rvTrendingSneakers != null) rvTrendingSneakers.setVisibility(View.GONE);
                    if (tvEmptyTrending != null) {
                        tvEmptyTrending.setVisibility(View.VISIBLE);
                        tvEmptyTrending.setText("Aún no tenemos tendencias de " + selectedBrand + " publicadas.");
                    }
                } else {
                    // Si hay zapatillas, mostramos la lista y ocultamos el texto
                    if (rvTrendingSneakers != null) rvTrendingSneakers.setVisibility(View.VISIBLE);
                    if (tvEmptyTrending != null) tvEmptyTrending.setVisibility(View.GONE);
                }

                if (layoutLoadingHome != null) layoutLoadingHome.setVisibility(View.GONE);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadUserName() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (tvGreeting != null) tvGreeting.setText("Buenas Tardes,\nInvitado");
            return;
        }

        FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists() && tvGreeting != null) {
                String name = task.getResult().child("fullName").getValue(String.class);
                String displayName = (name != null) ? name : "Usuario";
                tvGreeting.setText("Buenas Tardes,\n" + displayName);
            } else if (tvGreeting != null) {
                tvGreeting.setText("Buenas Tardes,\nUsuario");
            }
        });
    }
}