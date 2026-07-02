package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@Entity(tableName = "transactions_table", 
        indices = {
            @Index(value = {"firestoreId"}, unique = true),
            @Index(value = {"ownerId", "timestamp"})
        })
@IgnoreExtraProperties
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String type; 
    private String itemName;
    private int quantityChanged;
    private long amountCents; // H4 Fix: Precise currency
    private long timestamp;
    private String ownerId;
    private String firestoreId;
    
    private String performedBy;
    private String performedByRole;

    public Transaction() {
    }

    public Transaction(String type, String itemName, int quantityChanged, double amountChanged, long timestamp, String ownerId, String performedBy, String performedByRole) {
        this.type = type;
        this.itemName = itemName;
        this.quantityChanged = quantityChanged;
        this.amountCents = Math.round(amountChanged * 100);
        this.timestamp = timestamp;
        this.ownerId = ownerId;
        this.performedBy = performedBy;
        this.performedByRole = performedByRole;
    }

    @Exclude
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public int getQuantityChanged() { return quantityChanged; }
    public void setQuantityChanged(int quantityChanged) { this.quantityChanged = quantityChanged; }
    
    public long getAmountCents() { return amountCents; }
    public void setAmountCents(long amountCents) { this.amountCents = amountCents; }

    @Exclude
    public double getAmountChanged() { return amountCents / 100.0; }
    public void setAmountChanged(double amount) { this.amountCents = Math.round(amount * 100); }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getFirestoreId() { return firestoreId; }
    public void setFirestoreId(String firestoreId) { this.firestoreId = firestoreId; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getPerformedByRole() { return performedByRole; }
    public void setPerformedByRole(String performedByRole) { this.performedByRole = performedByRole; }
}
