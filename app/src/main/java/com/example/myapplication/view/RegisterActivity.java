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
    private boolean isGoogleUserFlow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Initialize UI components
        editTextDisplayName = findViewById(R.id.edit_text_display_name);
        editTextEmail = findViewById(R.id.edit_text_email_reg);
        editTextPassword = findViewById(R.id.edit_text_password_reg);
        spinnerRoles = findViewById(R.id.spinner_roles);
        Button buttonRegister = findViewById(R.id.button_register);

        // Configure the roles selection spinner
        String[] roles = {"עובד מחסן", "ראש משמרת", "מנהל מחסן"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spinnerRoles.setAdapter(adapter);

        // Check if a user session already exists from a third-party identity provider (e.g., Google Sign-In)
        FirebaseUser currentFirebaseUser = mAuth.getCurrentUser();
        if (currentFirebaseUser != null) {
            isGoogleUserFlow = true;

            // Auto-populate the validated email directly from the active Google session
            editTextEmail.setText(currentFirebaseUser.getEmail());
            editTextEmail.setEnabled(false); // Lock the input field to prevent mismatch errors

            // Hide the password field entirely since Google-linked accounts don't use local passwords
            editTextPassword.setVisibility(View.GONE);
        }

        // Setup the registration trigger action listener
        buttonRegister.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim().toLowerCase();
            String password = editTextPassword.getText().toString().trim();
            String name = editTextDisplayName.getText().toString().trim();
            String selectedRoleLabel = spinnerRoles.getSelectedItem().toString();
            String role = mapLabelToRole(selectedRoleLabel);

            // Validation check for mandatory profile information fields
            if (email.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "נא למלא שם ואימייל", Toast.LENGTH_SHORT).show();
                return;
            }

            // SMART REGISTER FLOW: If logged in via Google, bypass auth credential creation completely
            if (isGoogleUserFlow && mAuth.getCurrentUser() != null) {
                saveUserToFirestore(mAuth.getUid(), email, name, role);
                return;
            }

            // Standard Email/Password registration validation fallback branch
            if (password.isEmpty() || !isValidPassword(password)) {
                Toast.makeText(this, "סיסמה לא תקינה", Toast.LENGTH_LONG).show();
                return;
            }

            // Create a brand new traditional email/password account inside Firebase Auth backend
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Account created; map the newly provisioned UID to Firestore
                            saveUserToFirestore(mAuth.getCurrentUser().getUid(), email, name, role);
                        } else {
                            // If user already exists in Auth records, attempt standard fallback synchronization
                            if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                                mAuth.signInWithEmailAndPassword(email, password)
                                        .addOnSuccessListener(res -> saveUserToFirestore(res.getUser().getUid(), email, name, role))
                                        .addOnFailureListener(e -> Toast.makeText(this, "סיסמה שגויה למשתמש קיים", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(this, "שגיאה ברישום", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });
    }

    /**
     * Maps user metadata records and pushes the newly provisioned User profile structural document down to Firestore.
     */
    private void saveUserToFirestore(String uid, String email, String name, String role) {
        String employerId = uid;
        // Business Rule: Standard workers or shift leaders must await direct approval from a designated MANAGER
        if (!"MANAGER".equals(role)) {
            employerId = "PENDING";
        }

        // Construct the immutable enterprise user object architecture template container instance
        User newUser = new User(uid, email, name, role, employerId);

        mFirestore.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "פרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();

                    // Clear the backstack mapping to prevent navigation looping issues upon successful profiles synchronization
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    /**
     * Helper mapping engine to convert localization localized Hebrew labels into standardized uppercase database roles.
     */
    private String mapLabelToRole(String label) {
        switch (label) {
            case "ראש משמרת": return "SHIFT_LEADER";
            case "מנהל מחסן": return "MANAGER";
            default: return "WORKER";
        }
    }

    /**
     * Strict verification filtering regex to enforce baseline application credentials validation rules.
     */
    private boolean isValidPassword(String password) {
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }
}