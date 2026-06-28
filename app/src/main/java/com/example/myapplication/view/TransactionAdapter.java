package com.example.myapplication.view;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionHolder> {

    private List<Transaction> transactions = new ArrayList<>();

    @NonNull
    @Override
    public TransactionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_row, parent, false);
        return new TransactionHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionHolder holder, int position) {
        Transaction current = transactions.get(position);
        Context context = holder.itemView.getContext();
        
        holder.type.setText(translateType(context, current.getType()));
        holder.itemName.setText(current.getItemName());
        
        String systemLabel = context.getString(R.string.system_label);
        String performer = (current.getPerformedBy() != null ? current.getPerformedBy() : systemLabel) + 
                " [" + translateRole(context, current.getPerformedByRole()) + "]";
        holder.performer.setText(performer);

        String qtyLabel = context.getString(R.string.qty_label);
        String valLabel = context.getString(R.string.value_label);
        String details = String.format(Locale.getDefault(), "%s %d | %s ₪%.2f", 
                qtyLabel, current.getQuantityChanged(), valLabel, current.getAmountChanged());
        holder.details.setText(details);

        String dateString = DateFormat.format("dd/MM/yy HH:mm", current.getTimestamp()).toString();
        holder.time.setText(dateString);

        holder.type.setTextColor(0xFF212121);

        switch (current.getType()) {
            case "ADD":
            case "UPDATE_PLUS":
                holder.type.setTextColor(0xFF388E3C);
                break;
            case "DELETE":
            case "UPDATE_MINUS":
                holder.type.setTextColor(0xFFD32F2F);
                break;
            default:
                holder.type.setTextColor(0xFF1976D2);
        }
    }

    private String translateType(Context ctx, String type) {
        switch (type) {
            case "ADD": return ctx.getString(R.string.type_add);
            case "DELETE": return ctx.getString(R.string.type_delete);
            case "UPDATE_PLUS": return ctx.getString(R.string.type_update_plus);
            case "UPDATE_MINUS": return ctx.getString(R.string.type_update_minus);
            default: return type;
        }
    }

    private String translateRole(Context ctx, String role) {
        if (role == null) return ctx.getString(R.string.role_unknown);
        switch (role) {
            case "MANAGER": return ctx.getString(R.string.role_manager);
            case "SHIFT_LEADER": return ctx.getString(R.string.role_leader);
            case "WORKER": return ctx.getString(R.string.role_worker);
            default: return role;
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void setTransactions(List<Transaction> newTransactions) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return transactions.size(); }
            @Override public int getNewListSize() { return newTransactions.size(); }
            @Override public boolean areItemsTheSame(int op, int np) {
                String oldId = transactions.get(op).getFirestoreId();
                String newId = newTransactions.get(np).getFirestoreId();
                if (oldId != null && newId != null) return oldId.equals(newId);
                return transactions.get(op).getId() == newTransactions.get(np).getId();
            }
            @Override public boolean areContentsTheSame(int op, int np) {
                return transactions.get(op).getTimestamp() == newTransactions.get(np).getTimestamp() &&
                       transactions.get(op).getQuantityChanged() == newTransactions.get(np).getQuantityChanged();
            }
        });
        this.transactions = new ArrayList<>(newTransactions);
        result.dispatchUpdatesTo(this);
    }

    static class TransactionHolder extends RecyclerView.ViewHolder {
        private final TextView type, itemName, details, time, performer;

        public TransactionHolder(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.text_transaction_type);
            itemName = itemView.findViewById(R.id.text_transaction_item);
            details = itemView.findViewById(R.id.text_transaction_details);
            time = itemView.findViewById(R.id.text_transaction_time);
            performer = itemView.findViewById(R.id.text_transaction_performer);
        }
    }
}
