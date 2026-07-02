package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@Entity(tableName = "product_templates_table", indices = {@Index(value = {"firestoreId"}, unique = true)})
@IgnoreExtraProperties
public class ProductTemplate {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String name;
    private String sku;
    private long priceCents; // H4 Fix: Precise currency
    private String ownerId;
    private String firestoreId;
    private int lowStockThreshold;
    private String brand;

    public ProductTemplate() {}

    public ProductTemplate(String name, String sku, double defaultPrice, String ownerId, String brand) {
        this.name = name;
        this.sku = sku;
        this.priceCents = Math.round(Math.max(0.0, defaultPrice) * 100);
        this.ownerId = ownerId;
        this.brand = brand;
        this.lowStockThreshold = 0;
    }

    @Exclude
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    
    public long getPriceCents() { return priceCents; }
    public void setPriceCents(long priceCents) { this.priceCents = priceCents; }

    @Exclude
    public double getDefaultPrice() { return priceCents / 100.0; }
    public void setDefaultPrice(double price) { this.priceCents = Math.round(Math.max(0.0, price) * 100); }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getFirestoreId() { return firestoreId; }
    public void setFirestoreId(String firestoreId) { this.firestoreId = firestoreId; }

    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    @Override
    public String toString() {
        return name + " (" + sku + ")";
    }
}
