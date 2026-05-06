package com.example.myapplication.model;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Database: Defines the entities (tables) in the database and the version number.
 * We include both Item and User entities.
 * exportSchema = false is used to keep the project clean of extra schema files.
 */
@Database(entities = {Item.class, User.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // DAOs (Data Access Objects) for interacting with the tables
    public abstract ItemDao itemDao();
    public abstract UserDao userDao();

    // Singleton instance to prevent multiple instances of the database opening at the same time
    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;

    /**
     * ExecutorService to run database operations on background threads.
     * Room does not allow database access on the main UI thread to prevent freezing.
     */
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    /**
     * Returns the singleton instance of the database.
     * Uses synchronized block to ensure thread safety.
     */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "inventory_database")
                            /**
                             * .fallbackToDestructiveMigration()
                             * This is crucial when the database version changes.
                             * Instead of crashing due to missing Migration rules,
                             * it will clear the old data and recreate the tables.
                             */
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}