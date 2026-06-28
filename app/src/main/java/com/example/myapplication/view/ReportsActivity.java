package com.example.myapplication.view;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
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
import java.text.Bidi;
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
        LiveData<List<Item>> liveData = viewModel.getInventory(warehouseId);
        liveData.observe(this, new androidx.lifecycle.Observer<List<Item>>() {
            @Override
            public void onChanged(List<Item> items) {
                liveData.removeObserver(this);
                if (items != null && !items.isEmpty()) {
                    if (isCsv) generateInventoryCsv(items);
                    else generateInventoryPdf(items);
                }
            }
        });
    }

    private void exportTransactions(boolean isCsv) {
        if (warehouseId == null || selectedStartDate == 0) {
            Toast.makeText(this, "Please select date range", Toast.LENGTH_SHORT).show();
            return;
        }
        LiveData<List<Transaction>> liveData = viewModel.getTransactionsRange(warehouseId, selectedStartDate, selectedEndDate);
        liveData.observe(this, new androidx.lifecycle.Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> logs) {
                liveData.removeObserver(this);
                if (logs != null && !logs.isEmpty()) {
                    if (isCsv) generateTransactionsCsv(logs);
                    else generateTransactionsPdf(logs);
                }
            }
        });
    }

    private void generateInventoryCsv(List<Item> items) {
        StringBuilder csv = new StringBuilder("\uFEFFProduct Name,SKU,Quantity,Price,Total\n");
        for (Item item : items) {
            csv.append(String.format(Locale.getDefault(), "%s,%s,%d,%.2f,%.2f\n",
                    item.getName(), item.getSku(), item.getQuantity(), item.getPrice(), item.getQuantity() * item.getPrice()));
        }
        shareFile("Inventory", csv.toString(), ".csv");
    }

    private void generateTransactionsCsv(List<Transaction> logs) {
        StringBuilder csv = new StringBuilder("\uFEFFDate,Type,Item,Change,Value\n");
        for (Transaction log : logs) {
            csv.append(String.format(Locale.getDefault(), "%s,%s,%s,%d,%.2f\n",
                    displayDateFormat.format(new Date(log.getTimestamp())), log.getType(), log.getItemName(), log.getQuantityChanged(), log.getAmountChanged()));
        }
        shareFile("Transactions", csv.toString(), ".csv");
    }

    private void generateInventoryPdf(List<Item> items) {
        PdfDocument doc = new PdfDocument();
        Paint paint = new Paint();
        Paint headerPaint = new Paint();
        headerPaint.setFakeBoldText(true);
        headerPaint.setTextSize(16f);
        paint.setTextSize(12f);

        int pageNum = 1;
        int y = 80;
        PdfDocument.Page page = startNewPage(doc, pageNum++);
        Canvas canvas = page.getCanvas();

        canvas.drawText(fixRTL("Inventory Report - " + displayDateFormat.format(new Date())), 500, 40, headerPaint);
        
        // Table Headers
        drawTableRow(canvas, 60, paint, true, "Product", "SKU", "Qty", "Price");
        
        for (Item item : items) {
            if (y > 780) {
                doc.finishPage(page);
                page = startNewPage(doc, pageNum++);
                canvas = page.getCanvas();
                y = 60;
            }
            drawTableRow(canvas, y + 20, paint, false, item.getName(), item.getSku(), String.valueOf(item.getQuantity()), String.format("%.2f", item.getPrice()));
            y += 30;
        }
        
        doc.finishPage(page);
        sharePdf(doc, "Inventory");
    }

    private void generateTransactionsPdf(List<Transaction> logs) {
        PdfDocument doc = new PdfDocument();
        Paint paint = new Paint();
        Paint headerPaint = new Paint();
        headerPaint.setFakeBoldText(true);
        headerPaint.setTextSize(16f);
        paint.setTextSize(10f);

        int pageNum = 1;
        int y = 80;
        PdfDocument.Page page = startNewPage(doc, pageNum++);
        Canvas canvas = page.getCanvas();

        canvas.drawText(fixRTL("Transactions Report"), 500, 40, headerPaint);
        drawTableRow(canvas, 60, paint, true, "Date", "Type", "Item", "Qty");

        for (Transaction log : logs) {
            if (y > 780) {
                doc.finishPage(page);
                page = startNewPage(doc, pageNum++);
                canvas = page.getCanvas();
                y = 60;
            }
            String date = displayDateFormat.format(new Date(log.getTimestamp()));
            drawTableRow(canvas, y + 20, paint, false, date, log.getType(), log.getItemName(), String.valueOf(log.getQuantityChanged()));
            y += 25;
        }
        doc.finishPage(page);
        sharePdf(doc, "Transactions");
    }

    private void drawTableRow(Canvas canvas, int y, Paint paint, boolean isHeader, String col1, String col2, String col3, String col4) {
        if (isHeader) paint.setFakeBoldText(true);
        
        // Adjusting columns for RTL flow (Right to Left)
        canvas.drawText(fixRTL(col1), 400, y, paint); // Name/Date
        canvas.drawText(fixRTL(col2), 250, y, paint); // SKU/Type
        canvas.drawText(fixRTL(col3), 150, y, paint); // Qty
        canvas.drawText(fixRTL(col4), 50, y, paint);  // Price
        
        if (isHeader) {
            paint.setFakeBoldText(false);
            canvas.drawLine(50, y + 5, 550, y + 5, paint);
        }
    }

    private PdfDocument.Page startNewPage(PdfDocument doc, int num) {
        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(595, 842, num).create();
        return doc.startPage(pi);
    }

    /**
     * Corrects BiDi mixed text by handling Hebrew word order.
     */
    private String fixRTL(String text) {
        if (text == null || text.isEmpty()) return "";
        
        // Check if contains Hebrew characters
        boolean hasHebrew = false;
        for (char c : text.toCharArray()) {
            if (c >= '\u0590' && c <= '\u05FF') {
                hasHebrew = true;
                break;
            }
        }
        
        if (!hasHebrew) return text;

        // Manual BiDi logic: Reverse the whole string for Canvas.drawText
        // Then handle numbers/english segments if they appear "flipped"
        return new StringBuilder(text).reverse().toString();
    }

    private void shareFile(String prefix, String data, String ext) {
        File file = new File(getCacheDir(), prefix + "_" + fileDateFormat.format(new Date()) + ext);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(data.getBytes(StandardCharsets.UTF_8));
            triggerShare(file, ".csv".equals(ext) ? "text/csv" : "application/pdf");
        } catch (IOException ignored) {}
    }

    private void sharePdf(PdfDocument doc, String prefix) {
        File file = new File(getCacheDir(), prefix + "_" + fileDateFormat.format(new Date()) + ".pdf");
        try (FileOutputStream out = new FileOutputStream(file)) {
            doc.writeTo(out);
            doc.close();
            triggerShare(file, "application/pdf");
        } catch (IOException ignored) {}
    }

    private void triggerShare(File file, String mime) {
        Uri uri = FileProvider.getUriForFile(this, "com.example.myapplication.fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType(mime)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share Report"));
    }
}
