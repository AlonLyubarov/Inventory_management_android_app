package com.example.myapplication.view;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.R;
import com.example.myapplication.model.Item;
import com.example.myapplication.model.ProductTemplate;
import com.example.myapplication.model.User;
import com.example.myapplication.viewmodel.ItemViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ItemViewModel itemViewModel;
    private ItemAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AutoCompleteTextView autoTextName;
    private EditText editTextSku, editTextQuantity, editTextPrice, editTextBrand;
    private Button buttonAdd;
    private int selectedThreshold = 0;
    private User currentUser;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser fbUser = mAuth.getCurrentUser();

        if (fbUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentUserId = fbUser.getUid();

        // Initialize Jetpack Credential Manager instance
        credentialManager = CredentialManager.create(this);
        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);

        // UI references
        autoTextName = findViewById(R.id.edit_text_name);
        editTextSku = findViewById(R.id.edit_text_sku);
        editTextBrand = findViewById(R.id.edit_text_brand);
        editTextQuantity = findViewById(R.id.edit_text_quantity);
        editTextPrice = findViewById(R.id.edit_text_price);
        buttonAdd = findViewById(R.id.button_add);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        adapter = new ItemAdapter();
        recyclerView.setAdapter(adapter);

        // Initial Sync
        itemViewModel.syncFromCloud(currentUserId);

        // DYNAMIC PROFILE & INVENTORY LOGIC
        itemViewModel.getUserProfile(currentUserId).observe(this, user -> {
            this.currentUser = user;
            if (user != null) {
                applyPermissions(user.getRole());

                String warehouseId = user.getEmployerId();
                if ("PENDING".equals(warehouseId)) {
                    buttonAdd.setEnabled(false);
                    buttonAdd.setText("ממתין לאישור מנהל...");
                    adapter.setItems(new ArrayList<>());
                } else {
                    buttonAdd.setEnabled(true);
                    buttonAdd.setText("הוסף פריט");

                    // Observe items for the SPECIFIC warehouse this user belongs to
                    itemViewModel.getAllItems(warehouseId).observe(this, items -> {
                        if (items != null) adapter.setItems(items);
                    });

                    // Update autocomplete from shared templates
                    itemViewModel.getAllTemplates(warehouseId).observe(this, templates -> {
                        if (templates != null) {
                            ArrayAdapter<ProductTemplate> autocompAdapter = new ArrayAdapter<>(
                                    this, android.R.layout.simple_dropdown_item_1line, templates);
                            autoTextName.setAdapter(autocompAdapter);
                        }
                    });
                }
            } else {
                checkCloudProfile(currentUserId);
            }
        });

        autoTextName.setOnItemClickListener((parent, view, position, id) -> {
            ProductTemplate selected = (ProductTemplate) parent.getItemAtPosition(position);
            fillFormFromTemplate(selected);
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            itemViewModel.syncFromCloud(currentUserId);
            swipeRefreshLayout.postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
        });

        adapter.setOnItemClickListener(new ItemAdapter.OnItemClickListener() {
            @Override
            public void onQuantityChange(Item item, int newQuantity) {
                // Capture the authentic current state before any allocation modifications
                int oldQuantity = item.getQuantity();

                // CRITICAL FIX: Create a shallow clone/copy constructor layout instead of mutating the live reference.
                // This ensures Room and DiffUtil can distinctively see the state variance between old and new lists.
                Item updatedItem = new Item(item.getName(), item.getPrice(), newQuantity, item.getOwnerId(), item.getSku(), item.getBrand());

                // Preserve all internal database/cloud tracking identifiers
                updatedItem.setId(item.getId());
                updatedItem.setFirestoreId(item.getFirestoreId());
                updatedItem.setLowStockThreshold(item.getLowStockThreshold());

                // Dispatch the clean detached model instance directly to the architecture pipeline
                itemViewModel.update(updatedItem, oldQuantity);
            }

            @Override
            public void onDeleteClick(Item item) {
                if (currentUser != null && "WORKER".equals(currentUser.getRole())) {
                    Toast.makeText(MainActivity.this, "אין לך הרשאה למחוק פריטים", Toast.LENGTH_SHORT).show();
                } else {
                    showDeleteConfirmationDialog(item);
                }
            }
        });

        buttonAdd.setOnClickListener(v -> {
            String name = autoTextName.getText().toString();
            String sku = editTextSku.getText().toString();
            String brand = editTextBrand.getText().toString();
            String quantityStr = editTextQuantity.getText().toString();
            String priceStr = editTextPrice.getText().toString();

            if (name.trim().isEmpty() || quantityStr.trim().isEmpty() || priceStr.trim().isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int quantity = Integer.parseInt(quantityStr);
            double price = Double.parseDouble(priceStr);

            // Use warehouse context for adding items
            String ownerId = (currentUser != null) ? currentUser.getEmployerId() : currentUserId;
            Item newItem = new Item(name, price, quantity, ownerId, sku, brand);
            newItem.setLowStockThreshold(selectedThreshold);
            itemViewModel.insert(newItem);

            // Clear inputs
            autoTextName.setText("");
            editTextSku.setText("");
            editTextBrand.setText("");
            editTextQuantity.setText("");
            editTextPrice.setText("");
            selectedThreshold = 0;
        });
    }

    private void checkCloudProfile(String currentUserId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "יש להשלים את הגדרת הפרופיל", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void applyPermissions(String role) {
        invalidateOptionsMenu();
    }

    private void fillFormFromTemplate(ProductTemplate template) {
        autoTextName.setText(template.getName());
        editTextSku.setText(template.getSku());
        editTextPrice.setText(String.valueOf(template.getDefaultPrice()));
        editTextBrand.setText(template.getBrand());
        selectedThreshold = template.getLowStockThreshold();
        editTextQuantity.requestFocus();
    }

    private void showDeleteConfirmationDialog(Item item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete " + item.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    itemViewModel.delete(item);
                    Toast.makeText(MainActivity.this, "Item deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (currentUser != null) {
            String role = currentUser.getRole();
            if ("WORKER".equals(role)) {
                menu.findItem(R.id.action_reports).setVisible(false);
                menu.findItem(R.id.action_catalog).setVisible(false);
                menu.findItem(R.id.action_manage_users).setVisible(false);
            } else if ("SHIFT_LEADER".equals(role)) {
                menu.findItem(R.id.action_reports).setVisible(false);
                menu.findItem(R.id.action_manage_users).setVisible(false);
            } else if ("MANAGER".equals(role)) {
                menu.findItem(R.id.action_manage_users).setVisible(true);
            }
        }
        MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                searchDatabase(newText);
                return true;
            }
        });
        return true;
    }

    private void searchDatabase(String query) {
        if (currentUser == null) return;
        String searchQuery = "%" + query + "%";
        itemViewModel.searchDatabase(searchQuery).observe(this, items -> {
            if (items != null) {
                adapter.setItems(items);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_dashboard) {
            startActivity(new Intent(this, DashboardActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_catalog) {
            startActivity(new Intent(this, CatalogActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_reports) {
            startActivity(new Intent(this, ReportsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_manage_users) {
            startActivity(new Intent(this, ManageUsersActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            showLogoutDialog();
            return true;
        } else if (item.getItemId() == R.id.action_reset) {
            showResetWithPasswordDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם ברצונך להתנתק מהחשבון?")
                .setPositiveButton("התנתק", (dialog, which) -> {
                    itemViewModel.logoutOnly(() -> {
                        runOnUiThread(() -> {
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * Contextually builds the verification alert. If a Google provider is present,
     * it directly invokes the OAuth flow rather than displaying a useless password field.
     */
    private void showResetWithPasswordDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        boolean isGoogleUser = false;
        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
            if (profile.getProviderId().equals("google.com")) {
                isGoogleUser = true;
                break;
            }
        }

        // Contextual branching: If user is Google-backed, skip text dialog setup completely
        if (isGoogleUser) {
            new AlertDialog.Builder(this)
                    .setTitle("איפוס משתמש סופי")
                    .setMessage("כדי למחוק את הפרופיל שלך מהענן, עליך לבצע אימות קצר מול גוגל.")
                    .setPositiveButton("המשך לאימות", (dialog, which) -> verifyAndPerformReset(""))
                    .setNegativeButton("ביטול", null)
                    .show();
        } else {
            final EditText passwordInput = new EditText(this);
            passwordInput.setHint("הזן את סיסמת החשבון שלך");
            passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

            new AlertDialog.Builder(this)
                    .setTitle("איפוס משתמש סופי")
                    .setMessage("כדי למחוק את הפרופיל שלך מהענן ולהירשם מחדש, הזן את סיסמת ההתחברות שלך:")
                    .setView(passwordInput)
                    .setPositiveButton("אמת ואפס", (dialog, which) -> {
                        String pwd = passwordInput.getText().toString();
                        if (pwd.isEmpty()) {
                            Toast.makeText(this, "נא להזין סיסמה", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        verifyAndPerformReset(pwd);
                    })
                    .setNegativeButton("ביטול", null)
                    .show();
        }
    }

    /**
     * Verifies the user's identity based on their sign-in provider before performing a sensitive deep reset operation.
     * If the user is a Google user, it triggers the Credential Manager re-authentication flow.
     * If the user is a standard email/password user, it validates the provided password string.
     *
     * @param passwordIfEmailUser The password entered by the user (applicable only for email/password accounts).
     */
    private void verifyAndPerformReset(String passwordIfEmailUser) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Flag to determine if the currently logged-in account linked its primary provider via Google
        boolean isGoogleUser = false;

        // Iterate through all linked provider profiles associated with this Firebase account
        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
            if (profile.getProviderId().equals("google.com")) {
                isGoogleUser = true;
                break;
            }
        }

        if (isGoogleUser) {
            // Scenario A: Google Sign-In account detected.
            // Google accounts do not store passwords in Firebase. We must prompt the user to re-verify using Credential Manager.
            Toast.makeText(this, "Please authenticate your Google account...", Toast.LENGTH_SHORT).show();
            reauthenticateGoogleUserAndReset();
        } else {
            // Scenario B: Traditional Email/Password account detected.
            if (passwordIfEmailUser.isEmpty()) {
                Toast.makeText(this, "Please enter your password for verification.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate a standard email password credential wrapper using the provided string
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), passwordIfEmailUser);

            // Execute Firebase cloud re-authentication challenge
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Verification succeeded; execute the secure deep reset routing routine
                    performDeepReset();
                } else {
                    Toast.makeText(this, "Incorrect password! Reset canceled.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Re-authenticates a Google provider user by launching the modern Jetpack Credential Manager bottom sheet.
     * Upon receiving a valid new ID token bundle from Google, it submits it to Firebase to secure the session.
     */
    private void reauthenticateGoogleUserAndReset() {
        // 1. Configure the Google ID option. We set filter criteria to true to focus only on the pre-authorized active account.
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        // 2. Build the structural aggregate credential request container
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // 3. Fire the asynchronous request using the Java compatibility layer API
        credentialManager.getCredentialAsync(
                this,
                request,
                new android.os.CancellationSignal(), // Allows optional task termination management
                androidx.core.content.ContextCompat.getMainExecutor(this), // Forces callback responses to execute on the Main UI Thread
                new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();

                        // Validate that the returned payload conforms to the expected Custom Google ID Token template type
                        if (credential instanceof CustomCredential &&
                                credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {

                            try {
                                // Extract the refreshed authorization ID token string securely from the underlying data Bundle
                                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                                String idToken = googleIdTokenCredential.getIdToken();

                                // Map the newly acquired token to an actual Firebase AuthCredential token object instance
                                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);

                                // Submit the fresh token to Firebase to re-authenticate the current runtime user context session
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                if (currentUser != null) {
                                    currentUser.reauthenticate(firebaseCredential).addOnCompleteListener(reauthTask -> {
                                        if (reauthTask.isSuccessful()) {
                                            // Identity verified successfully via Google OAuth! Proceed with deep clear routines
                                            performDeepReset();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Google server re-authentication failed.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                Log.e("AuthReset", "Error mapping structural Google credentials bundle data payload", e);
                            }
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        // Triggered if the user dismisses the bottom sheet prompt or explicitly cancels the operation
                        Toast.makeText(MainActivity.this, "Re-authentication canceled by user.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    /**
     * Handles the final deep reset phase by removing user metadata from Cloud Firestore,
     * purging the local Room SQLite inventory database caches via ViewModel, and deleting the Auth account records.
     */
    private void performDeepReset() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String uid = user.getUid();
            Toast.makeText(this, "Deleting profile from cloud...", Toast.LENGTH_SHORT).show();

            // 1. Delete the user's profile document inside Cloud Firestore
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .delete()
                    .addOnCompleteListener(firestoreTask -> {

                        // 2. Clear the local Room SQLite inventory data storage using the ViewModel framework pipeline
                        itemViewModel.clearLocalDatabase();
                        Log.d("AuthReset", "Fired asynchronous request to flush Room entities through ViewModel architecture.");

                        // 3. Execute the final account deletion command from Firebase Authentication servers
                        user.delete().addOnCompleteListener(authTask -> {
                            if (authTask.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "Account successfully deleted.", Toast.LENGTH_SHORT).show();

                                // Sign out from active instance token session context
                                FirebaseAuth.getInstance().signOut();

                                // Route back to LoginActivity and flush the memory backstack pipeline
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish(); // Terminate the current MainActivity lifecycle context completely
                            } else {
                                // Triggered if the active operational token expired or structural handshake validation failed
                                Log.e("AuthReset", "Failed to delete user cloud authentication account logs", authTask.getException());
                                Toast.makeText(MainActivity.this, "Error deleting account: " + authTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });

                    });
        }
    }

  }
