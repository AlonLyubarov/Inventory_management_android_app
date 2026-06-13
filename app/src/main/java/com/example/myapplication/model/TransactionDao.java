package com.example.myapplication.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Transaction transaction);

    @Query("DELETE FROM transactions_table WHERE firestoreId = :fid")
    void deleteByFirestoreId(String fid);

    @Query("SELECT * FROM transactions_table WHERE firestoreId = :fid LIMIT 1")
    Transaction getByFirestoreId(String fid);

    @Query("SELECT * FROM transactions_table WHERE ownerId = :userId AND itemName LIKE :query ORDER BY timestamp DESC")
    LiveData<List<Transaction>> searchTransactions(String userId, String query);

    @Query("SELECT * FROM transactions_table WHERE ownerId = :userId AND timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    LiveData<List<Transaction>> getTransactionsByDateRange(String userId, long startDate, long endDate);

    @Query("DELETE FROM transactions_table WHERE timestamp < :threshold")
    void deleteOldTransactions(long threshold);

    @Query("DELETE FROM transactions_table")
    void deleteAllTransactions();
}
