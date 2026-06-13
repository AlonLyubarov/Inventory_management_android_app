package com.example.myapplication;

import com.example.myapplication.model.Item;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventoryItemTest {

    @Test
    public void testItemValueCalculation() {
        Item item = new Item("Phone", 1000.50, 5, "uid_1", "SKU_P", "Apple");
        double expectedTotal = 1000.50 * 5;
        assertEquals(expectedTotal, item.getPrice() * item.getQuantity(), 0.001);
    }

    @Test
    public void testLowStockLogic() {
        Item item = new Item("Milk", 5.0, 2, "uid_1", "SKU_M", "Tara");
        item.setLowStockThreshold(5);
        
        // Quantity (2) is less than threshold (5)
        assertTrue(item.getQuantity() < item.getLowStockThreshold());
        
        item.setQuantity(10);
        // Quantity (10) is more than threshold (5)
        assertFalse(item.getQuantity() < item.getLowStockThreshold());
    }

    @Test
    public void testSkuAssignment() {
        Item item = new Item("Laptop", 3500.0, 1, "uid_1", "SKU12345", "Dell");
        assertEquals("SKU12345", item.getSku());
    }
}
