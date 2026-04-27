package es.iescarrillo.sneakerhub.adapters;

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
import es.iescarrillo.sneakerhub.models.DetalleCarrito;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<DetalleCarrito> cartList;
    private OnCartItemChangeListener listener;

    public interface OnCartItemChangeListener {
        void onItemChanged(DetalleCarrito item);
        void onItemDeleted(DetalleCarrito item);
    }

    public CartAdapter(List<DetalleCarrito> cartList, OnCartItemChangeListener listener) {
        this.cartList = cartList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        DetalleCarrito item = cartList.get(position);
        holder.tvName.setText(item.getName());

        // Lógica de descuento
        double price = item.getPrice();
        double originalPrice = item.getOriginalPrice();

        if (originalPrice > price && originalPrice > 0) {
            // 1. Mostrar precio original tachado
            holder.tvOriginalPrice.setVisibility(View.VISIBLE);
            holder.tvOriginalPrice.setText(String.format("%.2f €", originalPrice));
            holder.tvOriginalPrice.setPaintFlags(holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            // 2. Pintar precio final de rojo
            holder.tvPrice.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_7));

            // 3. NUEVO: Calcular y mostrar el Badge de % de descuento sobre la imagen
            int pct = (int) Math.round(((originalPrice - price) / originalPrice) * 100);
            holder.tvDiscountBadge.setVisibility(View.VISIBLE);
            holder.tvDiscountBadge.setText("-" + pct + "%");

        } else {
            // Si no hay descuento, ocultamos los extras y dejamos el color normal
            holder.tvOriginalPrice.setVisibility(View.GONE);
            holder.tvDiscountBadge.setVisibility(View.GONE); // Ocultamos el badge
            holder.tvPrice.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_6));
        }

        holder.tvPrice.setText(String.format("%.2f €", price));
        holder.tvSize.setText("Talla: " + item.getTallaElegida().replace("_", "."));
        holder.tvQuantity.setText(String.valueOf(item.getCantidad()));
        Glide.with(holder.itemView.getContext()).load(item.getImageUrl()).into(holder.ivProduct);

        // Controles
        holder.ivDelete.setOnClickListener(v -> listener.onItemDeleted(item));

        holder.ivAdd.setOnClickListener(v -> {
            item.setCantidad(item.getCantidad() + 1);
            listener.onItemChanged(item);
        });

        holder.ivMinus.setOnClickListener(v -> {
            if (item.getCantidad() > 1) {
                item.setCantidad(item.getCantidad() - 1);
                listener.onItemChanged(item);
            }
        });
    }

    @Override public int getItemCount() { return cartList.size(); }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvOriginalPrice, tvSize, tvQuantity, tvDiscountBadge;
        ImageView ivProduct, ivMinus, ivAdd, ivDelete;

        public CartViewHolder(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvCartItemName);
            tvPrice = v.findViewById(R.id.tvCartItemPrice);
            tvOriginalPrice = v.findViewById(R.id.tvCartItemOriginalPrice);
            tvSize = v.findViewById(R.id.tvCartItemSize);
            tvQuantity = v.findViewById(R.id.tvCartItemQuantity);

            tvDiscountBadge = v.findViewById(R.id.tvCartItemDiscountBadge);

            ivProduct = v.findViewById(R.id.ivCartItemImage);
            ivMinus = v.findViewById(R.id.ivCartItemMinus);
            ivAdd = v.findViewById(R.id.ivCartItemAdd);
            ivDelete = v.findViewById(R.id.ivCartItemDelete);
        }
    }
}