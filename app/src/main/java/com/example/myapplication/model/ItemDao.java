package com.example.myapplication.model;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ItemDao {

    @Insert
    void insert(Item item); // Inserts a single item into the table

    @Update
    void update(Item item); // Updates an existing item (based on its ID)

    @Delete
    void delete(Item item); // Deletes a specific item

    @Query("DELETE FROM items_table")
    void deleteAllItems(); // Deletes all data from the table

    @Query("SELECT * FROM items_table WHERE ownerId = :userId ORDER BY name ASC")
    LiveData<List<Item>> getAllItems(String userId);
      }