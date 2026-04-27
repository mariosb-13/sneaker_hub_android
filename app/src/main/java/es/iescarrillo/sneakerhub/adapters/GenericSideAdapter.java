package es.iescarrillo.sneakerhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import es.iescarrillo.sneakerhub.R;

public class GenericSideAdapter extends RecyclerView.Adapter<GenericSideAdapter.ViewHolder> {

    private List<String> itemList;
    private OnItemClickListener listener;
    private List<String> selectedItems = new ArrayList<>();
    private boolean isMultiSelect;

    public interface OnItemClickListener { void onItemClick(String item); }

    public GenericSideAdapter(List<String> itemList, boolean isMultiSelect, OnItemClickListener listener) {
        this.itemList = itemList;
        this.isMultiSelect = isMultiSelect;
        this.listener = listener;
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public ArrayList<String> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filter_pill, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = itemList.get(position);
        holder.ivIcon.setVisibility(View.GONE);
        holder.tvName.setVisibility(View.VISIBLE);
        holder.tvName.setText(item);

        // --- COMPROBAMOS SI ESTÁ EN LA LISTA ---
        if (selectedItems.contains(item)) {
            holder.root.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.color_6));
            holder.tvName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
        } else {
            holder.root.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.color_5));
            holder.tvName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_6));
        }

        holder.itemView.setOnClickListener(v -> {
            // LÓGICA DE MULTISELECCIÓN
            if (!isMultiSelect) {
                // Si no es multiselect (ej. Precios), borramos lo anterior
                selectedItems.clear();
            }

            // Si ya estaba seleccionado, lo quitamos. Si no, lo añadimos.
            if (selectedItems.contains(item)) {
                selectedItems.remove(item);
            } else {
                selectedItems.add(item);
            }

            notifyDataSetChanged();
            listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() { return itemList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout root;
        TextView tvName;
        android.widget.ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.lyPillRoot);
            tvName = itemView.findViewById(R.id.tvPillName);
            ivIcon = itemView.findViewById(R.id.ivPillIcon);
        }
    }
}