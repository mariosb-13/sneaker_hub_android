package es.iescarrillo.sneakerhub.ui;

import android.graphics.Paint;
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
    private TextView tvDetailBrand, tvDetailName, tvDetailPrice, tvDetailOriginalPrice, tvDetailDiscountBadge;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            sneaker = (Sneaker) getArguments().getSerializable("sneaker");
        }

        if (sneaker == null) return;

        ivDetailImage = view.findViewById(R.id.ivDetailImage);
        tvDetailBrand = view.findViewById(R.id.tvDetailBrand);
        tvDetailName = view.findViewById(R.id.tvDetailName);
        tvDetailPrice = view.findViewById(R.id.tvDetailPrice);
        tvDetailOriginalPrice = view.findViewById(R.id.tvDetailOriginalPrice);
        tvDetailDiscountBadge = view.findViewById(R.id.tvDetailDiscountBadge);
        layoutSizesContainer = view.findViewById(R.id.layoutSizesContainer);
        btnAddToCart = view.findViewById(R.id.btnAddToCart);
        seekBar360 = view.findViewById(R.id.seekBar360);

        tvDetailName.setText(sneaker.getName());
        tvDetailBrand.setText(sneaker.getBrand());

        // --- LÓGICA DE DESCUENTOS VISUAL EN EL DETALLE ---
        double originalPrice = sneaker.getPrice();
        double finalPrice = originalPrice;

        if (sneaker.getDiscount() != null && sneaker.getDiscount().isActive() && sneaker.getDiscount().getPercentage() > 0) {
            int pct = sneaker.getDiscount().getPercentage();
            finalPrice = originalPrice - (originalPrice * (pct / 100.0));

            tvDetailDiscountBadge.setVisibility(View.VISIBLE);
            tvDetailDiscountBadge.setText("-" + pct + "%");

            tvDetailOriginalPrice.setVisibility(View.VISIBLE);
            tvDetailOriginalPrice.setText(String.format("%.2f €", originalPrice));
            tvDetailOriginalPrice.setPaintFlags(tvDetailOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            tvDetailPrice.setText(String.format("%.2f €", finalPrice));
            tvDetailPrice.setTextColor(android.graphics.Color.parseColor("#E53935"));
        } else {
            tvDetailDiscountBadge.setVisibility(View.GONE);
            tvDetailOriginalPrice.setVisibility(View.GONE);
            tvDetailPrice.setText(String.format("%.2f €", originalPrice));
            tvDetailPrice.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_6));
        }

        if (sneaker.getImageUrl() != null && isAdded()) {
            Glide.with(this).load(sneaker.getImageUrl()).diskCacheStrategy(DiskCacheStrategy.ALL).into(ivDetailImage);
        }

        if (sneaker.getSizes() != null) {
            List<String> tallasList = new ArrayList<>(sneaker.getSizes().keySet());
            Collections.sort(tallasList, (s1, s2) -> {
                String val1 = s1.replace("_", ".");
                String val2 = s2.replace("_", ".");
                try {
                    return Double.compare(Double.parseDouble(val1), Double.parseDouble(val2));
                } catch (NumberFormatException e) {
                    return val1.compareTo(val2);
                }
            });
            cargarTallas(tallasList);
        }

        // 360 Logic
        images360List = new ArrayList<>();
        Object rawImages = sneaker.getImages360();
        if (rawImages != null) {
            if (rawImages instanceof List) {
                for (Object item : (List<?>) rawImages) {
                    if (item != null && !item.toString().trim().isEmpty()) images360List.add(item.toString());
                }
            } else if (rawImages instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) rawImages;
                List<String> keys = new ArrayList<>();
                for (Object k : map.keySet()) keys.add(k.toString());
                Collections.sort(keys, (a, b) -> {
                    try { return Integer.compare(Integer.parseInt(a), Integer.parseInt(b)); }
                    catch (NumberFormatException e) { return a.compareTo(b); }
                });
                for (String key : keys) {
                    Object val = map.get(key);
                    if (val != null && !val.toString().trim().isEmpty()) images360List.add(val.toString());
                }
            }
        }

        if (!images360List.isEmpty()) {
            seekBar360.setVisibility(View.VISIBLE);
            seekBar360.setMax(images360List.size() - 1);
            for (String url : images360List) {
                Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).preload();
            }
            seekBar360.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && isAdded()) {
                        Glide.with(DetailFragment.this).load(images360List.get(progress))
                                .placeholder(ivDetailImage.getDrawable()).dontAnimate()
                                .diskCacheStrategy(DiskCacheStrategy.ALL).into(ivDetailImage);
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
        for (String rawSize : sizes) {
            String displaySize = rawSize.replace("_", ".");
            TextView tvSize = new TextView(getContext());
            tvSize.setText(displaySize);
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
                selectedSize = displaySize;
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
        String cartItemId = sneaker.getId() + "_" + selectedSize.replace(".", "_");

        // --- LÓGICA DE DESCUENTOS PARA EL CARRITO ---
        double originalPrice = sneaker.getPrice();
        double finalPrice = originalPrice;
        int discountPct = 0;

        if (sneaker.getDiscount() != null && sneaker.getDiscount().isActive()) {
            discountPct = sneaker.getDiscount().getPercentage();
            finalPrice = originalPrice - (originalPrice * (discountPct / 100.0));
        }

        Map<String, Object> item = new HashMap<>();
        item.put("detalleCartId", cartItemId);
        item.put("productId", sneaker.getId());
        item.put("name", sneaker.getName());
        item.put("price", finalPrice);
        item.put("originalPrice", originalPrice); // Guardamos el original para tacharlo en el carrito
        item.put("discountPct", discountPct);     // Guardamos el porcentaje para el badge en el carrito
        item.put("imageUrl", sneaker.getImageUrl());
        item.put("tallaElegida", selectedSize);
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