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
import es.iescarrillo.sneakerhub.models.SneakerCopy;

public class OrderDetailAdapter extends RecyclerView.Adapter<OrderDetailAdapter.ViewHolder> {
    private List<SneakerCopy> list;
    public OrderDetailAdapter(List<SneakerCopy> list) { this.list = list; }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_detail_product, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SneakerCopy item = list.get(position);
        holder.name.setText(item.getName_snap());
        holder.info.setText("Talla " + item.getTalla_elegida() + " | Cant: " + item.getCantidad_comprada());
        holder.price.setText(String.format("%.2f €", item.calcularSubtotal()));
        Glide.with(holder.itemView).load(item.getImagen_snap()).into(holder.img);
    }

    @Override public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, info, price; ImageView img;
        public ViewHolder(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.tvProdName);
            info = v.findViewById(R.id.tvProdInfo);
            price = v.findViewById(R.id.tvProdPrice);
            img = v.findViewById(R.id.ivProdDetail);
        }
    }
}