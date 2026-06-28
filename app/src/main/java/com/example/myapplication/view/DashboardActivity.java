package com.example.myapplication.view;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Transaction;
import com.example.myapplication.viewmodel.ItemViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private ItemViewModel viewModel;
    private TextView textTotalItems, textTotalValue, textFilterStatus;
    private TransactionAdapter adapter;
    private String warehouseId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { finish(); return; }

        viewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        initUI();
        setupDashboardStreams();

        viewModel.getUserProfile(uid).observe(this, user -> {
            if (user != null) {
                this.warehouseId = user.getEmployerId();
                viewModel.setWarehouseContext(warehouseId);
                observeStats();
            }
        });
    }

    private void setupDashboardStreams() {
        // Fix B-09: Observe the reactive dashboard search stream (handled in ViewModel)
        viewModel.getDashboardSearchStream().observe(this, logs -> {
            if (logs != null) adapter.setTransactions(logs);
        });
    }

    private void observeStats() {
        if (warehouseId == null) return;
        viewModel.getTotalItemsCount(warehouseId).observe(this, count -> 
                textTotalItems.setText(String.valueOf(count != null ? count : 0)));
        viewModel.getTotalInventoryValue(warehouseId).observe(this, value -> 
                textTotalValue.setText(String.format(Locale.getDefault(), "₪%.2f", value != null ? value : 0.0)));
    }

    private void initUI() {
        textTotalItems = findViewById(R.id.text_total_items);
        textTotalValue = findViewById(R.id.text_total_value);
        textFilterStatus = findViewById(R.id.text_filter_status);
        EditText editSearch = findViewById(R.id.edit_search_dashboard);
        ImageButton buttonClear = findViewById(R.id.button_clear_filter);
        
        RecyclerView recyclerView = findViewById(R.id.recycler_transactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter();
        recyclerView.setAdapter(adapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString();
                viewModel.setDashboardSearchQuery(q);
                if (!q.isEmpty()) textFilterStatus.setText("Search: " + q);
                else textFilterStatus.setText("Showing all activities");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.button_date_picker).setOnClickListener(v -> {
            MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker().build();
            picker.show(getSupportFragmentManager(), "date_picker");
            picker.addOnPositiveButtonClickListener(sel -> {
                // B-09 Fix: State managed in ViewModel now
                viewModel.setDashboardDateRange(sel.first, sel.second + 86400000);
                String range = dateFormat.format(new Date(sel.first)) + " - " + dateFormat.format(new Date(sel.second));
                textFilterStatus.setText("Range: " + range);
            });
        });

        buttonClear.setOnClickListener(v -> {
            editSearch.setText("");
            textFilterStatus.setText("Showing all activities");
            viewModel.setDashboardSearchQuery("");
            viewModel.setDashboardDateRange(null, null); // Reset B-05 Fix
        });
    }
}
