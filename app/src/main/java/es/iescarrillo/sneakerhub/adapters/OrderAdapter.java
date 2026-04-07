package es.iescarrillo.sneakerhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.models.Order;
import es.iescarrillo.sneakerhub.ui.MainActivity;
import es.iescarrillo.sneakerhub.ui.OrderDetailFragment;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
    private List<Order> orderList;

    public OrderAdapter(List<Order> orderList) { this.orderList = orderList; }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orderList.get(position);

        // TÍTULO: "Pedido #" + ID acortado (6 caracteres)
        String shortId = order.getOrder_id().length() > 6
                ? order.getOrder_id().substring(0, 6).toUpperCase()
                : order.getOrder_id().toUpperCase();
        holder.tvOrderTitle.setText("Pedido #" + shortId);

        // SUBTÍTULO: Fecha del pedido
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.tvOrderSubtitle.setText(sdf.format(new Date(order.getOrder_date())));

        // ESTADO Y TOTAL
        holder.tvStatus.setText(order.getStatus());
        holder.tvTotal.setText(String.format("%.2f €", order.getTotal()));

        // LISTA DE ZAPATILLAS (Debajo del título/fecha)
        holder.tvSummary.setText(order.getItemsListFormatted());

        holder.itemView.setOnClickListener(v -> {
            if (v.getContext() instanceof MainActivity) {
                ((MainActivity) v.getContext()).loadFragment(new OrderDetailFragment(order), null);
            }
        });
    }

    @Override public int getItemCount() { return orderList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderTitle, tvOrderSubtitle, tvStatus, tvSummary, tvTotal;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderTitle = itemView.findViewById(R.id.tvOrderTitle);
            tvOrderSubtitle = itemView.findViewById(R.id.tvOrderSubtitle);
            tvStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvSummary = itemView.findViewById(R.id.tvOrderItemsSummary);
            tvTotal = itemView.findViewById(R.id.tvOrderTotal);
        }
    }
}