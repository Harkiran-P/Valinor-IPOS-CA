package com.valinor.iposca.gui;

import com.valinor.iposca.dao.StockDAO;
import com.valinor.iposca.model.StockItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * GUI panel for the IPOS-CA-Stock package.
 * Lets the user view, add, edit, delete, and search stock items.
 * Also handles delivery recording, low stock warnings, VAT and markup configuration.
 */
public class StockPanel extends JPanel {

    private StockDAO stockDAO;

    // Table that displays all stock items
    private JTable stockTable;
    private DefaultTableModel tableModel;

    // Search bar at the top
    private JTextField searchField;

    // Column names for the stock table
    private final String[] columnNames = {
        "Item ID", "Description", "Pkg Type", "Unit", "Units/Pack",
        "Bulk Cost (£)", "Markup %", "Retail (£)", "Availability", "Stock Limit", "Status"
    };

    public StockPanel() {
        stockDAO = new StockDAO();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Build each section of the screen
        add(createTopPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        // Load stock data into the table
        refreshTable();

        // Show low stock warning if any items are below their threshold
        showLowStockWarningIfNeeded();
    }

    /**
     * Creates the top section with the search bar and VAT config button.
     */
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));

        // Title label
        JLabel titleLabel = new JLabel("Stock Management");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        // Search section
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchField = new JTextField(20);
        searchPanel.add(searchField);

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());
        searchPanel.add(searchButton);

        JButton clearButton = new JButton("Show All");
        clearButton.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });
        searchPanel.add(clearButton);

        // VAT rate button
        JButton vatButton = new JButton("Configure VAT Rate");
        vatButton.addActionListener(e -> configureVATRate());
        searchPanel.add(vatButton);

        // Low stock report button
        JButton lowStockButton = new JButton("Low Stock Report");
        lowStockButton.addActionListener(e -> showLowStockReport());
        searchPanel.add(lowStockButton);

        topPanel.add(searchPanel, BorderLayout.CENTER);

        return topPanel;
    }

    /**
     * Creates the table that displays stock items.
     */
    private JScrollPane createTablePanel() {
        // DefaultTableModel controls what data the table shows
        tableModel = new DefaultTableModel(columnNames, 0) {
            // This stops the user from editing cells directly in the table
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        stockTable = new JTable(tableModel);
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stockTable.setRowHeight(25);
        stockTable.getTableHeader().setReorderingAllowed(false);

        return new JScrollPane(stockTable);
    }

    /**
     * Creates the buttons at the bottom of the screen.
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        JButton addButton = new JButton("Add Item");
        addButton.addActionListener(e -> showAddItemDialog());
        buttonPanel.add(addButton);

        JButton editButton = new JButton("Edit Item");
        editButton.addActionListener(e -> showEditItemDialog());
        buttonPanel.add(editButton);

        JButton deleteButton = new JButton("Delete Item");
        deleteButton.addActionListener(e -> deleteSelectedItem());
        buttonPanel.add(deleteButton);

        JButton deliveryButton = new JButton("Record Delivery");
        deliveryButton.addActionListener(e -> showRecordDeliveryDialog());
        buttonPanel.add(deliveryButton);

        JButton markupButton = new JButton("Set Markup Rate");
        markupButton.addActionListener(e -> showSetMarkupDialog());
        buttonPanel.add(markupButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTable());
        buttonPanel.add(refreshButton);

        return buttonPanel;
    }

    /**
     * Reloads all stock data from the database into the table.
     */
    private void refreshTable() {
        tableModel.setRowCount(0);
        double vatRate = stockDAO.getVATRate();

        List<StockItem> items = stockDAO.getAllStockItems();
        for (StockItem item : items) {
            addItemToTable(item, vatRate);
        }
    }

    /**
     * Adds one stock item as a row in the table.
     */
    private void addItemToTable(StockItem item, double vatRate) {
        String status = item.isLowStock() ? "LOW STOCK" : "OK";

        tableModel.addRow(new Object[]{
            item.getItemId(),
            item.getDescription(),
            item.getPackageType(),
            item.getUnit(),
            item.getUnitsInPack(),
            String.format("%.2f", item.getBulkCost()),
            String.format("%.1f%%", item.getMarkupRate()),
            String.format("%.2f", item.getRetailPriceWithVAT(vatRate)),
            item.getAvailability(),
            item.getStockLimit(),
            status
        });
    }

    /**
     * Searches stock items by the keyword in the search box.
     */
    private void performSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            refreshTable();
            return;
        }

        tableModel.setRowCount(0);
        double vatRate = stockDAO.getVATRate();

        List<StockItem> items = stockDAO.searchStockItems(keyword);
        for (StockItem item : items) {
            addItemToTable(item, vatRate);
        }

        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No items found matching: " + keyword,
                "Search Results", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Shows a dialog to add a new stock item.
     */
    private void showAddItemDialog() {
        // Create input fields
        JTextField idField = new JTextField(15);
        JTextField descField = new JTextField(15);
        JTextField pkgTypeField = new JTextField(10);
        JTextField unitField = new JTextField(10);
        JTextField unitsPackField = new JTextField(5);
        JTextField bulkCostField = new JTextField(10);
        JTextField markupField = new JTextField(5);
        JTextField availField = new JTextField(5);
        JTextField limitField = new JTextField(5);

        // Arrange fields in a form layout
        JPanel formPanel = new JPanel(new GridLayout(9, 2, 5, 5));
        formPanel.add(new JLabel("Item ID (e.g. 100 00001):"));
        formPanel.add(idField);
        formPanel.add(new JLabel("Description:"));
        formPanel.add(descField);
        formPanel.add(new JLabel("Package Type (e.g. box):"));
        formPanel.add(pkgTypeField);
        formPanel.add(new JLabel("Unit (e.g. Caps, ml):"));
        formPanel.add(unitField);
        formPanel.add(new JLabel("Units in Pack:"));
        formPanel.add(unitsPackField);
        formPanel.add(new JLabel("Bulk Cost (£):"));
        formPanel.add(bulkCostField);
        formPanel.add(new JLabel("Markup Rate (%):"));
        formPanel.add(markupField);
        formPanel.add(new JLabel("Initial Availability (packs):"));
        formPanel.add(availField);
        formPanel.add(new JLabel("Low Stock Limit (packs):"));
        formPanel.add(limitField);

        int result = JOptionPane.showConfirmDialog(this, formPanel,
            "Add New Stock Item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                // Create the new stock item from the form inputs
                StockItem item = new StockItem();
                item.setItemId(idField.getText().trim());
                item.setDescription(descField.getText().trim());
                item.setPackageType(pkgTypeField.getText().trim());
                item.setUnit(unitField.getText().trim());
                item.setUnitsInPack(Integer.parseInt(unitsPackField.getText().trim()));
                item.setBulkCost(Double.parseDouble(bulkCostField.getText().trim()));
                item.setMarkupRate(Double.parseDouble(markupField.getText().trim()));
                item.setAvailability(Integer.parseInt(availField.getText().trim()));
                item.setStockLimit(Integer.parseInt(limitField.getText().trim()));

                // Check that required fields are not empty
                if (item.getItemId().isEmpty() || item.getDescription().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Item ID and Description are required.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Try to save to database
                if (stockDAO.addStockItem(item)) {
                    JOptionPane.showMessageDialog(this, "Stock item added successfully.");
                    refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Failed to add item. The Item ID may already exist.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Please enter valid numbers for numeric fields.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Shows a dialog to edit the currently selected stock item.
     */
    private void showEditItemDialog() {
        int selectedRow = stockTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to edit.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get the item ID from the selected row
        String itemId = (String) tableModel.getValueAt(selectedRow, 0);
        StockItem item = stockDAO.getStockItemById(itemId);

        if (item == null) {
            JOptionPane.showMessageDialog(this, "Item not found.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Pre-fill the form with existing data
        JTextField descField = new JTextField(item.getDescription(), 15);
        JTextField pkgTypeField = new JTextField(item.getPackageType(), 10);
        JTextField unitField = new JTextField(item.getUnit(), 10);
        JTextField unitsPackField = new JTextField(String.valueOf(item.getUnitsInPack()), 5);
        JTextField bulkCostField = new JTextField(String.valueOf(item.getBulkCost()), 10);
        JTextField markupField = new JTextField(String.valueOf(item.getMarkupRate()), 5);
        JTextField availField = new JTextField(String.valueOf(item.getAvailability()), 5);
        JTextField limitField = new JTextField(String.valueOf(item.getStockLimit()), 5);

        JPanel formPanel = new JPanel(new GridLayout(8, 2, 5, 5));
        formPanel.add(new JLabel("Description:"));
        formPanel.add(descField);
        formPanel.add(new JLabel("Package Type:"));
        formPanel.add(pkgTypeField);
        formPanel.add(new JLabel("Unit:"));
        formPanel.add(unitField);
        formPanel.add(new JLabel("Units in Pack:"));
        formPanel.add(unitsPackField);
        formPanel.add(new JLabel("Bulk Cost (£):"));
        formPanel.add(bulkCostField);
        formPanel.add(new JLabel("Markup Rate (%):"));
        formPanel.add(markupField);
        formPanel.add(new JLabel("Availability (packs):"));
        formPanel.add(availField);
        formPanel.add(new JLabel("Low Stock Limit (packs):"));
        formPanel.add(limitField);

        int result = JOptionPane.showConfirmDialog(this, formPanel,
            "Edit Stock Item: " + itemId, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                item.setDescription(descField.getText().trim());
                item.setPackageType(pkgTypeField.getText().trim());
                item.setUnit(unitField.getText().trim());
                item.setUnitsInPack(Integer.parseInt(unitsPackField.getText().trim()));
                item.setBulkCost(Double.parseDouble(bulkCostField.getText().trim()));
                item.setMarkupRate(Double.parseDouble(markupField.getText().trim()));
                item.setAvailability(Integer.parseInt(availField.getText().trim()));
                item.setStockLimit(Integer.parseInt(limitField.getText().trim()));

                if (stockDAO.updateStockItem(item)) {
                    JOptionPane.showMessageDialog(this, "Stock item updated successfully.");
                    refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update item.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Please enter valid numbers for numeric fields.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Deletes the currently selected stock item after confirmation.
     */
    private void deleteSelectedItem() {
        int selectedRow = stockTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to delete.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String itemId = (String) tableModel.getValueAt(selectedRow, 0);
        String description = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete:\n" + itemId + " - " + description + "?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (stockDAO.deleteStockItem(itemId)) {
                JOptionPane.showMessageDialog(this, "Item deleted successfully.");
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete item.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Shows a dialog to record a delivery for a selected stock item.
     */
    private void showRecordDeliveryDialog() {
        int selectedRow = stockTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to record a delivery for.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String itemId = (String) tableModel.getValueAt(selectedRow, 0);
        String description = (String) tableModel.getValueAt(selectedRow, 1);

        JTextField quantityField = new JTextField(10);
        JTextField notesField = new JTextField(20);

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        formPanel.add(new JLabel("Item:"));
        formPanel.add(new JLabel(itemId + " - " + description));
        formPanel.add(new JLabel("Quantity Delivered (packs):"));
        formPanel.add(quantityField);
        formPanel.add(new JLabel("Notes (optional):"));
        formPanel.add(notesField);

        int result = JOptionPane.showConfirmDialog(this, formPanel,
            "Record Delivery", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int quantity = Integer.parseInt(quantityField.getText().trim());

                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(this, "Quantity must be greater than 0.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String notes = notesField.getText().trim();

                if (stockDAO.recordDelivery(itemId, quantity, notes.isEmpty() ? null : notes)) {
                    JOptionPane.showMessageDialog(this,
                        "Delivery recorded. Stock increased by " + quantity + " packs.");
                    refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to record delivery.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for quantity.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Shows a dialog to set the markup rate for a selected stock item.
     */
    private void showSetMarkupDialog() {
        int selectedRow = stockTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to set markup for.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String itemId = (String) tableModel.getValueAt(selectedRow, 0);
        StockItem item = stockDAO.getStockItemById(itemId);

        if (item == null) {
            JOptionPane.showMessageDialog(this, "Item not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String input = JOptionPane.showInputDialog(this,
            "Enter new markup rate (%) for " + item.getDescription() + ":",
            String.valueOf(item.getMarkupRate()));

        if (input != null) {
            try {
                double newRate = Double.parseDouble(input.trim());
                if (newRate < 0) {
                    JOptionPane.showMessageDialog(this, "Markup rate cannot be negative.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                item.setMarkupRate(newRate);
                if (stockDAO.updateStockItem(item)) {
                    JOptionPane.showMessageDialog(this, "Markup rate updated to " + newRate + "%.");
                    refreshTable();
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Shows a dialog to configure the system-wide VAT rate.
     */
    private void configureVATRate() {
        double currentRate = stockDAO.getVATRate();

        String input = JOptionPane.showInputDialog(this,
            "Enter the VAT rate (%).\nCurrent rate: " + currentRate + "%",
            String.valueOf(currentRate));

        if (input != null) {
            try {
                double newRate = Double.parseDouble(input.trim());
                if (newRate < 0) {
                    JOptionPane.showMessageDialog(this, "VAT rate cannot be negative.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (stockDAO.setVATRate(newRate)) {
                    JOptionPane.showMessageDialog(this, "VAT rate updated to " + newRate + "%.");
                    refreshTable();
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Shows a popup listing all items that are below their stock limit.
     */
    private void showLowStockReport() {
        List<StockItem> lowItems = stockDAO.getLowStockItems();

        if (lowItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All items are above their stock limits.",
                "Low Stock Report", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Build a text report of low stock items
        StringBuilder report = new StringBuilder();
        report.append(String.format("%-15s %-20s %-12s %-12s %-15s\n",
            "Item ID", "Description", "Available", "Limit", "Recommended"));
        report.append("-".repeat(75)).append("\n");

        for (StockItem item : lowItems) {
            // Recommended order = enough to get 10% above the limit
            int recommended = (int) Math.ceil(item.getStockLimit() * 1.10) - item.getAvailability();
            if (recommended < 0) recommended = 0;

            report.append(String.format("%-15s %-20s %-12d %-12d %-15d\n",
                item.getItemId(),
                item.getDescription(),
                item.getAvailability(),
                item.getStockLimit(),
                recommended));
        }

        // Display in a scrollable text area
        JTextArea textArea = new JTextArea(report.toString());
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 300));

        JOptionPane.showMessageDialog(this, scrollPane,
            "Low Stock Report - " + lowItems.size() + " item(s) below limit",
            JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Checks for low stock items when the panel first loads.
     * Shows a warning popup if any items are below their threshold.
     */
    private void showLowStockWarningIfNeeded() {
        List<StockItem> lowItems = stockDAO.getLowStockItems();

        if (!lowItems.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("WARNING: ").append(lowItems.size())
                   .append(" item(s) are below their stock limit:\n\n");

            for (StockItem item : lowItems) {
                message.append("  - ").append(item.getDescription())
                       .append(" (").append(item.getAvailability())
                       .append("/").append(item.getStockLimit()).append(")\n");
            }

            message.append("\nConsider placing an order with InfoPharma.");

            JOptionPane.showMessageDialog(this, message.toString(),
                "Low Stock Warning", JOptionPane.WARNING_MESSAGE);
        }
    }
}
