package com.example.myapplication.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    private ManageUsersAdapter adapter;
    private FirebaseFirestore mFirestore;
    private String currentManagerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        mFirestore = FirebaseFirestore.getInstance();
        currentManagerId = FirebaseAuth.getInstance().getUid();

        RecyclerView recyclerView = findViewById(R.id.recycler_manage_users);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManageUsersAdapter();
        recyclerView.setAdapter(adapter);

        EditText editEmail = findViewById(R.id.edit_add_user_email);
        Button btnAdd = findViewById(R.id.button_add_user);

        loadMyTeam();

        btnAdd.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            if (!email.isEmpty()) {
                linkWorkerByEmail(email);
                editEmail.setText("");
            }
        });

        adapter.setOnRoleChangeListener(this::updateUserRole);
    }

    private void loadMyTeam() {
        // Using a real-time listener so the list updates automatically when a worker is added or changed
        mFirestore.collection("users")
                .whereEqualTo("employerId", currentManagerId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "שגיאה בעדכון הרשימה", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        List<User> userList = new ArrayList<>();
                        android.util.Log.d("ManageUsers", "Found " + snapshots.size() + " total users in snapshots");
                        for (QueryDocumentSnapshot doc : snapshots) {
                            User user = doc.toObject(User.class);
                            // Safety fix: If userId is missing inside the object, take it from the document ID
                            if (user.getUserId() == null) {
                                user.setUserId(doc.getId());
                            }
                            
                            // Don't show the manager himself, only his team
                            if (!user.getUserId().equals(currentManagerId)) {
                                userList.add(user);
                            }
                        }
                        android.util.Log.d("ManageUsers", "Final team list size: " + userList.size());
                        adapter.setUsers(userList);
                    }
                });
    }

    private void linkWorkerByEmail(String workerEmail) {
        // Standardize email to lowercase to prevent matching issues
        String searchEmail = workerEmail.toLowerCase().trim();
        
        mFirestore.collection("users")
                .whereEqualTo("email", searchEmail)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        Toast.makeText(this, "לא נמצא משתמש עם המייל: " + searchEmail, Toast.LENGTH_LONG).show();
                        return;
                    }
                    for (QueryDocumentSnapshot doc : snapshots) {
                        User worker = doc.toObject(User.class);
                        // Safety: ensure UID is set from doc ID if missing
                        if (worker.getUserId() == null) worker.setUserId(doc.getId());
                        
                        worker.setEmployerId(currentManagerId); 
                        
                        mFirestore.collection("users").document(worker.getUserId()).set(worker)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "העובד " + worker.getDisplayName() + " צורף לצוות שלך", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(err -> {
                                    Toast.makeText(this, "שגיאה בחיבור העובד", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void updateUserRole(User user, String newRole) {
        user.setRole(newRole);
        mFirestore.collection("users").document(user.getUserId()).set(user)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "הרשאה עודכנה", Toast.LENGTH_SHORT).show());
    }
}
