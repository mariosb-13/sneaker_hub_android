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

import java.util.ArrayList;
import java.util.List;

import es.iescarrillo.sneakerhub.R;

public class DetailFragment extends Fragment {

    private ImageView ivDetailImage;
    private TextView tvDetailBrand, tvDetailName, tvDetailPrice;
    private LinearLayout layoutSizesContainer;
    private Button btnAddToCart;
    private SeekBar seekBar360;

    private TextView selectedSizeView = null;
    private String selectedSize = "";
    private List<String> images360List;

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

        if (getArguments() != null) {
            String name = getArguments().getString("NAME");
            String brand = getArguments().getString("BRAND");
            double price = getArguments().getDouble("PRICE", 0.0);
            String imageUrl = getArguments().getString("IMAGE");
            ArrayList<String> sizes = getArguments().getStringArrayList("SIZES");

            // Llave corregida para tu Firebase
            images360List = getArguments().getStringArrayList("images360");
            if (images360List == null) images360List = getArguments().getStringArrayList("IMAGES_360");

            tvDetailName.setText(name);
            tvDetailBrand.setText(brand);
            tvDetailPrice.setText(String.format("%.2f €", price));

            // 1. Cargamos la principal con prioridad máxima
            if (imageUrl != null && isAdded()) {
                Glide.with(this)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivDetailImage);
            }

            if (sizes != null) cargarTallas(sizes);

            // 2. MAGIA: Empezamos a descargar el 360 por detrás NADA MÁS ENTRAR
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
                            // Al usar placeholder(ivDetailImage.getDrawable()), si la foto nueva
                            // aún no ha bajado, se queda la anterior y no ves el parpadeo
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
        }

        btnAddToCart.setOnClickListener(v -> {
            if (selectedSize.isEmpty()) {
                Toast.makeText(getContext(), "¡Selecciona tu talla!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Añadido: Talla " + selectedSize, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarTallas(List<String> sizes) {
        layoutSizesContainer.removeAllViews();
        for (String size : sizes) {
            TextView tvSize = new TextView(getContext());
            tvSize.setText(size);
            tvSize.setTextSize(16f);
            tvSize.setGravity(Gravity.CENTER);
            tvSize.setBackgroundResource(R.drawable.bg_brand_item);

            tvSize.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.color_5));
            tvSize.setTextColor(ContextCompat.getColor(getContext(), R.color.color_6));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (60 * getResources().getDisplayMetrics().density),
                    (int) (45 * getResources().getDisplayMetrics().density)
            );
            params.setMargins(0, 0, (int) (12 * getResources().getDisplayMetrics().density), 0);
            tvSize.setLayoutParams(params);

            tvSize.setOnClickListener(v -> {
                if (selectedSizeView != null) {
                    selectedSizeView.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.color_5));
                    selectedSizeView.setTextColor(ContextCompat.getColor(getContext(), R.color.color_6));
                }
                tvSize.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.color_6));
                tvSize.setTextColor(ContextCompat.getColor(getContext(), R.color.trending_card_bg));
                selectedSizeView = tvSize;
                selectedSize = size;
            });
            layoutSizesContainer.addView(tvSize);
        }
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