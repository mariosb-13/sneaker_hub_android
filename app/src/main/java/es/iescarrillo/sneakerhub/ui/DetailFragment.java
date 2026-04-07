package es.iescarrillo.sneakerhub.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.models.Sneaker;

public class DetailFragment extends Fragment {

    private ImageView ivDetailImage;
    private TextView tvDetailBrand, tvDetailName, tvDetailPrice;
    private LinearLayout layoutSizesContainer;
    private Button btnAddToCart;
    private SeekBar seekBar360;

    private TextView selectedSizeView = null;
    private String selectedSize = "";
    private List<String> images360List;

    private Sneaker sneaker;
    private DatabaseReference stockRef;
    private ValueEventListener stockListener;

    public DetailFragment() {}

    public DetailFragment(Sneaker sneaker) {
        this.sneaker = sneaker;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivDetailImage = view.findViewById(R.id.ivDetailImage);
        tvDetailBrand = view.findViewById(R.id.tvDetailBrand);
        tvDetailName = view.findViewById(R.id.tvDetailName);
        tvDetailPrice = view.findViewById(R.id.tvDetailPrice);
        layoutSizesContainer = view.findViewById(R.id.layoutSizesContainer);
        btnAddToCart = view.findViewById(R.id.btnAddToCart);
        seekBar360 = view.findViewById(R.id.seekBar360);

        if (sneaker == null) return;

        tvDetailName.setText(sneaker.getName());
        tvDetailBrand.setText(sneaker.getBrand());
        tvDetailPrice.setText(String.format("%.2f €", sneaker.getPrice()));

        if (sneaker.getImageUrl() != null && isAdded()) {
            Glide.with(this)
                    .load(sneaker.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivDetailImage);
        }

        // --- MAGIA 1: EXTRAER Y ORDENAR TALLAS DE MENOR A MAYOR ---
        if (sneaker.getSizes() != null) {
            List<String> tallasList = new ArrayList<>(sneaker.getSizes().keySet());

            Collections.sort(tallasList, (s1, s2) -> {
                // Cambiamos el _ por . solo para comparar numéricamente (ej. 42_5 -> 42.5)
                String val1 = s1.replace("_", ".");
                String val2 = s2.replace("_", ".");
                try {
                    return Double.compare(Double.parseDouble(val1), Double.parseDouble(val2));
                } catch (NumberFormatException e) {
                    return val1.compareTo(val2); // Por si alguna talla es letra (S, M, L)
                }
            });

            cargarTallas(tallasList);
        }

        images360List = sneaker.getImages360();
        if (images360List != null && !images360List.isEmpty()) {
            seekBar360.setVisibility(View.VISIBLE);
            seekBar360.setMax(images360List.size() - 1);

            for (String url : images360List) {
                Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).preload();
            }

            seekBar360.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && isAdded()) {
                        Glide.with(DetailFragment.this)
                                .load(images360List.get(progress))
                                .placeholder(ivDetailImage.getDrawable())
                                .dontAnimate()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(ivDetailImage);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        } else {
            seekBar360.setVisibility(View.GONE);
        }

        btnAddToCart.setOnClickListener(v -> {
            if (selectedSize.isEmpty()) {
                Toast.makeText(getContext(), "¡Selecciona tu talla!", Toast.LENGTH_SHORT).show();
            } else {
                agregarAlCarritoReal();
            }
        });
    }

    private void cargarTallas(List<String> sizes) {
        layoutSizesContainer.removeAllViews();
        for (String rawSize : sizes) { // rawSize viene como "42_5"

            // --- MAGIA 2: FORMATEAR PARA MOSTRAR ---
            String displaySize = rawSize.replace("_", "."); // displaySize es "42.5"

            TextView tvSize = new TextView(getContext());
            tvSize.setText(displaySize); // Mostramos el número bonito con el punto
            tvSize.setTextSize(16f);
            tvSize.setGravity(Gravity.CENTER);
            tvSize.setBackgroundResource(R.drawable.bg_brand_item);

            tvSize.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.color_5));
            tvSize.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_6));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (60 * getResources().getDisplayMetrics().density),
                    (int) (45 * getResources().getDisplayMetrics().density)
            );
            params.setMargins(0, 0, (int) (12 * getResources().getDisplayMetrics().density), 0);
            tvSize.setLayoutParams(params);

            tvSize.setOnClickListener(v -> {
                if (selectedSizeView != null) {
                    selectedSizeView.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.color_5));
                    selectedSizeView.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_6));
                }
                tvSize.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.color_6));
                tvSize.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                selectedSizeView = tvSize;

                selectedSize = displaySize; // Guardamos "42.5" para que el usuario lo vea así en el carrito

                // Pero a Firebase le seguimos pasando el "42_5" para que no pete al leer el stock
                verificarStockRealTime(rawSize);
            });
            layoutSizesContainer.addView(tvSize);
        }
    }

    private void verificarStockRealTime(String tallaKey) {
        if (stockListener != null && stockRef != null) stockRef.removeEventListener(stockListener);

        stockRef = FirebaseDatabase.getInstance().getReference("sneakers")
                .child(sneaker.getId()).child("sizes").child(tallaKey);

        stockListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer stock = snapshot.getValue(Integer.class);
                if (isAdded() && getContext() != null) {
                    if (stock != null && stock > 0) {
                        btnAddToCart.setEnabled(true);
                        btnAddToCart.setText("Añadir al carrito");
                        btnAddToCart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_6));
                    } else {
                        btnAddToCart.setEnabled(false);
                        btnAddToCart.setText("Agotado");
                        btnAddToCart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_2));
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        stockRef.addValueEventListener(stockListener);
    }

    private void agregarAlCarritoReal() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Inicia sesión para comprar", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("cart").child(user.getUid());

        // Volvemos a cambiar el "." por "_" solo para que la Key del carrito no dé problemas en Firebase
        String cartItemId = sneaker.getId() + "_" + selectedSize.replace(".", "_");

        Map<String, Object> item = new HashMap<>();
        item.put("detalleCartId", cartItemId);
        item.put("productId", sneaker.getId());
        item.put("name", sneaker.getName());
        item.put("price", sneaker.getPrice());
        item.put("imageUrl", sneaker.getImageUrl());
        item.put("tallaElegida", selectedSize); // Aquí se guarda con el punto (ej. "42.5") para el diseño del carrito
        item.put("cantidad", 1);

        cartRef.child(cartItemId).setValue(item).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Talla " + selectedSize + " añadida al carrito", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (stockListener != null && stockRef != null) stockRef.removeEventListener(stockListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            View topBar = getActivity().findViewById(R.id.topBar);
            if (topBar != null) topBar.setVisibility(View.GONE);
        }
    }
}