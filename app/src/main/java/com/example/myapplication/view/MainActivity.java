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

public class MainActivity extends AppCompatActivity {

    private ItemViewModel viewModel;
    private ItemAdapter adapter;
    private AutoCompleteTextView autoTextName;
    private EditText editTextSku, editTextQuantity, editTextPrice, editTextBrand;
    private Button buttonAdd;
    private User currentUser;
    private String uid;

    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;
    private boolean isSearching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { finish(); return; }

        viewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        initUI();
        setupStableStreams();

        viewModel.startSync(uid);

        viewModel.getUserProfile(uid).observe(this, user -> {
            this.currentUser = user;
            if (user != null) {
                invalidateOptionsMenu();
                String warehouseId = user.getEmployerId();
                
                // Fix C1: Improved Pending State Feedback
                if ("PENDING".equals(warehouseId)) {
                    buttonAdd.setEnabled(false);
                    buttonAdd.setText(R.string.pending_approval);
                    adapter.setItems(new java.util.ArrayList<>()); // Show empty while pending
                } else {
                    buttonAdd.setEnabled(true);
                    buttonAdd.setText(R.string.button_add);
                    viewModel.setWarehouseContext(warehouseId);
                }
            }
        });
    }

    private void setupStableStreams() {
        // Fix H1: Prevent inventoryStream from overriding search results
        viewModel.getInventoryStream().observe(this, items -> {
            if (!isSearching && items != null) adapter.setItems(items);
        });

        viewModel.getSearchStream().observe(this, items -> {
            if (isSearching && items != null) adapter.setItems(items);
        });

        viewModel.getTemplateStream().observe(this, templates -> {
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
                viewModel.updateItemQuantity(item, newQuantity - item.getQuantity());
            }

            @Override
            public void onDeleteClick(Item item) {
                if (currentUser != null && "WORKER".equals(currentUser.getRole())) {
                    Toast.makeText(MainActivity.this, R.string.no_permission_delete, Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(getString(R.string.delete_confirm_msg, item.getName()))
                            .setPositiveButton(R.string.button_reset, (d, w) -> viewModel.delete(item))
                            .setNegativeButton(android.R.string.cancel, null).show();
                }
            }
        });

        buttonAdd.setOnClickListener(v -> {
            String name = autoTextName.getText().toString().trim();
            String qtyStr = editTextQuantity.getText().toString().trim();
            String priceStr = editTextPrice.getText().toString().trim();

            if (name.isEmpty() || qtyStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, R.string.error_fill_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int qty = Integer.parseInt(qtyStr);
                double price = Double.parseDouble(priceStr);
                
                // Fix C3: Validation for negative numbers
                if (qty < 0 || price < 0) {
                    Toast.makeText(this, "נא להזין ערכים חיוביים בלבד", Toast.LENGTH_SHORT).show();
                    return;
                }

                String ownerId = (currentUser != null) ? currentUser.getEmployerId() : uid;
                Item newItem = new Item(name, price, qty, ownerId, editTextSku.getText().toString().trim(), editTextBrand.getText().toString().trim());
                viewModel.insert(newItem);
                clearInputs();
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.error_invalid_numbers, Toast.LENGTH_SHORT).show();
            }
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
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
        isSearching = !query.isEmpty();
        
        searchRunnable = () -> {
            viewModel.setSearchQuery(query);
            if (query.isEmpty()) {
                // If query cleared, force-refresh from main stream
                viewModel.getInventoryStream().getValue(); 
            }
        };
        searchHandler.postDelayed(searchRunnable, 300);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Fix H2: Cancel pending search on pause
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
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
        } else {
            // Fix L3: Hide restricted items until user profile is loaded
            menu.findItem(R.id.action_manage_users).setVisible(false);
            menu.findItem(R.id.action_reports).setVisible(false);
            menu.findItem(R.id.action_catalog).setVisible(false);
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
                .setTitle(R.string.menu_logout)
                .setMessage("האם להתנתק מהמערכת?")
                .setPositiveButton(R.string.menu_logout, (d, w) -> viewModel.logoutOnly(this::navigateToLogin))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void showResetDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        boolean isGoogleUser = false;
        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
            if (profile.getProviderId().equals("google.com")) { isGoogleUser = true; break; }
        }

        if (isGoogleUser) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.reset_dialog_title)
                    .setMessage(R.string.reset_dialog_msg)
                    .setPositiveButton(R.string.reauth_google, (d, w) -> reauthenticateGoogleAndReset())
                    .setNegativeButton(android.R.string.cancel, null).show();
        } else {
            final EditText input = new EditText(this);
            input.setHint(R.string.password_hint);
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.menu_reset)
                    .setMessage(R.string.reset_pwd_msg)
                    .setView(input)
                    .setPositiveButton(R.string.button_reset, (d, w) -> {
                        String password = input.getText().toString().trim();
                        if (!password.isEmpty()) verifyAndReset(password);
                        else Toast.makeText(this, R.string.password_hint, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(android.R.string.cancel, null).show();
        }
    }

    private void reauthenticateGoogleAndReset() {
        androidx.credentials.CredentialManager credentialManager = androidx.credentials.CredentialManager.create(this);
        com.google.android.libraries.identity.googleid.GetGoogleIdOption googleIdOption = 
            new com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();
        androidx.credentials.GetCredentialRequest request = new androidx.credentials.GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build();
        credentialManager.getCredentialAsync(this, request, new android.os.CancellationSignal(), androidx.core.content.ContextCompat.getMainExecutor(this),
                new androidx.credentials.CredentialManagerCallback<androidx.credentials.GetCredentialResponse, androidx.credentials.exceptions.GetCredentialException>() {
                    @Override public void onResult(androidx.credentials.GetCredentialResponse result) {
                        try {
                            com.google.android.libraries.identity.googleid.GoogleIdTokenCredential gitc = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(result.getCredential().getData());
                            AuthCredential cred = com.google.firebase.auth.GoogleAuthProvider.getCredential(gitc.getIdToken(), null);
                            FirebaseAuth.getInstance().getCurrentUser().reauthenticate(cred).addOnCompleteListener(t -> {
                                if (t.isSuccessful()) viewModel.logoutAndReset(MainActivity.this::navigateToLogin, MainActivity.this::showError);
                            });
                        } catch (Exception ignored) {}
                    }
                    @Override public void onError(androidx.credentials.exceptions.GetCredentialException e) { Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show(); }
                });
    }

    private void verifyAndReset(String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        AuthCredential cred = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(cred).addOnCompleteListener(task -> {
            if (task.isSuccessful()) viewModel.logoutAndReset(this::navigateToLogin, this::showError);
            else Toast.makeText(this, R.string.error_auth_failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void showError(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
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
