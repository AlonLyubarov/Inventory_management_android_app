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

        String[] roles = {"עובד מחסן", "ראש משמרת", "מנהל מחסן"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spinnerRoles.setAdapter(adapter);

        // UI Check: If user already authenticated (via Google or existing Auth), focus on profile completion
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            editTextEmail.setText(currentUser.getEmail());
            editTextEmail.setEnabled(false); // Lock email
            editTextPassword.setVisibility(View.GONE); // Password not needed for profile setup
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

            // Case A: User is already logged in (Google Flow or Profile Recovery)
            if (mAuth.getCurrentUser() != null) {
                saveUserToFirestore(mAuth.getUid(), email, name, role);
            } 
            // Case B: Traditional new user registration
            else {
                if (password.length() < 8) {
                    Toast.makeText(this, "סיסמה חייבת להיות לפחות 8 תווים", Toast.LENGTH_SHORT).show();
                    return;
                }
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                saveUserToFirestore(mAuth.getUid(), email, name, role);
                            } else {
                                Toast.makeText(this, "שגיאה ברישום: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }

    private void saveUserToFirestore(String uid, String email, String name, String role) {
        String employerId = uid;
        if (!"MANAGER".equals(role)) {
            employerId = "PENDING";
        }

        User newUser = new User(uid, email, name, role, employerId);

        mFirestore.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "הפרופיל נוצר בהצלחה", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Fix C5: Error handling for failed profile creation
                    Toast.makeText(this, "שגיאה ביצירת הפרופיל: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String mapLabelToRole(String label) {
        switch (label) {
            case "ראש משמרת": return "SHIFT_LEADER";
            case "מנהל מחסן": return "MANAGER";
            default: return "WORKER";
        }
    }
}
