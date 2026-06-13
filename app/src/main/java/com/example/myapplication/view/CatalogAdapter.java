package com.example.myapplication.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.ProductTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CatalogAdapter extends RecyclerView.Adapter<CatalogAdapter.CatalogHolder> {

    private List<ProductTemplate> templates = new ArrayList<>();
    private OnCatalogClickListener listener;

    public interface OnCatalogClickListener {
        void onDeleteClick(ProductTemplate template);
    }

    public void setOnCatalogClickListener(OnCatalogClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CatalogHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.catalog_row, parent, false);
        return new CatalogHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CatalogHolder holder, int position) {
        ProductTemplate current = templates.get(position);
        holder.name.setText(current.getName());
        holder.sku.setText("מק''ט: " + current.getSku());
        holder.price.setText(String.format(Locale.getDefault(), "₪%.2f", current.getDefaultPrice()));

        holder.delete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(current);
            }
        });
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    public void setTemplates(List<ProductTemplate> templates) {
        this.templates = templates;
        notifyDataSetChanged();
    }

    static class CatalogHolder extends RecyclerView.ViewHolder {
        private final TextView name, sku, price;
        private final ImageButton delete;

        public CatalogHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_catalog_name);
            sku = itemView.findViewById(R.id.text_catalog_sku);
            price = itemView.findViewById(R.id.text_catalog_price);
            delete = itemView.findViewById(R.id.button_delete_catalog);
        }
    }
}
