package es.iescarrillo.sneakerhub.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.models.Brand;

public class BrandSideAdapter extends RecyclerView.Adapter<BrandSideAdapter.BrandViewHolder> {

    private List<Brand> brandList;
    private OnBrandClickListener listener;
    private String selectedBrandName = "";

    public interface OnBrandClickListener { void onBrandClick(Brand brand); }

    public BrandSideAdapter(List<Brand> brandList, OnBrandClickListener listener) {
        this.brandList = brandList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BrandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filter_pill, parent, false);
        return new BrandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BrandViewHolder holder, int position) {
        Brand brand = brandList.get(position);

        // ENCENDEMOS LA IMAGEN y apagamos el texto
        holder.ivPillIcon.setVisibility(View.VISIBLE);
        holder.tvPillName.setVisibility(View.GONE);

        // Cargamos el logo con Glide
        Glide.with(holder.itemView.getContext())
                .load(brand.getIcon())
                .into(holder.ivPillIcon);

        if (selectedBrandName.equals(brand.getName())) {
            holder.lyPillRoot.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.color_6));
            holder.ivPillIcon.setColorFilter(Color.WHITE);
        } else {
            holder.lyPillRoot.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.color_5));
            holder.ivPillIcon.clearColorFilter();
        }

        holder.itemView.setOnClickListener(v -> {
            selectedBrandName = brand.getName();
            notifyDataSetChanged();
            listener.onBrandClick(brand);
        });
    }

    @Override
    public int getItemCount() { return brandList.size(); }

    public static class BrandViewHolder extends RecyclerView.ViewHolder {
        LinearLayout lyPillRoot;
        ImageView ivPillIcon;
        android.widget.TextView tvPillName;

        public BrandViewHolder(@NonNull View itemView) {
            super(itemView);
            lyPillRoot = itemView.findViewById(R.id.lyPillRoot);
            ivPillIcon = itemView.findViewById(R.id.ivPillIcon);
            tvPillName = itemView.findViewById(R.id.tvPillName);
        }
    }
}