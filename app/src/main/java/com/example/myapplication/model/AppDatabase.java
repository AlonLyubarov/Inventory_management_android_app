package com.example.myapplication.model;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Item.class, User.class, Transaction.class, ProductTemplate.class}, version = 19, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ItemDao itemDao();
    public abstract UserDao userDao();
    public abstract TransactionDao transactionDao();
    public abstract ProductTemplateDao productTemplateDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;

    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_table_firestoreId` ON `transactions_table` (`firestoreId`)");
        }
    };

    static final Migration MIGRATION_18_19 = new Migration(18, 19) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_table_ownerId_sku` ON `items_table` (`ownerId`, `sku`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_table_ownerId_name` ON `items_table` (`ownerId`, `name`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_table_ownerId_timestamp` ON `transactions_table` (`ownerId`, `timestamp`)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "inventory_database")
                            .addMigrations(MIGRATION_17_18, MIGRATION_18_19)
                            .fallbackToDestructiveMigration() // H3 Fix: Safety fallback as per README intent
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
