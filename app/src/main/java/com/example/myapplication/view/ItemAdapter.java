package com.example.myapplication.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Item;

import java.util.ArrayList;
import java.util.List;

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
        Item currentItem = items.get(position);
        String name = currentItem.getName();
        int quantity = currentItem.getQuantity();
        double price = currentItem.getPrice();
        double total = quantity * price;

        holder.textViewName.setText(name);
        holder.textViewQuantity.setText(String.valueOf(quantity));
        holder.textViewPrice.setText("₪" + String.format("%.2f", price));
        holder.textViewTotalPrice.setText("סה\"כ שווי: ₪" + String.format("%.2f", total));

        holder.buttonPlus.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuantityChange(currentItem, quantity + 1);
            }
        });

        holder.buttonMinus.setOnClickListener(v -> {
            if (listener != null && quantity > 0) {
                listener.onQuantityChange(currentItem, quantity - 1);
            }
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<Item> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public Item getItemAt(int position) {
        return items.get(position);
    }

    class ItemHolder extends RecyclerView.ViewHolder {
        private TextView textViewName;
        private TextView textViewQuantity;
        private TextView textViewPrice;
        private TextView textViewTotalPrice;
        private Button buttonPlus;
        private Button buttonMinus;
        private ImageButton buttonDelete;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_name);
            textViewQuantity = itemView.findViewById(R.id.text_view_quantity);
            textViewPrice = itemView.findViewById(R.id.text_view_price);
            textViewTotalPrice = itemView.findViewById(R.id.text_view_total_price);
            buttonPlus = itemView.findViewById(R.id.button_plus);
            buttonMinus = itemView.findViewById(R.id.button_minus);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }
    }
}
