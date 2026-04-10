package com.valinor.iposca;

import API.CA_PU_implementation;
import API.CA_PU_interface;

import java.util.List;
import java.util.Map;

/**
 * Quick test to verify the CA-PU API works.
 * Run this directly — it has its own main method.
 * Delete before demo.
 */
public class TestCAPU {

    public static void main(String[] args) {
        try {
            CA_PU_interface api = new CA_PU_implementation();

            // 1. Get VAT rate
            System.out.println("VAT Rate: " + api.getVATRate() + "%");
            System.out.println();

            // 2. Get full catalogue
            System.out.println("=== FULL CATALOGUE ===");
            System.out.printf("%-12s %-25s %-10s %-12s %-12s%n",
                    "Item ID", "Description", "Quantity", "Unit Cost", "Total Cost");
            System.out.println("-".repeat(75));

            List<Map<String, String>> catalogue = api.getCatalogue();
            for (Map<String, String> item : catalogue) {
                System.out.printf("%-12s %-25s %-10s £%-11s £%-11s%n",
                        item.get("itemId"),
                        item.get("description"),
                        item.get("quantity"),
                        item.get("unitCost"),
                        item.get("totalCost"));
            }
            System.out.println("\nTotal items: " + catalogue.size());
            System.out.println();

            // 3. Search
            System.out.println("=== SEARCH: 'vitamin' ===");
            List<Map<String, String>> results = api.searchCatalogue("vitamin");
            for (Map<String, String> item : results) {
                System.out.printf("%-12s %-25s %-10s £%-11s%n",
                        item.get("itemId"),
                        item.get("description"),
                        item.get("quantity"),
                        item.get("unitCost"));
            }
            System.out.println();

            // 4. Single item lookup
            System.out.println("=== ITEM DETAILS: '100 00001' ===");
            Map<String, String> item = api.getItemDetails("100 00001");
            if (item != null) {
                for (Map.Entry<String, String> e : item.entrySet()) {
                    System.out.println("  " + e.getKey() + ": " + e.getValue());
                }
            } else {
                System.out.println("  Item not found.");
            }
            System.out.println();

            // 5. Check availability
            System.out.println("=== AVAILABILITY CHECK ===");
            System.out.println("100 00001 x 5: " + api.checkAvailability("100 00001", 5));
            System.out.println("100 00001 x 999999: " + api.checkAvailability("100 00001", 999999));

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
