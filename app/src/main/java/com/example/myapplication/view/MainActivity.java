package com.example.myapplication.view;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Item;
import com.example.myapplication.viewmodel.ItemViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ItemViewModel itemViewModel;
    private ItemAdapter adapter;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Redirect to Login if not authenticated
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentUserId = currentUser.getUid();

        // UI Component references
        EditText editTextName = findViewById(R.id.edit_text_name);
        EditText editTextQuantity = findViewById(R.id.edit_text_quantity);
        EditText editTextPrice = findViewById(R.id.edit_text_price);
        Button buttonAdd = findViewById(R.id.button_add);

        // Setup RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        adapter = new ItemAdapter();
        recyclerView.setAdapter(adapter);

        // Initialize ViewModel
        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);

        // Handle quantity changes and deletion from the adapter
        adapter.setOnItemClickListener(new ItemAdapter.OnItemClickListener() {
            @Override
            public void onQuantityChange(Item item, int newQuantity) {
                int oldQuantity = item.getQuantity();
                item.setQuantity(newQuantity);
                itemViewModel.update(item, oldQuantity);
            }

            @Override
            public void onDeleteClick(Item item) {
                showDeleteConfirmationDialog(item);
            }
        });

        // Observe items from the database
        itemViewModel.getAllItems(currentUserId).observe(this, items -> {
            if (items != null) {
                adapter.setItems(items);
            }
        });

        // Add item button logic
        buttonAdd.setOnClickListener(v -> {
            String name = editTextName.getText().toString();
            String quantityStr = editTextQuantity.getText().toString();
            String priceStr = editTextPrice.getText().toString();

            if (name.trim().isEmpty() || quantityStr.trim().isEmpty() || priceStr.trim().isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int quantity = Integer.parseInt(quantityStr);
            double price = Double.parseDouble(priceStr);

            Item newItem = new Item(name, price, quantity, currentUserId);
            itemViewModel.insert(newItem);

            // Clear inputs after success
            editTextName.setText("");
            editTextQuantity.setText("");
            editTextPrice.setText("");
            Toast.makeText(this, "Item saved successfully", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Shows a confirmation dialog before deleting an item.
     */
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
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Executes search query against the database.
     */
    private void searchDatabase(String query) {
        String searchQuery = "%" + query + "%";
        itemViewModel.searchDatabase(searchQuery).observe(this, items -> {
            if (items != null) {
                adapter.setItems(items);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_dashboard) {
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
