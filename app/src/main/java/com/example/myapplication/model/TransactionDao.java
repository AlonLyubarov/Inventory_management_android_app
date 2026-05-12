package com.example.myapplication.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(Transaction transaction);

    // I increased the limit or ensured no limit is present. 
    // By default Room has no limit, but we will explicitly order and ensure we get all data.
    @Query("SELECT * FROM transactions_table WHERE ownerId = :userId AND itemName LIKE :query ORDER BY timestamp DESC")
    LiveData<List<Transaction>> searchTransactions(String userId, String query);

    @Query("SELECT * FROM transactions_table WHERE ownerId = :userId AND timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    LiveData<List<Transaction>> getTransactionsByDateRange(String userId, long startDate, long endDate);

    @Query("DELETE FROM transactions_table WHERE timestamp < :threshold")
    void deleteOldTransactions(long threshold);
}
