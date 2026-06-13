package com.example.myapplication.view;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Item;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemHolder> {

    private List<Item> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onQuantityChange(Item item, int newQuantity);
        void onDeleteClick(Item item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false);
        return new ItemHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
        Item item = items.get(position);

        holder.textViewName.setText(item.getName());
        holder.textViewSku.setText(item.getSku() != null ? "מק''ט: " + item.getSku() : "");
        holder.textViewQuantity.setText(String.valueOf(item.getQuantity()));
        holder.textViewPrice.setText(String.format(Locale.getDefault(), "₪%.2f", item.getPrice()));
        
        double total = item.getQuantity() * item.getPrice();
        holder.textViewTotalPrice.setText(String.format(Locale.getDefault(), "סה\"כ שווי: ₪%.2f", total));

        // UI Reset
        holder.itemCard.setCardBackgroundColor(Color.WHITE);
        holder.textViewQuantity.setTextColor(Color.BLACK);

        if (item.getLowStockThreshold() > 0 && item.getQuantity() < item.getLowStockThreshold()) {
            holder.itemCard.setCardBackgroundColor(0xFFFFEBEE);
            holder.textViewQuantity.setTextColor(0xFFD32F2F);
            holder.textViewName.setText("⚠️ " + item.getName());
        }

        // Actions: Pass to listener WITHOUT local modification for reactive flow
        holder.buttonPlus.setOnClickListener(v -> {
            if (listener != null) listener.onQuantityChange(item, item.getQuantity() + 1);
        });

        holder.buttonMinus.setOnClickListener(v -> {
            if (listener != null && item.getQuantity() > 0) 
                listener.onQuantityChange(item, item.getQuantity() - 1);
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void setItems(List<Item> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ItemDiffCallback(this.items, newItems));
        this.items = new ArrayList<>(newItems); // Clean copy
        diffResult.dispatchUpdatesTo(this);
    }

    private static class ItemDiffCallback extends DiffUtil.Callback {
        private final List<Item> oldList, newList;
        public ItemDiffCallback(List<Item> oldList, List<Item> newList) { this.oldList = oldList; this.newList = newList; }
        @Override
        public int getOldListSize() { return oldList.size(); }
        @Override
        public int getNewListSize() { return newList.size(); }
        @Override
        public boolean areItemsTheSame(int op, int np) {
            // CRITICAL: Compare using firestoreId to prevent list jumping during sync
            String oldId = oldList.get(op).getFirestoreId();
            String newId = newList.get(np).getFirestoreId();
            if (oldId != null && newId != null) return oldId.equals(newId);
            return oldList.get(op).getId() == newList.get(np).getId();
        }
        @Override
        public boolean areContentsTheSame(int op, int np) {
            Item o = oldList.get(op), n = newList.get(np);
            return o.getQuantity() == n.getQuantity() && o.getPrice() == n.getPrice() && o.getName().equals(n.getName());
        }
    }

    class ItemHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewSku, textViewQuantity, textViewPrice, textViewTotalPrice;
        Button buttonPlus, buttonMinus;
        ImageButton buttonDelete;
        MaterialCardView itemCard;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_name);
            textViewSku = itemView.findViewById(R.id.text_view_sku);
            textViewQuantity = itemView.findViewById(R.id.text_view_quantity);
            textViewPrice = itemView.findViewById(R.id.text_view_price);
            textViewTotalPrice = itemView.findViewById(R.id.text_view_total_price);
            buttonPlus = itemView.findViewById(R.id.button_plus);
            buttonMinus = itemView.findViewById(R.id.button_minus);
            buttonDelete = itemView.findViewById(R.id.button_delete);
            itemCard = itemView.findViewById(R.id.item_card_view);
        }
    }
}
