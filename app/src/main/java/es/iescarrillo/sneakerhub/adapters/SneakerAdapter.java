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

public class SneakerAdapter extends RecyclerView.Adapter<SneakerAdapter.SneakerViewHolder> {

    private List<Sneaker> sneakerList;
    private Context context;
    private final OnItemClickListener listener;

    public interface OnItemClickListener { void onItemClick(Sneaker sneaker); }

    public SneakerAdapter(List<Sneaker> sneakerList, Context context, OnItemClickListener listener) {
        this.sneakerList = sneakerList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SneakerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sneaker, parent, false);
        return new SneakerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SneakerViewHolder holder, int position) {
        Sneaker sneaker = sneakerList.get(position);
        holder.tvName.setText(sneaker.getName());
        holder.tvBrand.setText(sneaker.getBrand());
        Glide.with(context).load(sneaker.getImageUrl()).into(holder.ivSneaker);

        Sneaker.Discount discount = sneaker.getDiscount();
        if (discount != null && discount.isActive() && discount.getPercentage() > 0) {
            double finalPrice = sneaker.getPrice() - (sneaker.getPrice() * (discount.getPercentage() / 100.0));
            holder.tvDiscountBadge.setVisibility(View.VISIBLE);
            holder.tvDiscountBadge.setText("-" + discount.getPercentage() + "%");
            holder.tvOriginalPrice.setVisibility(View.VISIBLE);
            holder.tvOriginalPrice.setText(String.format("%.2f €", sneaker.getPrice()));
            holder.tvOriginalPrice.setPaintFlags(holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvPrice.setText(String.format("%.2f €", finalPrice));
            holder.tvPrice.setTextColor(Color.parseColor("#E53935"));
        } else {
            holder.tvDiscountBadge.setVisibility(View.GONE);
            holder.tvOriginalPrice.setVisibility(View.GONE);
            holder.tvPrice.setText(String.format("%.2f €", sneaker.getPrice()));
            holder.tvPrice.setTextColor(ContextCompat.getColor(context, R.color.color_1));
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(sneaker));
    }

    @Override public int getItemCount() { return sneakerList.size(); }

    public static class SneakerViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBrand, tvPrice, tvDiscountBadge, tvOriginalPrice;
        ImageView ivSneaker;
        public SneakerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvBrand = itemView.findViewById(R.id.tvBrand);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            ivSneaker = itemView.findViewById(R.id.ivSneaker);
            tvDiscountBadge = itemView.findViewById(R.id.tvDiscountBadge);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
        }
    }
}