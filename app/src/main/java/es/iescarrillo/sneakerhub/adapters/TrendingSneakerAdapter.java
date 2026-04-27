package es.iescarrillo.sneakerhub.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.models.Sneaker;

public class TrendingSneakerAdapter extends RecyclerView.Adapter<TrendingSneakerAdapter.TrendingViewHolder> {

    private List<Sneaker> sneakerList;
    private Context context;
    private OnSneakerClickListener listener;

    public interface OnSneakerClickListener { void onSneakerClick(Sneaker sneaker); }

    public TrendingSneakerAdapter(List<Sneaker> sneakerList, Context context, OnSneakerClickListener listener) {
        this.sneakerList = sneakerList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending_sneaker, parent, false);
        return new TrendingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrendingViewHolder holder, int position) {
        Sneaker sneaker = sneakerList.get(position);
        holder.tvName.setText(sneaker.getName());
        holder.tvBrand.setText(sneaker.getBrand());

        Glide.with(context).load(sneaker.getImageUrl()).into(holder.ivSneaker);

        Sneaker.Discount discount = sneaker.getDiscount();
        if (discount != null && discount.isActive() && discount.getPercentage() > 0) {
            double finalPrice = sneaker.getPrice() * (1 - (discount.getPercentage() / 100.0));
            if (holder.tvDiscountBadge != null) {
                holder.tvDiscountBadge.setVisibility(View.VISIBLE);
                holder.tvDiscountBadge.setText("-" + discount.getPercentage() + "%");
            }
            if (holder.tvOriginalPrice != null) {
                holder.tvOriginalPrice.setVisibility(View.VISIBLE);
                holder.tvOriginalPrice.setText(String.format("%.2f €", sneaker.getPrice()));
                holder.tvOriginalPrice.setPaintFlags(holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
            holder.tvPrice.setText(String.format("%.2f €", finalPrice));

            // Si hay descuento, usamos tu color_7 (rojo) para que destaque
            holder.tvPrice.setTextColor(ContextCompat.getColor(context, R.color.color_7));
        } else {
            if (holder.tvDiscountBadge != null) holder.tvDiscountBadge.setVisibility(View.GONE);
            if (holder.tvOriginalPrice != null) holder.tvOriginalPrice.setVisibility(View.GONE);

            holder.tvPrice.setText(String.format("%.2f €", sneaker.getPrice()));

            holder.tvPrice.setTextColor(ContextCompat.getColor(context, R.color.color_6));
        }

        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onSneakerClick(sneaker); });
    }

    @Override public int getItemCount() { return sneakerList.size(); }

    public static class TrendingViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSneaker;
        TextView tvName, tvBrand, tvPrice, tvDiscountBadge, tvOriginalPrice;
        public TrendingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSneaker = itemView.findViewById(R.id.ivTrendingSneaker);
            tvName = itemView.findViewById(R.id.tvTrendingName);
            tvBrand = itemView.findViewById(R.id.tvTrendingBrand);
            tvPrice = itemView.findViewById(R.id.tvTrendingPrice);
            tvDiscountBadge = itemView.findViewById(R.id.tvTrendingDiscountBadge);
            tvOriginalPrice = itemView.findViewById(R.id.tvTrendingOriginalPrice);
        }
    }
}