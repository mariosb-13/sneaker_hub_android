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

public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvTrendingTitle;
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
        trendingSneakerList = new ArrayList<>();

        setupTrendingRecyclerView();
        loadUserName();
        loadBrandPills();
    }

    private void setupTrendingRecyclerView() {
        rvTrendingSneakers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        trendingAdapter = new TrendingSneakerAdapter(trendingSneakerList, getContext(), sneaker -> {
            // NAVEGACIÓN DIRECTA
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new DetailFragment(sneaker))
                    .addToBackStack(null)
                    .commit();
        });
        rvTrendingSneakers.setAdapter(trendingAdapter);
    }

    private void loadBrandPills() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("brands");
        rvHomeBrands.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> marcasLista = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String brandName = data.child("name").getValue(String.class);
                    if (brandName != null) marcasLista.add(brandName);
                }
                Collections.sort(marcasLista);
                rvHomeBrands.setAdapter(new BrandPillAdapter(marcasLista, brand -> loadTrendingSneakers(brand)));

                if (!marcasLista.isEmpty()) loadTrendingSneakers(marcasLista.get(0));
                else if (layoutLoadingHome != null) layoutLoadingHome.setVisibility(View.GONE);
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
                        // FILTRAR POR MARCA Y TRENDING
                        if (sneaker.getBrand() != null &&
                                sneaker.getBrand().equalsIgnoreCase(selectedBrand) &&
                                sneaker.isTrending()) {
                            trendingSneakerList.add(sneaker);
                        }
                    }
                }
                trendingAdapter.notifyDataSetChanged();
                if (layoutLoadingHome != null) layoutLoadingHome.setVisibility(View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadUserName() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists() && tvGreeting != null) {
                    String name = task.getResult().child("fullName").getValue(String.class);
                    tvGreeting.setText("Buenas Tardes\n" + name + "...");
                }
            });
        }
    }
}