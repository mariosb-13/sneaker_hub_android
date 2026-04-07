package es.iescarrillo.sneakerhub.adapters;

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
import es.iescarrillo.sneakerhub.models.DetalleCarrito;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<DetalleCarrito> cartList;
    private OnCartItemChangeListener listener;

    public interface OnCartItemChangeListener { void onItemChanged(DetalleCarrito item); }

    public CartAdapter(List<DetalleCarrito> cartList, OnCartItemChangeListener listener) {
        this.cartList = cartList; this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CartViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        DetalleCarrito item = cartList.get(position);
        holder.tvName.setText(item.getName());
        holder.tvPrice.setText(String.format("%.2f €", item.getPrice()));
        holder.tvSize.setText("Talla: " + item.getTallaElegida().replace("_", "."));
        holder.tvQuantity.setText(String.valueOf(item.getCantidad()));
        Glide.with(holder.itemView.getContext()).load(item.getImageUrl()).into(holder.ivProduct);

        holder.ivAdd.setOnClickListener(v -> { item.setCantidad(item.getCantidad() + 1); listener.onItemChanged(item); });
        holder.ivMinus.setOnClickListener(v -> { item.setCantidad(item.getCantidad() - 1); listener.onItemChanged(item); });
    }

    @Override public int getItemCount() { return cartList.size(); }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvSize, tvQuantity;
        ImageView ivProduct, ivMinus, ivAdd;
        public CartViewHolder(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvCartItemName); tvPrice = v.findViewById(R.id.tvCartItemPrice);
            tvSize = v.findViewById(R.id.tvCartItemSize); tvQuantity = v.findViewById(R.id.tvCartItemQuantity);
            ivProduct = v.findViewById(R.id.ivCartItemImage); ivMinus = v.findViewById(R.id.ivCartItemMinus);
            ivAdd = v.findViewById(R.id.ivCartItemAdd);
        }
    }
}