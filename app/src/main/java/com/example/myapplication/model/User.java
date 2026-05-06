package com.example.myapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_table")
public class User {

    @PrimaryKey
    @NonNull
    private String userId; // Unique ID from Firebase Authentication

    private String email;

    // Constructor
    public User(@NonNull String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    // Getters and Setters for Room
    @NonNull
    public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}