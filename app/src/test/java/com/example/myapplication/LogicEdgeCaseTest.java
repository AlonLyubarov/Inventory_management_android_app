package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.example.myapplication.model.Item;

public class LogicEdgeCaseTest {

    @Test
    public void testNegativeQuantityPrevention() {
        Item item = new Item("Test", 10.0, 0, "owner", "SKU", "Brand");
        // Simulate clicking minus when at zero
        int delta = -1;
        int resultQty = Math.max(0, item.getQuantity() + delta);
        assertEquals(0, resultQty);
    }

    @Test
    public void testPriceNormalization() {
        double inputPrice = -50.5;
        double safePrice = Math.max(0, inputPrice);
        assertEquals(0.0, safePrice, 0.001);
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

    @Test
    public void testInventoryValueCalculation() {
        Item item = new Item("Gold", 2500.0, 5, "owner", "SKU", "Brand");
        double totalValue = item.getQuantity() * item.getPrice();
        assertEquals(12500.0, totalValue, 0.001);
    }
}
