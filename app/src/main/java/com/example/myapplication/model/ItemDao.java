package com.example.myapplication.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ItemDao {

    @Insert
    void insert(Item item);

    @Update
    void update(Item item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(Item item);

    @Delete
    void delete(Item item);

    @Query("DELETE FROM items_table")
    void deleteAllItems();

    @Query("SELECT * FROM items_table WHERE ownerId = :userId ORDER BY name ASC, id ASC")
    LiveData<List<Item>> getAllItems(String userId);

    @Query("SELECT * FROM items_table WHERE name LIKE :searchQuery OR sku LIKE :searchQuery ORDER BY name ASC, id ASC")
    LiveData<List<Item>> searchDatabase(String searchQuery);

    @Query("SELECT * FROM items_table WHERE firestoreId = :firestoreId LIMIT 1")
    Item getItemByFirestoreId(String firestoreId);

    @Query("SELECT * FROM items_table WHERE ownerId = :userId AND sku = :sku LIMIT 1")
    Item getItemBySku(String userId, String sku);

    @Query("SELECT COUNT(*) FROM items_table WHERE ownerId = :userId")
    LiveData<Integer> getTotalItemsCount(String userId);

    @Query("SELECT SUM(price * quantity) FROM items_table WHERE ownerId = :userId")
    LiveData<Double> getTotalInventoryValue(String userId);
}
