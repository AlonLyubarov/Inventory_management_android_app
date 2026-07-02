package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@Entity(tableName = "items_table", 
        indices = {
            @Index(value = {"firestoreId"}, unique = true),
            @Index(value = {"ownerId", "sku"}),
            @Index(value = {"ownerId", "name"})
        })
@IgnoreExtraProperties
public class Item {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private long priceCents; // H4 Fix: Precise currency
    private int quantity;
    private String ownerId;
    private long createdAt;
    private String firestoreId;
    private String sku;
    private int lowStockThreshold;
    private String brand;

    public Item() {
    }

    public Item(String name, double price, int quantity, String ownerId, String sku, String brand) {
        this.name = name;
        this.priceCents = Math.round(Math.max(0.0, price) * 100);
        this.quantity = Math.max(0, quantity);
        this.ownerId = ownerId;
        this.sku = sku;
        this.brand = brand;
        this.createdAt = System.currentTimeMillis();
        this.lowStockThreshold = 0;
    }

    @Exclude
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = Math.max(0, quantity); }
    
    public long getPriceCents() { return priceCents; }
    public void setPriceCents(long priceCents) { this.priceCents = Math.max(0, priceCents); }

    @Exclude
    public double getPrice() { return priceCents / 100.0; }
    public void setPrice(double price) { this.priceCents = Math.round(Math.max(0.0, price) * 100); }
    
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getFirestoreId() { return firestoreId; }
    public void setFirestoreId(String firestoreId) { this.firestoreId = firestoreId; }
    
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
}
