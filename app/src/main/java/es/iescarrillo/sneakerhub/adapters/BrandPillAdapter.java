package es.iescarrillo.sneakerhub.adapters;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import es.iescarrillo.sneakerhub.R;

public class BrandPillAdapter extends RecyclerView.Adapter<BrandPillAdapter.PillViewHolder> {

    private List<String> brandList;
    private OnPillClickListener listener;
    private int selectedPosition = 0;

    public interface OnPillClickListener {
        void onPillClick(String brand);
    }

    public BrandPillAdapter(List<String> brandList, OnPillClickListener listener) {
        this.brandList = brandList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brand_pill, parent, false);
        return new PillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PillViewHolder holder, int position) {
        String brand = brandList.get(position);
        boolean isSelected = (position == selectedPosition);

        // Ponemos el nombre de la marca
        holder.tvPillName.setText(brand);

        // LÓGICA DE EXPANSIÓN Y COLORES
        if (isSelected) {
            // SELECCIONADO: Fondo invertido (color_6), Logo y Texto blancos
            holder.lyPillRoot.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.color_6));
            holder.ivPillIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.white), PorterDuff.Mode.SRC_IN);
            holder.tvPillName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            holder.tvPillName.setVisibility(View.VISIBLE); // Se expande
        } else {
            // NO SELECCIONADO: Fondo gris (color_5), Logo del color de texto (color_6)
            holder.lyPillRoot.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.color_5));
            holder.ivPillIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_6), PorterDuff.Mode.SRC_IN);
            holder.tvPillName.setVisibility(View.GONE); // Se contrae
        }

        // Manejo del Clic
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Refrescamos solo la que se apagó y la que se acaba de encender (animación suave)
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);

            // Avisamos a la Home para que cargue las zapas de esta marca
            listener.onPillClick(brand);
        });

        // Cargar logo de Storage (igual que antes)
        String fileName = brand.replace(" ", "") + ".png";
        StorageReference logoRef = FirebaseStorage.getInstance().getReference().child("brand_logos/" + fileName);

        logoRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(holder.itemView.getContext()).load(uri).into(holder.ivPillIcon);
        }).addOnFailureListener(e -> {
            holder.ivPillIcon.setImageResource(R.drawable.ic_search);
        });
    }

    @Override
    public int getItemCount() {
        return brandList.size();
    }

    public static class PillViewHolder extends RecyclerView.ViewHolder {
        LinearLayout lyPillRoot;
        ImageView ivPillIcon;
        TextView tvPillName;

        public PillViewHolder(@NonNull View itemView) {
            super(itemView);
            lyPillRoot = itemView.findViewById(R.id.lyPillRoot);
            ivPillIcon = itemView.findViewById(R.id.ivPillIcon);
            tvPillName = itemView.findViewById(R.id.tvPillName);
        }
    }
}