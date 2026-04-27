package es.iescarrillo.sneakerhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.models.Brand;

public class BrandSideAdapter extends RecyclerView.Adapter<BrandSideAdapter.BrandViewHolder> {

    private List<Brand> brandList;
    private OnBrandClickListener listener;
    private List<String> selectedBrandNames = new ArrayList<>();

    public interface OnBrandClickListener { void onBrandClick(Brand brand); }

    public BrandSideAdapter(List<Brand> brandList, OnBrandClickListener listener) {
        this.brandList = brandList;
        this.listener = listener;
    }

    public void clearSelection() {
        selectedBrandNames.clear();
        notifyDataSetChanged();
    }

    /**
     * Devuelve una lista con los nombres de las marcas seleccionadas.
     * @return
     */
    public ArrayList<String> getSelectedBrands() {
        return new ArrayList<>(selectedBrandNames);
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

        holder.ivPillIcon.setVisibility(View.VISIBLE);
        holder.tvPillName.setVisibility(View.GONE);

        Glide.with(holder.itemView.getContext())
                .load(brand.getIcon())
                .into(holder.ivPillIcon);

        // COMPROBAMOS SI LA MARCA ESTÁ EN LA LISTA
        if (selectedBrandNames.contains(brand.getName())) {
            holder.lyPillRoot.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.color_6));
            holder.ivPillIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
        } else {
            holder.lyPillRoot.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.color_5));
            holder.ivPillIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_6));
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedBrandNames.contains(brand.getName())) {
                selectedBrandNames.remove(brand.getName());
            } else {
                selectedBrandNames.add(brand.getName());
            }
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