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
    private double defaultPrice;
    private String ownerId;
    private String firestoreId;
    private int lowStockThreshold;
    
    // New field for Brand Name
    private String brand;

    public ProductTemplate() {}

    public ProductTemplate(String name, String sku, double defaultPrice, String ownerId, String brand) {
        this.name = name;
        this.sku = sku;
        this.defaultPrice = defaultPrice;
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
    public double getDefaultPrice() { return defaultPrice; }
    public void setDefaultPrice(double defaultPrice) { this.defaultPrice = defaultPrice; }
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
