package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "items_table")
public class Item {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private double price;
    private int quantity;
    private String ownerId;
    private long createdAt;

    public Item(String name, double price,  int quantity, String ownerId) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.ownerId = ownerId;
        this.createdAt = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
