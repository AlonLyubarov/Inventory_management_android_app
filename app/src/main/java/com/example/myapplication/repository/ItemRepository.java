package com.example.myapplication.repository;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;

import com.example.myapplication.model.AppDatabase;
import com.example.myapplication.model.Item;
import com.example.myapplication.model.ItemDao;
import com.example.myapplication.model.Transaction;
import com.example.myapplication.model.TransactionDao;

import java.util.List;

/**
 * Repository class that handles data operations.
 * It provides a clean API so that the rest of the app can retrieve data easily.
 */
public class ItemRepository {

    private ItemDao mItemDao;
    private TransactionDao mTransactionDao;

    public ItemRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mItemDao = db.itemDao();
        mTransactionDao = db.transactionDao();
    }

    /**
     * Retrieves all items for a specific user.
     */
    public LiveData<List<Item>> getAllItems(String userId) {
        return mItemDao.getAllItems(userId);
    }

    /**
     * Inserts a new item and records the transaction.
     */
    public void insert(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemDao.insert(item);
            long now = System.currentTimeMillis();
            Transaction t = new Transaction("ADD", item.getName(), item.getQuantity(), 
                    item.getQuantity() * item.getPrice(), now, item.getOwnerId());
            mTransactionDao.insert(t);
            Log.d("InventoryRepo", "Recorded ADD: " + item.getName() + " for user: " + item.getOwnerId() + " at: " + now);
        });
    }

    /**
     * Updates an existing item and records the quantity change transaction.
     */
    public void update(Item item, int oldQuantity) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemDao.update(item);
            int diff = item.getQuantity() - oldQuantity;
            if (diff == 0) return;
            
            long now = System.currentTimeMillis();
            String type = diff > 0 ? "UPDATE_PLUS" : "UPDATE_MINUS";
            Transaction t = new Transaction(type, item.getName(), Math.abs(diff), 
                    Math.abs(diff) * item.getPrice(), now, item.getOwnerId());
            mTransactionDao.insert(t);
            Log.d("InventoryRepo", "Recorded " + type + ": " + item.getName() + " diff: " + diff + " for user: " + item.getOwnerId() + " at: " + now);
        });
    }

    /**
     * Deletes an item and records the deletion transaction.
     */
    public void delete(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemDao.delete(item);
            long now = System.currentTimeMillis();
            Transaction t = new Transaction("DELETE", item.getName(), item.getQuantity(), 
                    item.getQuantity() * item.getPrice(), now, item.getOwnerId());
            mTransactionDao.insert(t);
            Log.d("InventoryRepo", "Recorded DELETE: " + item.getName() + " for user: " + item.getOwnerId() + " at: " + now);
        });
    }

    /**
     * Searches the database for items matching the query.
     */
    public LiveData<List<Item>> searchDatabase(String searchQuery) {
        return mItemDao.searchDatabase(searchQuery);
    }

    /**
     * Retrieves all transactions for a specific user.
     */
    public LiveData<List<Transaction>> getAllTransactions(String userId) {
        return mTransactionDao.searchTransactions(userId, "%%");
    }

    public LiveData<Integer> getTotalItemsCount(String userId) {
        return mItemDao.getTotalItemsCount(userId);
    }

    public LiveData<Double> getTotalInventoryValue(String userId) {
        return mItemDao.getTotalInventoryValue(userId);
    }

    /**
     * Searches transactions for a specific user.
     */
    public LiveData<List<Transaction>> searchTransactions(String userId, String query) {
        return mTransactionDao.searchTransactions(userId, "%" + query + "%");
    }

    /**
     * Retrieves transactions within a date range.
     */
    public LiveData<List<Transaction>> getTransactionsByDateRange(String userId, long start, long end) {
        return mTransactionDao.getTransactionsByDateRange(userId, start, end);
    }

    /**
     * Deletes transactions older than the threshold timestamp.
     */
    public void cleanOldTransactions(long threshold) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Log.d("InventoryRepo", "CLEANUP TRIGGERED. Deleting before: " + threshold);
            mTransactionDao.deleteOldTransactions(threshold);
        });
    }
}
