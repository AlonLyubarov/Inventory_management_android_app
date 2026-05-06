package com.example.myapplication.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.myapplication.model.AppDatabase;
import com.example.myapplication.model.Item;
import com.example.myapplication.model.ItemDao;

import java.util.List;

/**
 * The Repository class abstracts access to multiple data sources (like the Room DB).
 * It provides a clean API for the rest of the application to interact with data.
 */
public class ItemRepository {

    private ItemDao mItemDao;

    /**
     * Constructor that initializes the database and the DAO.
     */
    public ItemRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mItemDao = db.itemDao();
    }

    /**
     * Returns all items as LiveData. Room executes this on a background thread.
     */
    public LiveData<List<Item>> getAllItems(String userId) {
        return mItemDao.getAllItems(userId);
    }
    /**
     * Inserts an item using the executor service defined in AppDatabase.
     * This ensures the database operation doesn't block the UI thread.
     */
    public void insert(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemDao.insert(item);
        });
    }

    public void update(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemDao.update(item);
        });
    }

    public void delete(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemDao.delete(item);
        });
    }
}