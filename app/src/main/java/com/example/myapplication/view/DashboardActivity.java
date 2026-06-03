package com.example.myapplication.view;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.User;
import com.example.myapplication.viewmodel.ItemViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private ItemViewModel itemViewModel;
    private TextView textTotalItems, textTotalValue, textFilterStatus;
    private TransactionAdapter adapter;
    private String warehouseId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // UI Initialization
        textTotalItems = findViewById(R.id.text_total_items);
        textTotalValue = findViewById(R.id.text_total_value);
        textFilterStatus = findViewById(R.id.text_filter_status);
        EditText editSearch = findViewById(R.id.edit_search_dashboard);
        Button buttonDatePicker = findViewById(R.id.button_date_picker);
        ImageButton buttonClear = findViewById(R.id.button_clear_filter);
        
        RecyclerView recyclerView = findViewById(R.id.recycler_transactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter();
        recyclerView.setAdapter(adapter);

        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load Warehouse context from Profile
        itemViewModel.getUserProfile(userId).observe(this, user -> {
            if (user != null) {
                this.warehouseId = user.getEmployerId();
                initializeDashboard();
            }
        });

        // 4. Clear Filter Logic
        buttonClear.setOnClickListener(v -> {
            editSearch.setText("");
            if (warehouseId != null) loadAllTransactions();
        });
    }

    private void initializeDashboard() {
        if (warehouseId == null) return;

        // 1. Initial Load
        loadAllTransactions();
        
        itemViewModel.getTotalItemsCount(warehouseId).observe(this, count -> {
            int finalCount = (count != null) ? count : 0;
            textTotalItems.setText(String.format(Locale.getDefault(), "סהכ פריטים: %d", finalCount));
        });

        itemViewModel.getTotalInventoryValue(warehouseId).observe(this, value -> {
            double finalValue = (value != null) ? value : 0.0;
            textTotalValue.setText(String.format(Locale.getDefault(), "שווי כולל: ₪%.2f", finalValue));
        });

        // 2. Search Logic (Updated to use warehouseId)
        EditText editSearch = findViewById(R.id.edit_search_dashboard);
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                if (query.isEmpty()) {
                    loadAllTransactions();
                } else {
                    itemViewModel.searchTransactions(warehouseId, query).observe(DashboardActivity.this, transactions -> {
                        if (transactions != null) {
                            adapter.setTransactions(transactions);
                            textFilterStatus.setText("תוצאות חיפוש עבור: " + query);
                        }
                    });
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 3. Date Range Picker Logic (Updated to use warehouseId)
        Button buttonDatePicker = findViewById(R.id.button_date_picker);
        buttonDatePicker.setOnClickListener(v -> {
            MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText("בחר טווח תאריכים")
                    .build();

            picker.show(getSupportFragmentManager(), "date_picker");

            picker.addOnPositiveButtonClickListener(selection -> {
                long start = selection.first;
                long rawEnd = selection.second;
                long queryEnd = rawEnd + 86399999;

                itemViewModel.getTransactionsByDateRange(warehouseId, start, queryEnd).observe(this, transactions -> {
                    if (transactions != null) {
                        adapter.setTransactions(transactions);
                        String status = "מציג מ-" + dateFormat.format(new Date(start)) + " עד " + dateFormat.format(new Date(rawEnd));
                        textFilterStatus.setText(status);
                    }
                });
            });
        });

        // 5. Cloud & Local Cleanup
        itemViewModel.cleanOldTransactions(30, warehouseId);
    }

    private void loadAllTransactions() {
        if (warehouseId == null) return;
        itemViewModel.getAllTransactions(warehouseId).observe(this, transactions -> {
            if (transactions != null) {
                adapter.setTransactions(transactions);
                textFilterStatus.setText("מציג את כל הפעילויות");
            }
        });
    }
}
