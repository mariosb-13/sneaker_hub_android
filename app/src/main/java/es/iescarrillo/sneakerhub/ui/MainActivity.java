package es.iescarrillo.sneakerhub.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import es.iescarrillo.sneakerhub.R;

public class MainActivity extends AppCompatActivity {

    // --- VARIABLES GLOBALES ---
    private DrawerLayout drawerLayout;

    // Iconos del menú inferior
    private ImageView navHome, navSearch, navCart, navProfile;

    // Variables del Menú Lateral (SideSheet)
    private LinearLayout btnSideNike, btnSideAdidas;
    private TextView btnToggleMen, btnToggleWomen;

    // Estado del filtro (Por defecto "Man")
    private String currentGenderFilter = "Man";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. INICIALIZAR DRAWER Y TOP BAR
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView ivMenu = findViewById(R.id.ivMenu);

        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // 2. INICIALIZAR NAVEGACIÓN INFERIOR
        navHome = findViewById(R.id.navHome);
        navSearch = findViewById(R.id.navSearch);
        navCart = findViewById(R.id.navCart);
        navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> loadFragment(new HomeFragment(), navHome));
        navSearch.setOnClickListener(v -> loadFragment(new SearchFragment(), navSearch));
        navCart.setOnClickListener(v -> loadFragment(new CartFragment(), navCart));
        navProfile.setOnClickListener(v -> loadFragment(new ProfileFragment(), navProfile));

        // 3. INICIALIZAR LÓGICA DEL SIDE SHEET (Filtros)
        setupSideSheetLogic();

        // 4. CARGA INICIAL
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), navHome);
        }


        //subirDatosAutomaticos();

    }

    /**
     * Lógica del menu lateral
     */
    private void setupSideSheetLogic() {
        btnToggleMen = findViewById(R.id.btnToggleMen);
        btnToggleWomen = findViewById(R.id.btnToggleWomen);
        btnSideNike = findViewById(R.id.btnSideNike);
        btnSideAdidas = findViewById(R.id.btnSideAdidas);

        // Listeners de Género
        if (btnToggleMen != null) btnToggleMen.setOnClickListener(v -> updateGenderVisuals("Man"));
        if (btnToggleWomen != null) btnToggleWomen.setOnClickListener(v -> updateGenderVisuals("Woman"));

        // Listeners de Marca
        if (btnSideNike != null) btnSideNike.setOnClickListener(v -> navigateToFilter("Nike"));
        if (btnSideAdidas != null) btnSideAdidas.setOnClickListener(v -> navigateToFilter("Adidas"));

        // Estado inicial visual
        if (btnToggleMen != null) updateGenderVisuals("Man");
    }

    /**
     * Actualiza la visualización de los botones de género
     * @param gender
     */
    private void updateGenderVisuals(String gender) {
        currentGenderFilter = gender;
        if (btnToggleMen == null || btnToggleWomen == null) return;

        // Guardamos padding para que no se deforme el botón
        int pLeft = btnToggleMen.getPaddingLeft();
        int pTop = btnToggleMen.getPaddingTop();
        int pRight = btnToggleMen.getPaddingRight();
        int pBottom = btnToggleMen.getPaddingBottom();

        int colorActive = ContextCompat.getColor(this, R.color.color_4); // Negro
        int colorInactive = ContextCompat.getColor(this, R.color.color_3); // Gris

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

        // Restauramos padding
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

    /**
     * Lógica de carga de fragmentos
     * @param fragment
     * @param activeIcon
     */
    private void loadFragment(Fragment fragment, ImageView activeIcon) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
        updateIconColors(activeIcon);

        View topBar = findViewById(R.id.topBar);
        View fragmentContainer = findViewById(R.id.fragmentContainer);
        float scale = getResources().getDisplayMetrics().density;
        int padding80dp = (int) (80 * scale + 0.5f);

        if (topBar != null && fragmentContainer != null) {
            if (fragment instanceof SearchFragment) {
                // MODO BUSCADOR: Sin barra flotante, padding 0
                topBar.setVisibility(View.GONE);
                fragmentContainer.setPadding(0, 0, 0, padding80dp);

                // Barra Blanca + Iconos Negros
                configurarBarra(Color.WHITE, true);
            } else {
                // MODO NORMAL: Con barra flotante
                topBar.setVisibility(View.VISIBLE);
                fragmentContainer.setPadding(0, padding80dp, 0, padding80dp);

                // Barra Negra + Iconos Blancos
                configurarBarra(Color.BLACK, false);
            }
        }
    }

    /**
     * Actualiza el color de los iconos
     * @param activeIcon
     */
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

    // Método para cambiar el color de la barra de estado de forma segura
    private void configurarBarra(int colorFondo, boolean iconosNegros) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(colorFondo);

        if (drawerLayout != null) {
            drawerLayout.setStatusBarBackgroundColor(colorFondo);
        }

        View decorView = window.getDecorView();
        if (iconosNegros) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            decorView.setSystemUiVisibility(0);
        }
    }

    private void subirDatosAutomaticos() {
        android.widget.Toast.makeText(this, "Cargando en Realtime DB...", android.widget.Toast.LENGTH_SHORT).show();

        // ADIDAS
        subirA_Realtime("AdidasForumBuckleLowBadBunnyLastForum.png", "Adidas Forum Bad Bunny Black", "Adidas", "Man", 160.0);
        subirA_Realtime("adidasForumLowBadBunnyPinkEasterEgg 2.png", "Adidas Forum Bad Bunny Pink", "Adidas", "Woman", 160.0);

        // NIKE
        subirA_Realtime("DunkLowSBThePowerpuffGirlsBubbles 1.png", "Nike SB Dunk Bubbles", "Nike", "Woman", 135.0);
        subirA_Realtime("Nike-Dunk-Low-Scrap-Black-Gum.png", "Nike Dunk Scrap", "Nike", "Man", 110.0);
        subirA_Realtime("Nike-SB-Dunk-Low-Civilist.png", "Nike SB Dunk Civilist", "Nike", "Man", 120.0);
        subirA_Realtime("NikeSBDunkLowProQSNeckface 1.png", "Nike SB Dunk Neckface", "Nike", "Man", 120.0);
        subirA_Realtime("lote-34 1.png", "Nike Dunk Off-White 34", "Nike", "Man", 180.0);
        subirA_Realtime("j1-travis-medium-olive 1.png", "Jordan 1 Travis Olive", "Nike", "Woman", 150.0);
        subirA_Realtime("velvet-brown-travis.png", "Jordan 1 Travis Velvet", "Nike", "Man", 150.0);

        // NEW BALANCE
        subirA_Realtime("NewBalance550WhiteGreen.png", "NB 550 White Green", "New Balance", "Man", 110.0);
    }

    private void subirA_Realtime(String archivo, String nombre, String marca, String genero, double precio) {
        // 1. Obtener URL de Storage
        com.google.firebase.storage.FirebaseStorage.getInstance().getReference().child(archivo)
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    // 2. Crear objeto Map (o usar clase Sneaker)
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("name", nombre);
                    map.put("brand", marca);
                    map.put("gender", genero);
                    map.put("price", precio);
                    map.put("imageUrl", uri.toString());
                    // Tallas
                    map.put("sizes", java.util.Arrays.asList("38", "39", "40", "41", "42", "43", "44"));

                    // 3. Subir a Realtime Database (push() crea un ID único)
                    FirebaseDatabase.getInstance().getReference("sneakers")
                            .push()
                            .setValue(map);
                });
    }

    // --- FUNCIÓN AUXILIAR (IGUAL QUE ANTES) ---
    // --- FUNCIÓN AUXILIAR CON TALLAS ---
    private void subirZapatillaConFoto(String nombreArchivo, String nombre, String marca, String genero, double precio) {
        com.google.firebase.storage.FirebaseStorage storage = com.google.firebase.storage.FirebaseStorage.getInstance();
        com.google.firebase.storage.StorageReference storageRef = storage.getReference().child(nombreArchivo);

        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String urlReal = uri.toString();

            // GENERAMOS TALLAS (Simulación: del 39 al 45)
            // En una app real, esto vendría de un inventario
            java.util.List<String> tallas = java.util.Arrays.asList("39", "40", "40.5", "41", "42", "42.5", "43", "44", "45");

            java.util.Map<String, Object> zap = new java.util.HashMap<>();
            zap.put("name", nombre);
            zap.put("brand", marca);
            zap.put("gender", genero);
            zap.put("price", precio);
            zap.put("imageUrl", urlReal);
            zap.put("sizes", tallas); // <--- AÑADIMOS LAS TALLAS A FIREBASE

            FirebaseFirestore.getInstance().collection("sneakers")
                    .add(zap)
                    .addOnSuccessListener(r -> android.util.Log.d("AUTO_LOAD", "Subida OK: " + nombre));
        }).addOnFailureListener(e -> {
            android.util.Log.e("AUTO_LOAD", "FALLO con foto: " + nombreArchivo, e);
        });
    }
}