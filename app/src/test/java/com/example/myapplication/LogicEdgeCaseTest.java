package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.example.myapplication.model.Item;

public class LogicEdgeCaseTest {

    @Test
    public void testNegativeQuantityPrevention() {
        Item item = new Item("Test", 10.0, 5, "owner", "SKU", "Brand");
        item.setQuantity(-5); // B-14 Fix: Test guard in setter
        assertEquals(0, item.getQuantity());
    }

    @Test
    public void testNegativePricePrevention() {
        Item item = new Item("Test", 10.0, 5, "owner", "SKU", "Brand");
        item.setPrice(-99.9); // B-14 Fix: Test guard in setter
        assertEquals(0.0, item.getPrice(), 0.001);
    }

    @Test
    public void testStringNormalization() {
        String inputName = "  iPhone 15 Pro  ";
        String normalized = inputName.trim();
        assertEquals("iPhone 15 Pro", normalized);

        String inputSku = "abc-123-def";
        String normalizedSku = inputSku.toUpperCase().trim();
        assertEquals("ABC-123-DEF", normalizedSku);
    }
}
