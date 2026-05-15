package com.example.myapplication.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.myapplication.model.Item;
import com.example.myapplication.model.Transaction;
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

    /**
     * Cleans up transactions older than the specified number of days.
     * Uses java.time.Instant for safe and readable time calculations.
     */
    public void cleanOldTransactions(long days) {
        if (days <= 0) return;

        // Calculate the threshold timestamp (current time minus X days)
        long threshold = Instant.now()
                .minus(days, ChronoUnit.DAYS)
                .toEpochMilli();

        mRepository.cleanOldTransactions(threshold);
    }
}
