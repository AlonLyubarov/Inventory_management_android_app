package com.example.myapplication.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.User;
import com.example.myapplication.model.AppDatabase;
import com.google.firebase.auth.FirebaseAuth;
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

        buttonRegister.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim().toLowerCase();
            String password = editTextPassword.getText().toString().trim();
            String name = editTextDisplayName.getText().toString().trim();
            String selectedRoleLabel = spinnerRoles.getSelectedItem().toString();
            String role = mapLabelToRole(selectedRoleLabel);

            if (email.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "נא למלא שם ואימייל", Toast.LENGTH_SHORT).show();
                return;
            }

            // SMART REGISTER: If user is already logged in (auth exists), just save to Firestore
            if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail().equalsIgnoreCase(email)) {
                saveUserToFirestore(mAuth.getUid(), email, name, role);
                return;
            }

            if (password.isEmpty() || !isValidPassword(password)) {
                Toast.makeText(this, "סיסמה לא תקינה", Toast.LENGTH_LONG).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            saveUserToFirestore(mAuth.getCurrentUser().getUid(), email, name, role);
                        } else {
                            if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                                // User exists in Auth, try to sign in then save
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

    private void saveUserToFirestore(String uid, String email, String name, String role) {
        String employerId = uid;
        if (!"MANAGER".equals(role)) {
            employerId = "PENDING";
        }
        User newUser = new User(uid, email, name, role, employerId);
        mFirestore.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "פרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
    }

    private String mapLabelToRole(String label) {
        switch (label) {
            case "ראש משמרת": return "SHIFT_LEADER";
            case "מנהל מחסן": return "MANAGER";
            default: return "WORKER";
        }
    }

    private boolean isValidPassword(String password) {
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }
}
