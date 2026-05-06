package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * The @Entity annotation tells Room to create a table in the database.
 * We specify the table name to keep it consistent and organized.
 */
@Entity(tableName = "items_table")
public class Item {

    /**
     * The @PrimaryKey annotation marks this field as the unique identifier for each row.
     * Setting autoGenerate to true means Room will automatically handle the ID (1, 2, 3...).
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private double price;
    private int quantity;
    private String ownerId;

    /**
     * Constructor used to create a new Item object.
     * Note: The 'id' is not in the constructor because it is auto-generated.
     */
    public Item(String name, double price,  int quantity, String ownerId) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.ownerId = ownerId;
    }

    /**
     * Room needs Getters and Setters to access and update the private fields.
     */
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
}