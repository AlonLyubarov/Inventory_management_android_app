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
        mFirestore.collection("users")
                .whereEqualTo("employerId", currentManagerId)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        List<User> userList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            User user = doc.toObject(User.class);
                            if (user.getUserId() == null) user.setUserId(doc.getId());
                            if (!user.getUserId().equals(currentManagerId)) {
                                userList.add(user);
                            }
                        }
                        adapter.setUsers(userList);
                    }
                });
    }

    private void linkWorkerByEmail(String workerEmail) {
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
                        
                        // Fix H3: Prevent linking a user who is already a MANAGER
                        if ("MANAGER".equals(worker.getRole())) {
                            Toast.makeText(this, "לא ניתן לצרף משתמש שהוא מנהל בעצמו", Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        mFirestore.collection("users").document(doc.getId())
                                .update("employerId", currentManagerId)
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, "העובד צורף בהצלחה", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(err -> Toast.makeText(this, "שגיאה בחיבור העובד", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void updateUserRole(User user, String newRole) {
        mFirestore.collection("users").document(user.getUserId())
                .update("role", newRole)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "הרשאה עודכנה", Toast.LENGTH_SHORT).show());
    }
}
