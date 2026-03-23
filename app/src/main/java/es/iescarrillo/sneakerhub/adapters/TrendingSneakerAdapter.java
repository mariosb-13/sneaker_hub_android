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

public class TrendingSneakerAdapter extends RecyclerView.Adapter<TrendingSneakerAdapter.TrendingViewHolder> {

    private List<Sneaker> sneakerList;
    private Context context;
    private OnSneakerClickListener listener;

    public interface OnSneakerClickListener {
        void onSneakerClick(Sneaker sneaker);
    }

    public TrendingSneakerAdapter(List<Sneaker> sneakerList, Context context, OnSneakerClickListener listener) {
        this.sneakerList = sneakerList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trending_sneaker, parent, false);
        return new TrendingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrendingViewHolder holder, int position) {
        Sneaker sneaker = sneakerList.get(position);

        holder.tvName.setText(sneaker.getName());
        holder.tvBrand.setText(sneaker.getBrand());
        holder.tvPrice.setText(String.format("%.2f€", sneaker.getPrice()));

        Glide.with(context)
                .load(sneaker.getImageUrl())
                .placeholder(R.drawable.nike) // Pon una imagen por defecto de carga
                .into(holder.ivSneaker);

        holder.itemView.setOnClickListener(v -> listener.onSneakerClick(sneaker));
    }

    @Override
    public int getItemCount() {
        return sneakerList.size();
    }

    public static class TrendingViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSneaker;
        TextView tvName, tvBrand, tvPrice;

        public TrendingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSneaker = itemView.findViewById(R.id.ivTrendingSneaker);
            tvName = itemView.findViewById(R.id.tvTrendingName);
            tvBrand = itemView.findViewById(R.id.tvTrendingBrand);
            tvPrice = itemView.findViewById(R.id.tvTrendingPrice);
        }
    }
}