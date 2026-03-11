package es.iescarrillo.sneakerhub.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import es.iescarrillo.sneakerhub.R;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView navHome, navSearch, navCart, navProfile;
    private LinearLayout btnSideNike, btnSideAdidas;
    private TextView btnToggleMen, btnToggleWomen;
    private String currentGenderFilter = "Man";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. ACTIVAR MODO EDGE-TO-EDGE (Pantalla completa real)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        View mainContent = findViewById(R.id.mainContent);
        View topBar = findViewById(R.id.topBar);
        View bottomBar = findViewById(R.id.bottomBar);

        // 2. MATEMÁTICAS: Calcular la altura de la batería y los botones de abajo
        ViewCompat.setOnApplyWindowInsetsListener(mainContent, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Empujar el TopBar hacia abajo (16dp originales + altura de la barra de estado)
            if (topBar != null) {
                ViewGroup.MarginLayoutParams topParams = (ViewGroup.MarginLayoutParams) topBar.getLayoutParams();
                topParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.top;
                topBar.setLayoutParams(topParams);
            }

            // Empujar el BottomBar hacia arriba (24dp originales + altura de los botones del móvil)
            if (bottomBar != null) {
                ViewGroup.MarginLayoutParams bottomParams = (ViewGroup.MarginLayoutParams) bottomBar.getLayoutParams();
                bottomParams.bottomMargin = (int) (24 * getResources().getDisplayMetrics().density) + systemBars.bottom;
                bottomBar.setLayoutParams(bottomParams);
            }

            return insets;
        });

        // 3. INICIALIZAR VISTAS
        ImageView ivMenu = findViewById(R.id.ivMenu);
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        navHome = findViewById(R.id.navHome);
        navSearch = findViewById(R.id.navSearch);
        navCart = findViewById(R.id.navCart);
        navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> loadFragment(new HomeFragment(), navHome));
        navSearch.setOnClickListener(v -> loadFragment(new SearchFragment(), navSearch));
        navCart.setOnClickListener(v -> loadFragment(new CartFragment(), navCart));
        navProfile.setOnClickListener(v -> loadFragment(new ProfileFragment(), navProfile));

        setupSideSheetLogic();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), navHome);
        }
    }

    private void setupSideSheetLogic() {
        btnToggleMen = findViewById(R.id.btnToggleMen);
        btnToggleWomen = findViewById(R.id.btnToggleWomen);
        btnSideNike = findViewById(R.id.btnSideNike);
        btnSideAdidas = findViewById(R.id.btnSideAdidas);

        if (btnToggleMen != null) btnToggleMen.setOnClickListener(v -> updateGenderVisuals("Man"));
        if (btnToggleWomen != null) btnToggleWomen.setOnClickListener(v -> updateGenderVisuals("Woman"));
        if (btnSideNike != null) btnSideNike.setOnClickListener(v -> navigateToFilter("Nike"));
        if (btnSideAdidas != null) btnSideAdidas.setOnClickListener(v -> navigateToFilter("Adidas"));

        if (btnToggleMen != null) updateGenderVisuals("Man");
    }

    private void updateGenderVisuals(String gender) {
        currentGenderFilter = gender;
        if (btnToggleMen == null || btnToggleWomen == null) return;

        int pLeft = btnToggleMen.getPaddingLeft();
        int pTop = btnToggleMen.getPaddingTop();
        int pRight = btnToggleMen.getPaddingRight();
        int pBottom = btnToggleMen.getPaddingBottom();

        int colorActive = ContextCompat.getColor(this, R.color.color_4);
        int colorInactive = ContextCompat.getColor(this, R.color.color_3);

        if (gender.equals("Man")) {
            btnToggleMen.setBackgroundResource(R.drawable.bg_brand_item);
            btnToggleMen.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
            btnToggleMen.setTextColor(colorActive);
            btnToggleMen.setElevation(8f);

            btnToggleWomen.setBackground(null);
            btnToggleWomen.setTextColor(colorInactive);
            btnToggleWomen.setElevation(0f);
        } else {
            btnToggleWomen.setBackgroundResource(R.drawable.bg_brand_item);
            btnToggleWomen.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
            btnToggleWomen.setTextColor(colorActive);
            btnToggleWomen.setElevation(8f);

            btnToggleMen.setBackground(null);
            btnToggleMen.setTextColor(colorInactive);
            btnToggleMen.setElevation(0f);
        }

        btnToggleMen.setPadding(pLeft, pTop, pRight, pBottom);
        btnToggleWomen.setPadding(pLeft, pTop, pRight, pBottom);
    }

    private void navigateToFilter(String brand) {
        drawerLayout.closeDrawer(GravityCompat.START);
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString("BRAND", brand);
        args.putString("GENDER", currentGenderFilter);
        fragment.setArguments(args);
        loadFragment(fragment, navSearch);
        Toast.makeText(this, "Filtrando: " + brand + " (" + currentGenderFilter + ")", Toast.LENGTH_SHORT).show();
    }

    private void loadFragment(Fragment fragment, ImageView activeIcon) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
        updateIconColors(activeIcon);

        View topBar = findViewById(R.id.topBar);
        // ¡Cero paddings mágicos para el contenedor! Ahora la pantalla es 100% libre.

        if (topBar != null) {
            if (fragment instanceof SearchFragment) {
                topBar.setVisibility(View.GONE);
                configurarBarra(Color.TRANSPARENT, true);
            } else {
                topBar.setVisibility(View.VISIBLE);
                configurarBarra(Color.TRANSPARENT, false);
            }
        }
    }

    private void updateIconColors(ImageView activeIcon) {
        int colorActive = ContextCompat.getColor(this, R.color.color_1);
        int colorInactive = ContextCompat.getColor(this, R.color.color_3);

        navHome.setColorFilter(colorInactive);
        navSearch.setColorFilter(colorInactive);
        navCart.setColorFilter(colorInactive);
        navProfile.setColorFilter(colorInactive);

        if (activeIcon != null) {
            activeIcon.setColorFilter(colorActive);
        }
    }

    private void configurarBarra(int colorFondo, boolean iconosNegros) {
        Window window = getWindow();
        // Hacemos las barras del sistema transparentes para que el fondo se vea por debajo
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        View decorView = window.getDecorView();
        if (iconosNegros) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            decorView.setSystemUiVisibility(0);
        }
    }
}