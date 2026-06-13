package com.example.myapplication;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.myapplication.model.AppDatabase;
import com.example.myapplication.model.Item;
import com.example.myapplication.model.ItemDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 * Tests the Room Database DAO operations.
 */
@RunWith(AndroidJUnit4.class)
public class InventoryDatabaseTest {
    private ItemDao itemDao;
    private AppDatabase db;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        itemDao = db.itemDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void writeItemAndReadInList() {
        Item item = new Item("Hammer", 25.0, 10, "manager_123", "SKU_H", "WorkTools");
        itemDao.insert(item);
        
        Item result = itemDao.getItemBySku("manager_123", "SKU_H");
        assertEquals("Hammer", result.getName());
        assertEquals(10, result.getQuantity());
    }
}
