package com.example.myapplication.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
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
        holder.textName.setText(current.getName());
        holder.textSku.setText("מק''ט: " + current.getSku());
        holder.textPrice.setText(String.format(Locale.getDefault(), "₪%.2f", current.getDefaultPrice()));

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(current);
        });
    }

    @Override
    public int getItemCount() { return templates.size(); }

    public void setTemplates(List<ProductTemplate> newTemplates) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return templates.size(); }
            @Override
            public int getNewListSize() { return newTemplates.size(); }
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return templates.get(oldItemPosition).getId() == newTemplates.get(newItemPosition).getId();
            }
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ProductTemplate oldT = templates.get(oldItemPosition);
                ProductTemplate newT = newTemplates.get(newItemPosition);
                return oldT.getName().equals(newT.getName()) && 
                       oldT.getSku().equals(newT.getSku()) && 
                       oldT.getDefaultPrice() == newT.getDefaultPrice();
            }
        });
        this.templates = new ArrayList<>(newTemplates);
        diffResult.dispatchUpdatesTo(this);
    }

    static class CatalogHolder extends RecyclerView.ViewHolder {
        TextView textName, textSku, textPrice;
        ImageButton btnDelete;

        public CatalogHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_catalog_name);
            textSku = itemView.findViewById(R.id.text_catalog_sku);
            textPrice = itemView.findViewById(R.id.text_catalog_price);
            btnDelete = itemView.findViewById(R.id.button_delete_catalog);
        }
    }
}
