package com.example.myapplication.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    @Query("SELECT * FROM user_table WHERE userId = :id LIMIT 1")
    User getUserById(String id);

    @Query("SELECT * FROM user_table WHERE userId = :id LIMIT 1")
    LiveData<User> getUserProfileLiveData(String id);

    @Query("DELETE FROM user_table")
    void deleteAllUsers();
}
