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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.R;
import com.example.myapplication.model.Item;
import com.example.myapplication.model.ProductTemplate;
import com.example.myapplication.model.User;
import com.example.myapplication.viewmodel.ItemViewModel;
import com.google.firebase.auth.FirebaseAuth;

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

        // Always start sync on app open
        viewModel.startSync(uid);

        // REACTIVE: Listen to Profile
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

        // UI observes Room exclusively
        viewModel.getInventory(warehouseId).observe(this, items -> {
            if (items != null) adapter.setItems(items);
        });

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
                // Create a clean copy to ensure DiffUtil and Room detect the change properly
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
                Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
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

        MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        
        if (searchView != null) {
            searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) { return false; }

                @Override
                public boolean onQueryTextChange(String newText) {
                    performSearch(newText);
                    return true;
                }
            });
        }

        return true;
    }

    private void performSearch(String query) {
        if (currentUser == null) return;
        String warehouseId = currentUser.getEmployerId();
        if (warehouseId == null) return;

        // Reactive search observe
        viewModel.search(warehouseId, query).observe(this, items -> {
            if (items != null) adapter.setItems(items);
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_dashboard) startActivity(new Intent(this, DashboardActivity.class));
        else if (id == R.id.action_catalog) startActivity(new Intent(this, CatalogActivity.class));
        else if (id == R.id.action_reports) startActivity(new Intent(this, ReportsActivity.class));
        else if (id == R.id.action_manage_users) startActivity(new Intent(this, ManageUsersActivity.class));
        else if (id == R.id.action_logout) viewModel.logoutAndReset(() -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
        return super.onOptionsItemSelected(item);
    }
}
