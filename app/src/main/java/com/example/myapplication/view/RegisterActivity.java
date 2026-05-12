package com.example.myapplication.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.User;
import com.example.myapplication.model.AppDatabase;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth instance
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        editTextEmail = findViewById(R.id.edit_text_email_reg);
        editTextPassword = findViewById(R.id.edit_text_password_reg);
        Button buttonRegister = findViewById(R.id.button_register);

        // Set click listener for the register button
        buttonRegister.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            // Basic validation for empty fields
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
                return;
            }

            // Strong password validation
            if (!isValidPassword(password)) {
                Toast.makeText(this, "הסיסמה חייבת להיות באורך 8 תווים לפחות, ולכלול אות גדולה, אות קטנה, מספר ותו מיוחד", Toast.LENGTH_LONG).show();
                return;
            }

            // Step 1: Create user in Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Step 2: Get the unique User ID (UID) and Email from the newly created Firebase user
                            String uid = mAuth.getCurrentUser().getUid();
                            String userEmail = mAuth.getCurrentUser().getEmail();

                            // Step 3: Create a User entity object for Room Database
                            User newUser = new User(uid, userEmail);

                            // Step 4: Save the user to the local Room Database on a background thread
                            new Thread(() -> {
                                AppDatabase.getDatabase(getApplicationContext()).userDao().insert(newUser);
                            }).start();

                            // Show success message and navigate to MainActivity
                            Toast.makeText(RegisterActivity.this, "ההרשמה הצליחה!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));

                            // Close RegisterActivity so the user cannot navigate back to it
                            finish();
                        } else {
                            // If registration fails, display the error message from Firebase
                            Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private boolean isValidPassword(String password) {
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }
}
