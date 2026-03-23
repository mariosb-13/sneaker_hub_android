package es.iescarrillo.sneakerhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import es.iescarrillo.sneakerhub.R;

public class BrandSideAdapter extends RecyclerView.Adapter<BrandSideAdapter.BrandViewHolder> {

    private List<String> brandList;
    private OnBrandClickListener listener;

    public interface OnBrandClickListener {
        void onBrandClick(String brand);
    }

    public BrandSideAdapter(List<String> brandList, OnBrandClickListener listener) {
        this.brandList = brandList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BrandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_side_brand, parent, false);
        return new BrandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BrandViewHolder holder, int position) {
        String brand = brandList.get(position);
        holder.tvBrandName.setText(brand);
        holder.itemView.setOnClickListener(v -> listener.onBrandClick(brand));

        // Aplicamos la magia del color dinámico al ImageView
        int tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_6);
        holder.ivBrandIcon.setColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN);

        String fileName = brand.replace(" ", "") + ".png";

        StorageReference logoRef = FirebaseStorage.getInstance().getReference().child("brand_logos/" + fileName);

        // 3. Descargamos con Glide
        logoRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(holder.itemView.getContext())
                    .load(uri)
                    .into(holder.ivBrandIcon);
        }).addOnFailureListener(e -> {
            // Si falla, icono por defecto
            holder.ivBrandIcon.setImageResource(R.drawable.ic_search);
        });
    }

    @Override
    public int getItemCount() {
        return brandList.size();
    }

    public static class BrandViewHolder extends RecyclerView.ViewHolder {
        TextView tvBrandName;
        ImageView ivBrandIcon;

        public BrandViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBrandName = itemView.findViewById(R.id.tvBrandName);
            ivBrandIcon = itemView.findViewById(R.id.ivBrandIcon);
        }
    }
}