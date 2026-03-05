package es.iescarrillo.sneakerhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.models.Sneaker;

public class SneakerAdapter extends RecyclerView.Adapter<SneakerAdapter.SneakerViewHolder> {

    private List<Sneaker> sneakerList;
    private Context context;
    private final OnItemClickListener listener; // <--- NUEVO: Variable para el listener

    // 1. DEFINIMOS LA INTERFAZ (El contrato para el click)
    public interface OnItemClickListener {
        void onItemClick(Sneaker sneaker);
    }

    // 2. ACTUALIZAMOS EL CONSTRUCTOR PARA RECIBIR EL LISTENER
    public SneakerAdapter(List<Sneaker> sneakerList, Context context, OnItemClickListener listener) {
        this.sneakerList = sneakerList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SneakerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sneaker, parent, false);
        return new SneakerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SneakerViewHolder holder, int position) {
        Sneaker sneaker = sneakerList.get(position);

        // Datos visuales
        holder.tvName.setText(sneaker.getName());
        holder.tvBrand.setText(sneaker.getBrand());
        holder.tvPrice.setText(String.format("%.2f €", sneaker.getPrice()));

        // Carga de imagen con Glide
        String imageUrl = sneaker.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.logo_app)
                    .error(R.drawable.logo_app)
                    .into(holder.ivSneaker);
        } else {
            holder.ivSneaker.setImageResource(R.drawable.logo_app);
        }

        // 3. DETECTAR EL CLICK EN TODA LA FILA
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(sneaker); // ¡Avisamos al fragmento!
            }
        });
    }

    @Override
    public int getItemCount() {
        return sneakerList.size();
    }

    public static class SneakerViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBrand, tvPrice;
        ImageView ivSneaker;

        public SneakerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvBrand = itemView.findViewById(R.id.tvBrand);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            ivSneaker = itemView.findViewById(R.id.ivSneaker);
        }
    }
}