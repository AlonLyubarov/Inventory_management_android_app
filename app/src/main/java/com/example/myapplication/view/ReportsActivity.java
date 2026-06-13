package com.example.myapplication.view;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.model.Item;
import com.example.myapplication.model.Transaction;
import com.example.myapplication.viewmodel.ItemViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private ItemViewModel viewModel;
    private String warehouseId;
    private long selectedStartDate = 0, selectedEndDate = 0;
    private final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { finish(); return; }

        viewModel = new ViewModelProvider(this).get(ItemViewModel.class);

        viewModel.getUserProfile(uid).observe(this, user -> {
            if (user != null) this.warehouseId = user.getEmployerId();
        });

        findViewById(R.id.button_export_inventory_csv).setOnClickListener(v -> exportInventory(true));
        findViewById(R.id.button_export_inventory_pdf).setOnClickListener(v -> exportInventory(false));

        findViewById(R.id.button_date_range_report).setOnClickListener(v -> {
            MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker().build();
            picker.show(getSupportFragmentManager(), "date_picker");
            picker.addOnPositiveButtonClickListener(sel -> {
                selectedStartDate = sel.first;
                selectedEndDate = sel.second + 86400000;
            });
        });

        findViewById(R.id.button_export_transactions_csv).setOnClickListener(v -> exportTransactions(true));
        findViewById(R.id.button_export_transactions_pdf).setOnClickListener(v -> exportTransactions(false));
    }

    private void exportInventory(boolean isCsv) {
        if (warehouseId == null) return;
        viewModel.getInventory(warehouseId).observe(this, items -> {
            if (items == null || items.isEmpty()) return;
            if (isCsv) generateInventoryCsv(items);
            else generateInventoryPdf(items);
        });
    }

    private void exportTransactions(boolean isCsv) {
        if (warehouseId == null || selectedStartDate == 0) return;
        viewModel.getTransactionsByDateRange(warehouseId, selectedStartDate, selectedEndDate).observe(this, logs -> {
            if (logs == null || logs.isEmpty()) return;
            if (isCsv) generateTransactionsCsv(logs);
            else generateTransactionsPdf(logs);
        });
    }

    private void generateInventoryCsv(List<Item> items) {
        StringBuilder csv = new StringBuilder("\uFEFFשם מוצר,מק''ט,כמות,מחיר,סה''כ\n");
        for (Item item : items) {
            csv.append(String.format(Locale.getDefault(), "%s,%s,%d,%.2f,%.2f\n",
                    item.getName(), item.getSku(), item.getQuantity(), item.getPrice(), item.getQuantity() * item.getPrice()));
        }
        shareFile("Inventory", csv.toString(), "text/csv", ".csv");
    }

    private void generateTransactionsCsv(List<Transaction> logs) {
        StringBuilder csv = new StringBuilder("\uFEFFתאריך,סוג,פריט,שינוי,שווי\n");
        for (Transaction log : logs) {
            csv.append(String.format(Locale.getDefault(), "%s,%s,%s,%d,%.2f\n",
                    displayDateFormat.format(new Date(log.getTimestamp())), log.getType(), log.getItemName(), log.getQuantityChanged(), log.getAmountChanged()));
        }
        shareFile("Transactions", csv.toString(), "text/csv", ".csv");
    }

    private void generateInventoryPdf(List<Item> items) {
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = doc.startPage(pi);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(14f);
        int y = 50;
        canvas.drawText("Inventory Report - " + displayDateFormat.format(new Date()), 50, y, paint);
        y += 30;
        for (Item item : items) {
            canvas.drawText(item.getName() + " | SKU: " + item.getSku() + " | Qty: " + item.getQuantity(), 50, y, paint);
            y += 20;
            if (y > 800) break;
        }
        doc.finishPage(page);
        sharePdf(doc, "Inventory");
    }

    private void generateTransactionsPdf(List<Transaction> logs) {
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = doc.startPage(pi);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(14f);
        int y = 50;
        canvas.drawText("Transactions Report", 50, y, paint);
        y += 30;
        for (Transaction log : logs) {
            canvas.drawText(displayDateFormat.format(new Date(log.getTimestamp())) + " | " + log.getType() + " | " + log.getItemName(), 50, y, paint);
            y += 20;
            if (y > 800) break;
        }
        doc.finishPage(page);
        sharePdf(doc, "Transactions");
    }

    private void shareFile(String prefix, String data, String mime, String ext) {
        File file = new File(getCacheDir(), prefix + "_" + fileDateFormat.format(new Date()) + ext);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(data.getBytes(StandardCharsets.UTF_8));
            triggerShare(file, mime);
        } catch (IOException ignored) {}
    }

    private void sharePdf(PdfDocument doc, String prefix) {
        File file = new File(getCacheDir(), prefix + "_" + fileDateFormat.format(new Date()) + ".pdf");
        try {
            doc.writeTo(new FileOutputStream(file));
            doc.close();
            triggerShare(file, "application/pdf");
        } catch (IOException ignored) {}
    }

    private void triggerShare(File file, String mime) {
        Uri uri = FileProvider.getUriForFile(this, "com.example.myapplication.fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "שתף דוח"));
    }
}
