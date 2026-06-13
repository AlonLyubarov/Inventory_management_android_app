package com.example.myapplication.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Item;
import com.example.myapplication.model.ProductTemplate;
import com.example.myapplication.model.User;
import com.example.myapplication.viewmodel.ItemViewModel;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ItemViewModel viewModel;
    private ItemAdapter adapter;
    private AutoCompleteTextView autoTextName;
    private EditText editTextSku, editTextQuantity, editTextPrice, editTextBrand;
    private Button buttonAdd;
    private User currentUser;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { finish(); return; }

        viewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        initUI();

        // Start reactive sync
        viewModel.startSync(uid);

        // REACTIVE: Listen to Profile changes
        viewModel.getUserProfile(uid).observe(this, user -> {
            this.currentUser = user;
            if (user != null) {
                invalidateOptionsMenu();
                setupInventoryStream(user.getEmployerId());
            }
        });
    }

    private void setupInventoryStream(String warehouseId) {
        if (warehouseId == null || "PENDING".equals(warehouseId)) {
            buttonAdd.setEnabled(false);
            buttonAdd.setText("ממתין לאישור...");
            adapter.setItems(new ArrayList<>());
            return;
        }

        buttonAdd.setEnabled(true);
        buttonAdd.setText("הוסף למלאי");

        // Reactively observe inventory list
        viewModel.getInventory(warehouseId).observe(this, items -> {
            if (items != null) {
                adapter.setItems(items);
            }
        });

        // Reactively observe templates for autocomplete
        viewModel.getTemplates(warehouseId).observe(this, templates -> {
            if (templates != null) {
                ArrayAdapter<ProductTemplate> autocompAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_dropdown_item_1line, templates);
                autoTextName.setAdapter(autocompAdapter);
            }
        });
    }

    private void initUI() {
        autoTextName = findViewById(R.id.edit_text_name);
        editTextSku = findViewById(R.id.edit_text_sku);
        editTextBrand = findViewById(R.id.edit_text_brand);
        editTextQuantity = findViewById(R.id.edit_text_quantity);
        editTextPrice = findViewById(R.id.edit_text_price);
        buttonAdd = findViewById(R.id.button_add);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new ItemAdapter.OnItemClickListener() {
            @Override
            public void onQuantityChange(Item item, int newQuantity) {
                int old = item.getQuantity();
                // Create a shallow copy to ensure DiffUtil detects the change
                Item updated = new Item(item.getName(), item.getPrice(), newQuantity, item.getOwnerId(), item.getSku(), item.getBrand());
                updated.setId(item.getId());
                updated.setFirestoreId(item.getFirestoreId());
                updated.setLowStockThreshold(item.getLowStockThreshold());
                viewModel.update(updated, old);
            }

            @Override
            public void onDeleteClick(Item item) {
                if (currentUser != null && "WORKER".equals(currentUser.getRole())) {
                    Toast.makeText(MainActivity.this, "אין הרשאה למחיקה", Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("למחוק את " + item.getName() + "?")
                            .setPositiveButton("מחק", (d, w) -> viewModel.delete(item))
                            .setNegativeButton("ביטול", null).show();
                }
            }
        });

        buttonAdd.setOnClickListener(v -> {
            String name = autoTextName.getText().toString();
            String qty = editTextQuantity.getText().toString();
            String price = editTextPrice.getText().toString();
            if (name.isEmpty() || qty.isEmpty() || price.isEmpty()) {
                Toast.makeText(this, "מלא את כל השדות", Toast.LENGTH_SHORT).show();
                return;
            }

            String ownerId = (currentUser != null) ? currentUser.getEmployerId() : uid;
            Item newItem = new Item(name, Double.parseDouble(price), Integer.parseInt(qty), 
                    ownerId, editTextSku.getText().toString(), editTextBrand.getText().toString());
            viewModel.insert(newItem);
            clearInputs();
        });

        autoTextName.setOnItemClickListener((parent, view, position, id) -> {
            ProductTemplate t = (ProductTemplate) parent.getItemAtPosition(position);
            autoTextName.setText(t.getName());
            editTextSku.setText(t.getSku());
            editTextPrice.setText(String.valueOf(t.getDefaultPrice()));
            editTextBrand.setText(t.getBrand());
        });
    }

    private void clearInputs() {
        autoTextName.setText(""); editTextSku.setText(""); editTextBrand.setText("");
        editTextQuantity.setText(""); editTextPrice.setText("");
    }

    private void performSearch(String query) {
        if (currentUser == null) return;
        viewModel.search(currentUser.getEmployerId(), query).observe(this, items -> {
            if (items != null) adapter.setItems(items);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (currentUser != null) {
            String role = currentUser.getRole();
            menu.findItem(R.id.action_manage_users).setVisible("MANAGER".equals(role));
            if ("WORKER".equals(role)) {
                menu.findItem(R.id.action_reports).setVisible(false);
                menu.findItem(R.id.action_catalog).setVisible(false);
            }
        }
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) menu.findItem(R.id.action_search).getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String q) { return false; }
                @Override public boolean onQueryTextChange(String q) { performSearch(q); return true; }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_dashboard) startActivity(new Intent(this, DashboardActivity.class));
        else if (id == R.id.action_catalog) startActivity(new Intent(this, CatalogActivity.class));
        else if (id == R.id.action_reports) startActivity(new Intent(this, ReportsActivity.class));
        else if (id == R.id.action_manage_users) startActivity(new Intent(this, ManageUsersActivity.class));
        else if (id == R.id.action_logout) showLogoutDialog();
        else if (id == R.id.action_reset) showResetDialog();
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם להתנתק מהמערכת?")
                .setPositiveButton("התנתק", (d, w) -> viewModel.logoutOnly(this::navigateToLogin))
                .setNegativeButton("ביטול", null).show();
    }

    private void showResetDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        boolean isGoogleUser = false;
        if (user.getProviderData() != null) {
            for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
                if (profile.getProviderId().equals("google.com")) {
                    isGoogleUser = true;
                    break;
                }
            }
        }

        if (isGoogleUser) {
            new AlertDialog.Builder(this)
                    .setTitle("איפוס משתמש סופי")
                    .setMessage("מכיוון שאתה מחובר דרך גוגל, עליך לבצע אימות קצר כדי לאפס את החשבון.")
                    .setPositiveButton("אמת עם גוגל", (d, w) -> reauthenticateGoogleAndReset())
                    .setNegativeButton("ביטול", null).show();
        } else {
            final EditText input = new EditText(this);
            input.setHint("הזן סיסמה");
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            new AlertDialog.Builder(this)
                    .setTitle("איפוס משתמש")
                    .setMessage("הזן סיסמה למחיקת הפרופיל והרשמה מחדש:")
                    .setView(input)
                    .setPositiveButton("אפס", (d, w) -> {
                        String password = input.getText().toString().trim();
                        if (!password.isEmpty()) {
                            verifyAndReset(password);
                        } else {
                            Toast.makeText(this, "חובה להזין סיסמה", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("ביטול", null).show();
        }
    }

    private void reauthenticateGoogleAndReset() {
        androidx.credentials.CredentialManager credentialManager = androidx.credentials.CredentialManager.create(this);
        com.google.android.libraries.identity.googleid.GetGoogleIdOption googleIdOption = 
            new com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        androidx.credentials.GetCredentialRequest request = new androidx.credentials.GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(this, request, null, 
            androidx.core.content.ContextCompat.getMainExecutor(this),
            new androidx.credentials.CredentialManagerCallback<androidx.credentials.GetCredentialResponse, androidx.credentials.exceptions.GetCredentialException>() {
                @Override
                public void onResult(androidx.credentials.GetCredentialResponse result) {
                    androidx.credentials.Credential credential = result.getCredential();
                    if (credential instanceof androidx.credentials.CustomCredential &&
                        credential.getType().equals(com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                        try {
                            com.google.android.libraries.identity.googleid.GoogleIdTokenCredential googleIdTokenCredential = 
                                com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.getData());
                            AuthCredential fbCred = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdTokenCredential.getIdToken(), null);
                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                FirebaseAuth.getInstance().getCurrentUser().reauthenticate(fbCred).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) viewModel.logoutAndReset(MainActivity.this::navigateToLogin);
                                });
                            }
                        } catch (Exception ignored) {}
                    }
                }
                @Override
                public void onError(androidx.credentials.exceptions.GetCredentialException e) {
                    Toast.makeText(MainActivity.this, "אימות נכשל: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void verifyAndReset(String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        AuthCredential cred = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(cred).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                viewModel.logoutAndReset(this::navigateToLogin);
            } else {
                Toast.makeText(this, "סיסמה שגויה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToLogin() {
        runOnUiThread(() -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
