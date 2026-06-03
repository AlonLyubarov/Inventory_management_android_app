package com.example.myapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.IgnoreExtraProperties;

@Entity(tableName = "user_table")
@IgnoreExtraProperties
public class User {

    @PrimaryKey
    @NonNull
    private String userId;

    private String email;
    private String displayName;
    private String role; // "WORKER", "SHIFT_LEADER", "MANAGER"
    private String employerId; // The UID of the manager this user works for

    public User() {
    }

    public User(@NonNull String userId, String email, String displayName, String role, String employerId) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.employerId = employerId;
    }

    @NonNull
    public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmployerId() { return employerId; }
    public void setEmployerId(String employerId) { this.employerId = employerId; }
}
