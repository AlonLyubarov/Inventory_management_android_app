package com.example.myapplication.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword, editTextDisplayName;
    private Spinner spinnerRoles;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        editTextDisplayName = findViewById(R.id.edit_text_display_name);
        editTextEmail = findViewById(R.id.edit_text_email_reg);
        editTextPassword = findViewById(R.id.edit_text_password_reg);
        spinnerRoles = findViewById(R.id.spinner_roles);
        Button buttonRegister = findViewById(R.id.button_register);

        // C1 Fix: Remove MANAGER from self-selection list
        String[] roles = {"עובד מחסן", "ראש משמרת"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spinnerRoles.setAdapter(adapter);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            editTextEmail.setText(currentUser.getEmail());
            editTextEmail.setEnabled(false); 
            editTextPassword.setVisibility(View.GONE);
        }

        buttonRegister.setOnClickListener(v -> {
            String name = editTextDisplayName.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim().toLowerCase();
            String password = editTextPassword.getText().toString().trim();
            String selectedRoleLabel = spinnerRoles.getSelectedItem().toString();
            String role = mapLabelToRole(selectedRoleLabel);

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mAuth.getCurrentUser() != null) {
                saveUserProfile(mAuth.getUid(), email, name, role);
            } else {
                if (password.length() < 8) {
                    Toast.makeText(this, "סיסמה חייבת להיות לפחות 8 תווים", Toast.LENGTH_SHORT).show();
                    return;
                }
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                saveUserProfile(mAuth.getUid(), email, name, role);
                            } else {
                                String err = task.getException() != null ? task.getException().getMessage() : "Unknown";
                                Toast.makeText(this, "שגיאה ברישום: " + err, Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }

    /**
     * H1 Fix: Use merge and prevent role escalation for existing users.
     */
    private void saveUserProfile(String uid, String email, String name, String role) {
        mFirestore.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", uid);
            data.put("email", email);
            data.put("displayName", name);
            
            if (!doc.exists()) {
                // New user: Set initial role and employerId
                data.put("role", role);
                data.put("employerId", "PENDING");
            } else {
                // H1 Fix: Existing user cannot change their role from this screen
                Toast.makeText(this, "פרופיל קיים - מעדכן פרטים בסיסיים בלבד", Toast.LENGTH_SHORT).show();
            }

            mFirestore.collection("users").document(uid).set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.success_profile_created, Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_server_access, Toast.LENGTH_SHORT).show());
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String mapLabelToRole(String label) {
        if ("ראש משמרת".equals(label)) return "SHIFT_LEADER";
        return "WORKER"; // Default
    }
}
