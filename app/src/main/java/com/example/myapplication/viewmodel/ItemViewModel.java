package com.example.myapplication.viewmodel;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.myapplication.model.Item;
import com.example.myapplication.model.ProductTemplate;
import com.example.myapplication.model.Transaction;
import com.example.myapplication.model.User;
import com.example.myapplication.repository.ItemRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ItemViewModel extends AndroidViewModel {

    private ItemRepository mRepository;

    public ItemViewModel(@NonNull Application application) {
        super(application);
        mRepository = new ItemRepository(application);
    }

    public void syncFromCloud(String userId) {
        mRepository.syncFromCloud(userId);
    }

    public LiveData<User> getUserProfile(String userId) {
        return mRepository.getUserProfile(userId);
    }

    public LiveData<List<Item>> getAllItems(String userId) {
        return mRepository.getAllItems(userId);
    }

    public LiveData<List<Item>> searchDatabase(String searchQuery) {
        return mRepository.searchDatabase(searchQuery);
    }

    public void insert(Item item) {
        mRepository.insert(item);
    }

    public void update(Item item, int oldQuantity) {
        mRepository.update(item, oldQuantity);
    }

    public void delete(Item item) {
        mRepository.delete(item);
    }

    public LiveData<List<Transaction>> getAllTransactions(String userId) {
        return mRepository.getAllTransactions(userId);
    }

    public LiveData<List<Transaction>> searchTransactions(String userId, String query) {
        return mRepository.searchTransactions(userId, query);
    }

    public LiveData<List<Transaction>> getTransactionsByDateRange(String userId, long start, long end) {
        return mRepository.getTransactionsByDateRange(userId, start, end);
    }

    public LiveData<Integer> getTotalItemsCount(String userId) {
        return mRepository.getTotalItemsCount(userId);
    }

    public LiveData<Double> getTotalInventoryValue(String userId) {
        return mRepository.getTotalInventoryValue(userId);
    }

    public void cleanOldTransactions(long days, String userId) {
        long threshold = Instant.now().minus(days, ChronoUnit.DAYS).toEpochMilli();
        mRepository.cleanOldTransactions(threshold, userId);
    }

    // Product Template Methods
    public LiveData<List<ProductTemplate>> getAllTemplates(String userId) {
        return mRepository.getAllTemplates(userId);
    }

    public void upsertTemplate(ProductTemplate template) {
        mRepository.upsertTemplate(template);
    }

    public void deleteTemplate(ProductTemplate template) {
        mRepository.deleteTemplate(template);
    }

    public void logoutAndReset(Runnable onComplete) {
        mRepository.logoutAndReset(onComplete);
    }

    public void logoutOnly(Runnable onComplete) {
        mRepository.logoutOnly(onComplete);
    }

    /**
     * Asynchronously purges all local Room SQLite tracking entity records during a deep data wipe operation.
     * This utility leverages the static thread executor pre-configured on the centralized AppDatabase structural class.
     */
    public void clearLocalDatabase() {
        com.example.myapplication.model.AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Initialize database framework link using the verified structural layout context instance
                com.example.myapplication.model.AppDatabase db =
                        com.example.myapplication.model.AppDatabase.getDatabase(this.getApplication());

                // Clear the content of all structural entities natively mapped inside Room storage
                db.clearAllTables();
                Log.d("ItemViewModel", "Successfully dropped local Room SQLite tables records from storage context.");
            } catch (Exception e) {
                Log.e("ItemViewModel", "Fatal error occurred while executing direct clearAllTables cache purge execution", e);
            }
        });
    }
}