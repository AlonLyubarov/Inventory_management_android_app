package com.example.myapplication.view;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import android.content.Intent;
import android.os.Bundle;
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
                int oldQuantity = item.getQuantity();
                item.setQuantity(newQuantity);
                itemViewModel.update(item, oldQuantity);
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

    private void showResetWithPasswordDialog() {
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
                    verifyPasswordAndReset(pwd);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void verifyPasswordAndReset(String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        com.google.firebase.auth.AuthCredential credential = 
            com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), password);

        Toast.makeText(this, "מאמת סיסמה...", Toast.LENGTH_SHORT).show();
        
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                performDeepReset();
            } else {
                Toast.makeText(this, "סיסמה שגויה! האיפוס בוטל.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performDeepReset() {
        Toast.makeText(this, "מבצע איפוס, נא להמתין...", Toast.LENGTH_SHORT).show();
        itemViewModel.logoutAndReset(() -> {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "האיפוס הושלם. נא להירשם מחדש.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }
}
