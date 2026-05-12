package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions_table")
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String type; // "ADD", "DELETE", "UPDATE_PLUS", "UPDATE_MINUS"
    private String itemName;
    private int quantityChanged;
    private double amountChanged;
    private long timestamp;
    private String ownerId;

    public Transaction(String type, String itemName, int quantityChanged, double amountChanged, long timestamp, String ownerId) {
        this.type = type;
        this.itemName = itemName;
        this.quantityChanged = quantityChanged;
        this.amountChanged = amountChanged;
        this.timestamp = timestamp;
        this.ownerId = ownerId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getType() { return type; }
    public String getItemName() { return itemName; }
    public int getQuantityChanged() { return quantityChanged; }
    public double getAmountChanged() { return amountChanged; }
    public long getTimestamp() { return timestamp; }
    public String getOwnerId() { return ownerId; }
}
