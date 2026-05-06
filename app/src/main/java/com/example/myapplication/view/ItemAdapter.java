package com.example.myapplication.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * The Adapter connects the data (List of Items) to the RecyclerView (UI).
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemHolder> {

    private List<Item> items = new ArrayList<>();

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflating the item_row layout we created earlier
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
        holder.textViewQuantity.setText("Quantity: " + quantity);
        holder.textViewPrice.setText("Price: ₪" + price);

        holder.textViewTotalPrice.setText("Total Value: ₪" + String.format("%.2f", total));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Method to update the list of items whenever the database changes.
     */
    public void setItems(List<Item> items) {
        this.items = items;
        notifyDataSetChanged(); // Tells the RecyclerView to refresh the screen
    }

    /**
     * Helper method to retrieve an Item object at a specific position.
     * This is essential for swipe-to-delete logic in MainActivity.
     */
    public Item getItemAt(int position) {
        return items.get(position);
    }

    /**
     * ViewHolder holds the references to the views in item_row.xml.
     */
    class ItemHolder extends RecyclerView.ViewHolder {
        private TextView textViewName;
        private TextView textViewQuantity;

        private TextView textViewPrice;

        private TextView textViewTotalPrice;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_name);
            textViewQuantity = itemView.findViewById(R.id.text_view_quantity);
            textViewPrice = itemView.findViewById(R.id.text_view_price);
            textViewTotalPrice = itemView.findViewById(R.id.text_view_total_price);

        }
    }
}