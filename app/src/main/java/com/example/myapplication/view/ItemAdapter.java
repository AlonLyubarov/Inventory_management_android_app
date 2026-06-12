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
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_row, parent, false);
        return new ItemHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
        // Fallback to standard getAdapterPosition() for backwards compatibility support
        int currentPos = holder.getAdapterPosition();
        if (currentPos == RecyclerView.NO_POSITION) return;

        Item currentItem = items.get(currentPos);

        holder.textViewName.setText(currentItem.getName());

        if (currentItem.getSku() != null && !currentItem.getSku().isEmpty()) {
            holder.textViewSku.setVisibility(View.VISIBLE);
            holder.textViewSku.setText("מק''ט: " + currentItem.getSku());
        } else {
            holder.textViewSku.setVisibility(View.GONE);
        }

        int quantity = currentItem.getQuantity();
        int threshold = currentItem.getLowStockThreshold();

        holder.textViewQuantity.setText(String.valueOf(quantity));

        // Low Stock Alert Logic Layout Engine execution
        if (threshold > 0 && quantity < threshold) {
            holder.itemView.findViewById(R.id.item_card_container).setBackgroundColor(0xFFFFEBEE);
            holder.textViewQuantity.setTextColor(0xFFD32F2F);
            holder.textViewName.setText("⚠️ " + currentItem.getName() + " (מלאי נמוך!)");
        } else {
            holder.itemView.findViewById(R.id.item_card_container).setBackgroundColor(Color.WHITE);
            holder.textViewQuantity.setTextColor(0xFF000000);
            holder.textViewName.setText(currentItem.getName());
        }

        holder.textViewPrice.setText(String.format(Locale.getDefault(), "₪%.2f", currentItem.getPrice()));

        double total = quantity * currentItem.getPrice();
        holder.textViewTotalPrice.setText(String.format(Locale.getDefault(), "סה\"כ שווי: ₪%.2f", total));

        // Asynchronous structural click interactions bindings setup with live evaluation
        holder.buttonPlus.setOnClickListener(v -> {
            if (listener != null) {
                int safePos = holder.getAdapterPosition();
                if (safePos != RecyclerView.NO_POSITION) {
                    Item liveItem = items.get(safePos);
                    // Dynamically evaluate the absolute live value inside model storage
                    listener.onQuantityChange(liveItem, liveItem.getQuantity() + 1);
                }
            }
        });

        holder.buttonMinus.setOnClickListener(v -> {
            if (listener != null) {
                int safePos = holder.getAdapterPosition();
                if (safePos != RecyclerView.NO_POSITION) {
                    Item liveItem = items.get(safePos);
                    int liveQty = liveItem.getQuantity();
                    if (liveQty > 0) {
                        listener.onQuantityChange(liveItem, liveQty - 1);
                    }
                }
            }
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) {
                int safePos = holder.getAdapterPosition();
                if (safePos != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(items.get(safePos));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Professional way to update the list using DiffUtil.
     * This prevents items from jumping around and animates changes smoothly.
     */
    public void setItems(List<Item> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ItemDiffCallback(this.items, newItems));
        this.items.clear();
        this.items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Callback class for DiffUtil to compare old and new list items.
     */
    private static class ItemDiffCallback extends DiffUtil.Callback {
        private final List<Item> oldList;
        private final List<Item> newList;

        public ItemDiffCallback(List<Item> oldList, List<Item> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }

        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getId() == newList.get(newPos).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            Item oldItem = oldList.get(oldPos);
            Item newItem = newList.get(newPos);
            return oldItem.getQuantity() == newItem.getQuantity() &&
                    oldItem.getPrice() == newItem.getPrice() &&
                    oldItem.getName().equals(newItem.getName());
        }
    }

    class ItemHolder extends RecyclerView.ViewHolder {
        private final TextView textViewName, textViewSku, textViewQuantity, textViewPrice, textViewTotalPrice;
        private final Button buttonPlus, buttonMinus;
        private final ImageButton buttonDelete;

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
        }
    }
}