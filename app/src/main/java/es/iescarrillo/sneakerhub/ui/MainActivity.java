package es.iescarrillo.sneakerhub.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.adapters.BrandSideAdapter;
import es.iescarrillo.sneakerhub.adapters.GenericSideAdapter;
import es.iescarrillo.sneakerhub.models.Brand;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private View topBar, bottomBar;
    private ImageView navHome, navSearch, navCart, navProfile;

    private RecyclerView rvSideBrands, rvSideSizes, rvSidePrices;
    private TextView btnToggleMen, btnToggleWomen;

    private String selectedGender = "Man";
    private String selectedBrand = "";
    private String selectedSize = "";
    private int selectedMaxPrice = 10000; // Por defecto un precio alto

    private List<Brand> brandList = new ArrayList<>();
    private BrandSideAdapter brandAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        topBar = findViewById(R.id.topBar);
        bottomBar = findViewById(R.id.bottomBar);
        View mainContent = findViewById(R.id.mainContent);

        // --- INSETS (Márgenes) ---
        ViewCompat.setOnApplyWindowInsetsListener(mainContent, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (bottomBar != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottomBar.getLayoutParams();
                lp.bottomMargin = (int) (24 * getResources().getDisplayMetrics().density) + systemBars.bottom;
                bottomBar.setLayoutParams(lp);
            }
            return insets;
        });

        // --- INICIALIZAR VISTAS DEL MENÚ INFERIOR ---
        navHome = findViewById(R.id.navHome);
        navSearch = findViewById(R.id.navSearch);
        navCart = findViewById(R.id.navCart);
        navProfile = findViewById(R.id.navProfile);

        // --- FILTROS SIDE SHEET ---
        btnToggleMen = findViewById(R.id.btnToggleMen);
        btnToggleWomen = findViewById(R.id.btnToggleWomen);
        rvSideBrands = findViewById(R.id.rvSideBrands);
        rvSideSizes = findViewById(R.id.rvSideSizes);
        rvSidePrices = findViewById(R.id.rvSidePrices);

        configurarBotonesGenero();

        // Tallas: 4 columnas
        rvSideSizes.setLayoutManager(new GridLayoutManager(this, 4));
        List<String> tallas = Arrays.asList("38", "39", "40", "41", "42", "43", "44", "45", "46");
        rvSideSizes.setAdapter(new GenericSideAdapter(tallas, t -> selectedSize = t));

        // Precios: 3 columnas (Queda más equilibrado que 2)
        rvSidePrices.setLayoutManager(new GridLayoutManager(this, 3));
        List<String> precios = Arrays.asList("100€", "150€", "200€", "300€", "500€", "1000€");
        rvSidePrices.setAdapter(new GenericSideAdapter(precios, p -> selectedMaxPrice = Integer.parseInt(p.replace("€", ""))));

        // Marcas: 2 columnas
        rvSideBrands.setLayoutManager(new GridLayoutManager(this, 2));
        brandAdapter = new BrandSideAdapter(brandList, b -> selectedBrand = b.getName());
        rvSideBrands.setAdapter(brandAdapter);
        cargarMarcas();

        // Botón Aplicar Filtros
        findViewById(R.id.btnApplyFilters).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Bundle bundle = new Bundle();
            bundle.putString("filterBrand", selectedBrand);
            bundle.putString("filterGender", selectedGender);
            bundle.putInt("filterPrice", selectedMaxPrice);
            bundle.putString("filterSize", selectedSize);

            SearchFragment fragment = new SearchFragment();
            fragment.setArguments(bundle);
            // Pasamos navSearch para que se ilumine la lupa al filtrar
            loadFragment(fragment, navSearch);
        });

        // Botón Menú Global (Hamburguesa)
        ImageView ivMenuGlobal = findViewById(R.id.ivMenuGlobal);
        if (ivMenuGlobal != null) {
            ivMenuGlobal.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // --- CONFIGURACIÓN DE CLICKS DE NAVEGACIÓN ---
        navHome.setOnClickListener(v -> loadFragment(new HomeFragment(), navHome));
        navSearch.setOnClickListener(v -> loadFragment(new SearchFragment(), navSearch));
        navCart.setOnClickListener(v -> loadFragment(new CartFragment(), navCart));
        navProfile.setOnClickListener(v -> loadFragment(new ProfileFragment(), navProfile));

        // Cargar inicio por defecto
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), navHome);
        }
    }

    private void cargarMarcas() {
        FirebaseDatabase.getInstance().getReference("brands").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                brandList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Brand b = ds.getValue(Brand.class);
                    if (b != null) { b.setId(ds.getKey()); brandList.add(b); }
                }
                Collections.sort(brandList, (b1, b2) -> b1.getName().compareToIgnoreCase(b2.getName()));
                brandAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void configurarBotonesGenero() {
        btnToggleMen.setOnClickListener(v -> { selectedGender = "Man"; actualizarEstiloBotones(btnToggleMen, btnToggleWomen); });
        btnToggleWomen.setOnClickListener(v -> { selectedGender = "Woman"; actualizarEstiloBotones(btnToggleWomen, btnToggleMen); });
    }

    private void actualizarEstiloBotones(TextView sel, TextView desel) {
        sel.setBackgroundResource(R.drawable.bg_brand_item);
        sel.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
        sel.setTextColor(ContextCompat.getColor(this, R.color.color_6));
        sel.setElevation(2f);
        desel.setBackground(null);
        desel.setTextColor(ContextCompat.getColor(this, R.color.color_2));
        desel.setElevation(0f);
    }

    // --- LÓGICA DE NAVEGACIÓN RESTAURADA COMPLETAMENTE ---
    public void loadFragment(Fragment fragment, ImageView activeIcon) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();

        if (activeIcon != null) updateIconColors(activeIcon);

        if (topBar != null) {
            if (fragment instanceof SearchFragment ||
                    fragment instanceof DetailFragment ||
                    fragment instanceof SuccessFragment ||
                    fragment instanceof OrderDetailFragment) {
                topBar.setVisibility(View.GONE);
            } else {
                topBar.setVisibility(View.VISIBLE);
            }
        }
        configurarBarraEstado(true);
    }

    private void updateIconColors(ImageView activeIcon) {
        int colorActive = ContextCompat.getColor(this, R.color.color_6);
        int colorInactive = ContextCompat.getColor(this, R.color.color_2);
        navHome.setColorFilter(colorInactive);
        navSearch.setColorFilter(colorInactive);
        navCart.setColorFilter(colorInactive);
        navProfile.setColorFilter(colorInactive);
        activeIcon.setColorFilter(colorActive);
    }

    private void configurarBarraEstado(boolean iconosNegros) {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(iconosNegros);
        }
    }
}