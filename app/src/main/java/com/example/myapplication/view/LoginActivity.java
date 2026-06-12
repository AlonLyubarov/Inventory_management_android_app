package com.example.myapplication.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.myapplication.R;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and the modern Jetpack Credential Manager
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        // Initialize UI components
        editTextEmail = findViewById(R.id.edit_text_email);
        editTextPassword = findViewById(R.id.edit_text_password);
        Button buttonLogin = findViewById(R.id.button_login);
        com.google.android.gms.common.SignInButton buttonGoogleLogin = findViewById(R.id.button_google_login);
        TextView textViewRegister = findViewById(R.id.text_view_register);

        // Standard Email & Password Login Flow
        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            navigateToMain();
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Google Sign-In Flow Trigger
        buttonGoogleLogin.setOnClickListener(v -> {
            signInWithGoogle();
        });

        // Navigate to Registration Screen
        textViewRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Configures and launches the Credential Manager bottom sheet to authenticate via Google.
     */
    private void signInWithGoogle() {
        // 1. Configure the Google ID token option using the Web Client ID linked to Firebase
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Show all Google accounts signed-in on the device
                .setServerClientId(getString(R.string.default_web_client_id)) // Extracted automatically from google-services.json
                .setAutoSelectEnabled(true) // Enables silent/automatic sign-in for returning users
                .build();

        // 2. Build the aggregate credential request wrapper
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // 3. Define the main thread executor so callback responses can update the UI directly
        Executor mainExecutor = ContextCompat.getMainExecutor(this);

        // 4. Fire the asynchronous request using the appropriate Java compatibility layer API
        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(), // Allows optional operation cancellation
                mainExecutor,
                new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        // Successfully retrieved credentials from the local bottom sheet flow
                        handleSignInResult(result);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        // User dismissed the prompt, canceled, or configuration mismatch occurred
                        Toast.makeText(LoginActivity.this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Extracts the raw ID token from the CustomCredential container returned by Credential Manager.
     */
    private void handleSignInResult(GetCredentialResponse result) {
        Credential credential = result.getCredential();

        // Modern Google ID tokens wrapper structure inside Jetpack Credential Manager is represented as a CustomCredential
        if (credential instanceof CustomCredential &&
                credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {

            try {
                // Parse out the nested ID token values from the raw Bundle payload data securely
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                String idToken = googleIdTokenCredential.getIdToken();

                // Pass the acquired OAuth Web Token down to Firebase authentication server
                firebaseAuthWithGoogle(idToken);
            } catch (Exception e) {
                Log.e("Login", "Error parsing Google ID Token structural bundle data", e);
                Toast.makeText(this, "Failed to process Google Account payload logs", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Authenticates the validated Google ID Token directly against Firebase Authentication backend services.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        // Bind the ID Token string data to an actual Firebase credential object instance
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Seamless sign-in completed! Forward user to the primary app workspace
                        navigateToMain();
                    } else {
                        Toast.makeText(LoginActivity.this, "Firebase cloud synchronization with Google token failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Redirects the user to the MainActivity and removes the Login page from the backstack.
     */
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Finish current context so back button does not reopen login screen
    }
}