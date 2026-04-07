package es.iescarrillo.sneakerhub.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import es.iescarrillo.sneakerhub.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuración Inmersiva de la ventana (pantalla completa sin barras)
        configurarVentanaInmersiva();
        setContentView(R.layout.activity_splash);

        // Animación de Entrada del Logo usando tu ID real
        ImageView ivLogoSplash = findViewById(R.id.ivLogoSplash);
        if (ivLogoSplash != null) {
            iniciarAnimacionProgresiva(ivLogoSplash);
        }

        // Precargar datos antes de entrar
        precargarDatos();
    }

    private void precargarDatos() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("sneakers");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                irAlMainActivity();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                irAlMainActivity();
            }
        });
    }

    private void irAlMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);

        // Suave transición de salida para un toque profesional
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }


    private void configurarVentanaInmersiva() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void iniciarAnimacionProgresiva(View view) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.8f, 1.0f, 0.8f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(1200);
        scaleAnimation.setInterpolator(new DecelerateInterpolator());

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(1200);

        view.startAnimation(fadeIn);
        view.startAnimation(scaleAnimation);
    }
}