package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@Entity(tableName = "transactions_table", indices = {@Index(value = {"firestoreId"}, unique = true)})
@IgnoreExtraProperties
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String type; // "ADD", "DELETE", "UPDATE_PLUS", "UPDATE_MINUS"
    private String itemName;
    private int quantityChanged;
    private double amountChanged;
    private long timestamp;
    private String ownerId;
    private String firestoreId;
    
    // Accountability Fields
    private String performedBy;     // User Display Name
    private String performedByRole; // User Role at time of action

    public Transaction() {
    }

    public Transaction(String type, String itemName, int quantityChanged, double amountChanged, long timestamp, String ownerId, String performedBy, String performedByRole) {
        this.type = type;
        this.itemName = itemName;
        this.quantityChanged = quantityChanged;
        this.amountChanged = amountChanged;
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
    public double getAmountChanged() { return amountChanged; }
    public void setAmountChanged(double amountChanged) { this.amountChanged = amountChanged; }
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
