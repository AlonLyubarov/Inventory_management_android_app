package com.example.myapplication.view;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
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
        
        // Show Performer details
        String performer = (current.getPerformedBy() != null ? current.getPerformedBy() : "מערכת") + 
                " [" + translateRole(current.getPerformedByRole()) + "]";
        holder.performer.setText(performer);

        String details = String.format(Locale.getDefault(), "כמות: %d | שווי: ₪%.2f", 
                current.getQuantityChanged(), current.getAmountChanged());
        holder.details.setText(details);

        String dateString = DateFormat.format("dd/MM/yy HH:mm", current.getTimestamp()).toString();
        holder.time.setText(dateString);

        // Color based on type
        switch (current.getType()) {
            case "ADD":
            case "UPDATE_PLUS":
                holder.type.setTextColor(0xFF388E3C); // Green
                break;
            case "DELETE":
            case "UPDATE_MINUS":
                holder.type.setTextColor(0xFFD32F2F); // Red
                break;
            default:
                holder.type.setTextColor(0xFF1976D2); // Blue
        }
    }

    private String translateType(String type) {
        switch (type) {
            case "ADD": return "הוספה";
            case "DELETE": return "מחיקה";
            case "UPDATE_PLUS": return "הוספת כמות";
            case "UPDATE_MINUS": return "הורדת כמות";
            default: return type;
        }
    }

    private String translateRole(String role) {
        if (role == null) return "לא ידוע";
        switch (role) {
            case "MANAGER": return "מנהל";
            case "SHIFT_LEADER": return "ר.משמרת";
            case "WORKER": return "עובד";
            default: return role;
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
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
