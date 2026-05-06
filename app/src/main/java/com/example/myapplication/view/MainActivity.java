package com.example.myapplication.view;
import androidx.appcompat.widget.SearchView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Item;
import com.example.myapplication.viewmodel.ItemViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Global variables (Members) accessible by all methods in this class
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

        // Initialize RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Initialize the global adapter variable
        adapter = new ItemAdapter();
        recyclerView.setAdapter(adapter);

        // Initialize the global ViewModel variable
        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);

        // Observe all items for the current user
        itemViewModel.getAllItems(currentUserId).observe(this, items -> {
            if (items != null) {
                adapter.setItems(items);
            }
        });

        // Swipe to Delete functionality
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Item itemToDelete = adapter.getItemAt(position);
                itemViewModel.delete(itemToDelete);
                Toast.makeText(MainActivity.this, "Item deleted", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(recyclerView);

        // Logic for adding a new item
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

            editTextName.setText("");
            editTextQuantity.setText("");
            editTextPrice.setText("");
            Toast.makeText(this, "Item saved", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            // You can also redirect to Login screen here
        }
    }

    /**
     * Filters the database based on the search query.
     */
    private void searchDatabase(String query) {
        String searchQuery = "%" + query + "%";

        // Calling the method through the initialized itemViewModel instance
        itemViewModel.searchDatabase(searchQuery).observe(this, items -> {
            if (items != null) {
                adapter.setItems(items);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();

        // Listener for search input changes
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Execute search logic on every keystroke
                searchDatabase(newText);
                return true;
            }
        });

        return true;
    }
}