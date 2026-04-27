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
import java.util.List;
import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.models.Brand;

public class BrandPillAdapter extends RecyclerView.Adapter<BrandPillAdapter.PillViewHolder> {

    private List<Brand> brandList;
    private OnPillClickListener listener;
    private int selectedPosition = 0;

    public interface OnPillClickListener {
        void onPillClick(Brand brand);
    }

    public BrandPillAdapter(List<Brand> brandList, OnPillClickListener listener) {
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
        Brand brand = brandList.get(position);
        boolean isSelected = (position == selectedPosition);

        holder.tvPillName.setText(brand.getName());

        // LÓGICA DE COLORES
        if (isSelected) {
            holder.lyPillRoot.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.color_6));
            holder.ivPillIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.white), PorterDuff.Mode.SRC_IN);
            holder.tvPillName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            holder.tvPillName.setVisibility(View.VISIBLE);
        } else {
            holder.lyPillRoot.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.color_5));
            holder.ivPillIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_6), PorterDuff.Mode.SRC_IN);
            holder.tvPillName.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            listener.onPillClick(brand);
        });

        if (brand.getIcon() != null && !brand.getIcon().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(brand.getIcon())
                    .into(holder.ivPillIcon);
        } else {
            holder.ivPillIcon.setImageResource(R.drawable.ic_search);
        }
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