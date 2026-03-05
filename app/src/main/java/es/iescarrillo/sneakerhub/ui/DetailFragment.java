package es.iescarrillo.sneakerhub.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;

import java.util.ArrayList; // Importante
import java.util.List;

import es.iescarrillo.sneakerhub.R;

public class DetailFragment extends Fragment {

    private ImageView ivDetailImage;
    private TextView tvDetailBrand, tvDetailName, tvDetailPrice;
    private LinearLayout layoutSizesContainer; // El contenedor horizontal

    private String name, brand, imageUrl;
    private double price;
    private List<String> sizes; // La lista que viene de Firebase

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        // 1. RECIBIR DATOS
        if (getArguments() != null) {
            name = getArguments().getString("NAME");
            brand = getArguments().getString("BRAND");
            price = getArguments().getDouble("PRICE");
            imageUrl = getArguments().getString("IMAGE");

            // Recibimos la lista de tallas
            sizes = getArguments().getStringArrayList("SIZES");
        }

        // 2. VINCULAR VISTAS
        ivDetailImage = view.findViewById(R.id.ivDetailImage);
        tvDetailBrand = view.findViewById(R.id.tvDetailBrand);
        tvDetailName = view.findViewById(R.id.tvDetailName);
        tvDetailPrice = view.findViewById(R.id.tvDetailPrice);
        layoutSizesContainer = view.findViewById(R.id.layoutSizesContainer);
        View btnAddToCart = view.findViewById(R.id.btnAddToCart);

        // 3. PINTAR DATOS BÁSICOS
        tvDetailBrand.setText(brand);
        tvDetailName.setText(name);
        tvDetailPrice.setText(String.format("%.2f €", price));
        if (getContext() != null) Glide.with(getContext()).load(imageUrl).into(ivDetailImage);

        // 4. GENERAR TALLAS DINÁMICAS (Aquí está la magia)
        pintarTallas();

        // 5. BOTÓN
        btnAddToCart.setOnClickListener(v ->
                Toast.makeText(getContext(), "Añadido: " + name, Toast.LENGTH_SHORT).show()
        );

        return view;
    }

    private void pintarTallas() {
        // Limpiamos lo que hubiera antes (ej: los placeholders del XML)
        layoutSizesContainer.removeAllViews();

        if (sizes == null || sizes.isEmpty()) {
            TextView error = new TextView(getContext());
            error.setText("Agotado");
            layoutSizesContainer.addView(error);
            return;
        }

        // Bucle: Por cada talla en la lista, creamos un TextView
        for (String talla : sizes) {
            TextView tvTalla = new TextView(getContext());
            tvTalla.setText("EU " + talla);
            tvTalla.setTextSize(14f);
            tvTalla.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_6));
            tvTalla.setPadding(40, 20, 40, 20);

            // Usamos un fondo de sistema o uno tuyo propio
            tvTalla.setBackgroundResource(android.R.drawable.editbox_background_normal);

            // Márgenes para separarlos
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(10, 0, 10, 0);
            tvTalla.setLayoutParams(params);

            tvTalla.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Talla " + talla, Toast.LENGTH_SHORT).show()
            );

            // Añadimos al contenedor
            layoutSizesContainer.addView(tvTalla);
        }
    }
}