package com.example.myapplication.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Item item);

    @Update
    void update(Item item);

    @Transaction
    default void smartUpsert(Item item) {
        long id = insertWithIdReturn(item);
        if (id == -1) {
            update(item);
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertWithIdReturn(Item item);

    @Delete
    void delete(Item item);

    @Query("DELETE FROM items_table WHERE firestoreId = :firestoreId")
    void deleteByFirestoreId(String firestoreId);

    @Query("DELETE FROM items_table")
    void deleteAllItems();

    @Query("SELECT * FROM items_table WHERE ownerId = :userId ORDER BY name ASC, firestoreId ASC")
    LiveData<List<Item>> getAllItems(String userId);

    @Query("SELECT * FROM items_table WHERE ownerId = :userId AND (name LIKE :searchQuery ESCAPE '\\' OR sku LIKE :searchQuery ESCAPE '\\') ORDER BY name ASC")
    LiveData<List<Item>> searchDatabase(String userId, String searchQuery);

    @Query("SELECT * FROM items_table WHERE firestoreId = :firestoreId LIMIT 1")
    Item getItemByFirestoreId(String firestoreId);

    @Query("SELECT * FROM items_table WHERE ownerId = :userId AND sku = :sku AND name = :name LIMIT 1")
    Item getItemBySkuAndName(String userId, String sku, String name);

    @Query("SELECT * FROM items_table WHERE ownerId = :userId AND sku = :sku LIMIT 1")
    Item getItemBySku(String userId, String sku);

    @Query("SELECT COUNT(*) FROM items_table WHERE ownerId = :userId")
    LiveData<Integer> getTotalItemsCount(String userId);

    // H4 Fix: Sum cents and divide by 100 for display
    @Query("SELECT COALESCE(SUM(priceCents * quantity), 0) / 100.0 FROM items_table WHERE ownerId = :userId")
    LiveData<Double> getTotalInventoryValue(String userId);
}
