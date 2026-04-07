package es.iescarrillo.sneakerhub.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import es.iescarrillo.sneakerhub.R;

public class GenericSideAdapter extends RecyclerView.Adapter<GenericSideAdapter.ViewHolder> {

    private List<String> itemList;
    private OnItemClickListener listener;
    private int selectedPos = -1;

    public interface OnItemClickListener { void onItemClick(String item); }

    public GenericSideAdapter(List<String> itemList, OnItemClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filter_pill, parent, false);        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = itemList.get(position);
        holder.ivIcon.setVisibility(View.GONE);
        holder.tvName.setVisibility(View.VISIBLE);
        holder.tvName.setText(item);

        if (selectedPos == position) {
            holder.root.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.color_6));
            holder.tvName.setTextColor(Color.WHITE);
        } else {
            holder.root.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.color_5));
            holder.tvName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_6));
        }

        holder.itemView.setOnClickListener(v -> {
            selectedPos = holder.getAdapterPosition();
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