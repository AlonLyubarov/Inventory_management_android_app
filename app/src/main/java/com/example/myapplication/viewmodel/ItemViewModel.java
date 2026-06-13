package com.example.myapplication.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.myapplication.model.*;
import com.example.myapplication.repository.ItemRepository;
import java.util.List;

public class ItemViewModel extends AndroidViewModel {

    private final ItemRepository mRepository;

    public ItemViewModel(@NonNull Application application) {
        super(application);
        mRepository = new ItemRepository(application);
    }

    public void startSync(String userId) { mRepository.startReactiveSync(userId); }
    public LiveData<User> getUserProfile(String userId) { return mRepository.getUserProfile(userId); }
    public LiveData<List<Item>> getInventory(String warehouseId) { return mRepository.getInventory(warehouseId); }
    public LiveData<List<ProductTemplate>> getTemplates(String warehouseId) { return mRepository.getTemplates(warehouseId); }

    public void insert(Item item) { mRepository.insertItem(item); }
    public void update(Item item, int oldQty) { mRepository.updateItem(item, oldQty); }
    public void delete(Item item) { mRepository.deleteItem(item); }
    public void upsertTemplate(ProductTemplate t) { mRepository.upsertTemplate(t); }
    public void deleteTemplate(ProductTemplate t) { mRepository.deleteTemplate(t); }

    public LiveData<List<Transaction>> getAllTransactions(String wid) { return mRepository.getAllTransactions(wid); }
    public LiveData<List<Transaction>> searchTransactions(String wid, String q) { return mRepository.searchTransactions(wid, q); }
    public LiveData<List<Transaction>> getTransactionsByDateRange(String wid, long s, long e) { return mRepository.getTransactionsByRange(wid, s, e); }
    
    public LiveData<Integer> getTotalItemsCount(String wid) { return mRepository.getCount(wid); }
    public LiveData<Double> getTotalInventoryValue(String wid) { return mRepository.getValue(wid); }

    public LiveData<List<Item>> search(String wid, String q) { return mRepository.searchDatabase(wid, q); }

    public void logoutOnly(Runnable onComplete) { mRepository.logoutOnly(onComplete); }
    public void logoutAndReset(Runnable onComplete) { mRepository.logoutAndReset(onComplete); }

    public void clearLocalDatabase() {
        com.example.myapplication.model.AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                com.example.myapplication.model.AppDatabase db =
                        com.example.myapplication.model.AppDatabase.getDatabase(this.getApplication());
                db.clearAllTables();
            } catch (Exception ignored) {}
        });
    }
}
