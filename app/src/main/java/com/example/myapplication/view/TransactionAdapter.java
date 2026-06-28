package com.example.myapplication.view;

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
        holder.type.setText(translateType(current.getType()));
        holder.itemName.setText(current.getItemName());
        
        String performer = (current.getPerformedBy() != null ? current.getPerformedBy() : "System") + 
                " [" + translateRole(current.getPerformedByRole()) + "]";
        holder.performer.setText(performer);

        String details = String.format(Locale.getDefault(), "Qty: %d | Value: ₪%.2f", 
                current.getQuantityChanged(), current.getAmountChanged());
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

    private String translateType(String type) {
        switch (type) {
            case "ADD": return "Add";
            case "DELETE": return "Delete";
            case "UPDATE_PLUS": return "In (+)";
            case "UPDATE_MINUS": return "Out (-)";
            default: return type;
        }
    }

    private String translateRole(String role) {
        if (role == null) return "Unknown";
        switch (role) {
            case "MANAGER": return "Manager";
            case "SHIFT_LEADER": return "Leader";
            case "WORKER": return "Worker";
            default: return role;
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void setTransactions(List<Transaction> newTransactions) {
        // B-10 Fix: Implement DiffUtil for Transactions
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
