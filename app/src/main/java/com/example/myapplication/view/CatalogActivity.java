package com.example.myapplication.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.ProductTemplate;
import com.example.myapplication.viewmodel.ItemViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class CatalogActivity extends AppCompatActivity {

    private ItemViewModel viewModel;
    private CatalogAdapter adapter;
    private String warehouseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { finish(); return; }

        viewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        initUI();

        // Reactive: Observe profile to get warehouseId, then observe Catalog
        viewModel.getUserProfile(uid).observe(this, user -> {
            if (user != null) {
                this.warehouseId = user.getEmployerId();
                observeCatalog();
            }
        });
    }

    private void observeCatalog() {
        if (warehouseId == null) return;
        viewModel.getTemplates(warehouseId).observe(this, templates -> {
            if (templates != null) adapter.setTemplates(templates);
        });
    }

    private void initUI() {
        EditText editName = findViewById(R.id.edit_catalog_name);
        EditText editSku = findViewById(R.id.edit_catalog_sku);
        EditText editBrand = findViewById(R.id.edit_catalog_brand);
        EditText editPrice = findViewById(R.id.edit_catalog_price);
        EditText editThreshold = findViewById(R.id.edit_catalog_threshold);
        Button buttonAdd = findViewById(R.id.button_add_to_catalog);

        RecyclerView recyclerView = findViewById(R.id.recycler_catalog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CatalogAdapter();
        recyclerView.setAdapter(adapter);

        buttonAdd.setOnClickListener(v -> {
            if (warehouseId == null) return;
            String name = editName.getText().toString().trim();
            String sku = editSku.getText().toString().trim();
            String price = editPrice.getText().toString().trim();

            if (name.isEmpty() || sku.isEmpty() || price.isEmpty()) {
                Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                ProductTemplate t = new ProductTemplate(name, sku, Double.parseDouble(price), warehouseId, editBrand.getText().toString().trim());
                t.setLowStockThreshold(editThreshold.getText().toString().isEmpty() ? 0 : Integer.parseInt(editThreshold.getText().toString()));
                
                viewModel.upsertTemplate(t);
                editName.setText(""); editSku.setText(""); editBrand.setText(""); editPrice.setText(""); editThreshold.setText("");
            } catch (NumberFormatException e) {
                Toast.makeText(this, "נא להזין מספרים תקינים", Toast.LENGTH_SHORT).show();
            }
        });

        adapter.setOnCatalogClickListener(t -> {
            new AlertDialog.Builder(this)
                    .setMessage("למחוק את " + t.getName() + " מהקטלוג?")
                    .setPositiveButton("מחק", (d, w) -> viewModel.deleteTemplate(t))
                    .setNegativeButton("ביטול", null).show();
        });
    }
}
