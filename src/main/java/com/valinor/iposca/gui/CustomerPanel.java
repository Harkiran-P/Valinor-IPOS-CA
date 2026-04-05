package com.valinor.iposca.gui;

import com.valinor.iposca.dao.CustomerDAO;
import com.valinor.iposca.model.AccountHolder;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * GUI panel for the IPOS-CA-CUST package.
 * Lets the user manage customer account holders, record payments,
 * generate reminders and monthly statements.
 */
public class CustomerPanel extends JPanel {

    private CustomerDAO customerDAO;
    private JTable customerTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    private final String[] columnNames = {
        "Acc ID", "First Name", "Last Name", "Phone", "Email",
        "Credit Limit (£)", "Balance (£)", "Discount", "Status",
        "1st Reminder", "2nd Reminder"
    };

    public CustomerPanel() {
        customerDAO = new CustomerDAO();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createTopPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        refreshTable();
    }

    /**
     * Creates the top section with title, search bar, and action buttons.
     */
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));

        JLabel titleLabel = new JLabel("Customer Account Management");
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

        topPanel.add(searchPanel, BorderLayout.CENTER);

        return topPanel;
    }

    /**
     * Creates the table that displays all account holders.
     */
    private JScrollPane createTablePanel() {
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customerTable = new JTable(tableModel);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.setRowHeight(25);
        customerTable.getTableHeader().setReorderingAllowed(false);

        return new JScrollPane(customerTable);
    }

    /**
     * Creates two rows of buttons at the bottom of the screen.
     */
    private JPanel createButtonPanel() {
        JPanel outerPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        // Row 1: Account management buttons
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));

        JButton addButton = new JButton("Add Customer");
        addButton.addActionListener(e -> showAddCustomerDialog());
        row1.add(addButton);

        JButton editButton = new JButton("Edit Customer");
        editButton.addActionListener(e -> showEditCustomerDialog());
        row1.add(editButton);

        JButton deleteButton = new JButton("Delete Customer");
        deleteButton.addActionListener(e -> deleteSelectedCustomer());
        row1.add(deleteButton);

        JButton viewButton = new JButton("View Details");
        viewButton.addActionListener(e -> viewCustomerDetails());
        row1.add(viewButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTable());
        row1.add(refreshButton);

        outerPanel.add(row1);

        // Row 2: Payment and reminder buttons
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));

        JButton paymentButton = new JButton("Record Payment");
        paymentButton.addActionListener(e -> showRecordPaymentDialog());
        row2.add(paymentButton);

        JButton restoreButton = new JButton("Restore Account");
        restoreButton.addActionListener(e -> restoreSelectedAccount());
        row2.add(restoreButton);

        JButton statusButton = new JButton("Update Statuses");
        statusButton.addActionListener(e -> runStatusUpdate());
        row2.add(statusButton);

        JButton reminderButton = new JButton("Generate Reminders");
        reminderButton.addActionListener(e -> generateReminders());
        row2.add(reminderButton);

        JButton statementButton = new JButton("Monthly Statements");
        statementButton.addActionListener(e -> generateStatements());
        row2.add(statementButton);

        outerPanel.add(row2);

        return outerPanel;
    }

    /**
     * Reloads all customer data from the database into the table.
     */
    private void refreshTable() {
        tableModel.setRowCount(0);
        List<AccountHolder> holders = customerDAO.getAllAccountHolders();

        for (AccountHolder holder : holders) {
            addHolderToTable(holder);
        }
    }

    /**
     * Adds one account holder as a row in the table.
     */
    private void addHolderToTable(AccountHolder holder) {
        // Build the discount display text
        String discountDisplay;
        if ("fixed".equals(holder.getDiscountType())) {
            discountDisplay = "Fixed " + holder.getDiscountRate() + "%";
        } else if ("flexible".equals(holder.getDiscountType())) {
            discountDisplay = "Flexible";
        } else {
            discountDisplay = "None";
        }

        tableModel.addRow(new Object[]{
            holder.getAccountId(),
            holder.getFirstName(),
            holder.getLastName(),
            holder.getPhone(),
            holder.getEmail(),
            String.format("%.2f", holder.getCreditLimit()),
            String.format("%.2f", holder.getOutstandingBalance()),
            discountDisplay,
            holder.getAccountStatus(),
            holder.getStatus1stReminder(),
            holder.getStatus2ndReminder()
        });
    }

    /**
     * Searches customers by the keyword in the search box.
     */
    private void performSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            refreshTable();
            return;
        }

        tableModel.setRowCount(0);
        List<AccountHolder> holders = customerDAO.searchAccountHolders(keyword);

        for (AccountHolder holder : holders) {
            addHolderToTable(holder);
        }

        if (holders.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No customers found matching: " + keyword,
                "Search Results", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Shows a dialog to create a new customer account.
     */
    private void showAddCustomerDialog() {
        JTextField firstNameField = new JTextField(15);
        JTextField lastNameField = new JTextField(15);
        JTextField addressField = new JTextField(20);
        JTextField phoneField = new JTextField(15);
        JTextField emailField = new JTextField(20);
        JTextField creditLimitField = new JTextField(10);

        // Discount type dropdown
        JComboBox<String> discountTypeBox = new JComboBox<>(new String[]{"none", "fixed", "flexible"});
        JTextField discountRateField = new JTextField("0.0", 5);

        // Only show discount rate field when "fixed" is selected
        discountTypeBox.addActionListener(e -> {
            String selected = (String) discountTypeBox.getSelectedItem();
            discountRateField.setEnabled("fixed".equals(selected));
            if (!"fixed".equals(selected)) {
                discountRateField.setText("0.0");
            }
        });
        discountRateField.setEnabled(false);

        JPanel formPanel = new JPanel(new GridLayout(8, 2, 5, 5));
        formPanel.add(new JLabel("First Name:*"));
        formPanel.add(firstNameField);
        formPanel.add(new JLabel("Last Name:*"));
        formPanel.add(lastNameField);
        formPanel.add(new JLabel("Address:"));
        formPanel.add(addressField);
        formPanel.add(new JLabel("Phone:"));
        formPanel.add(phoneField);
        formPanel.add(new JLabel("Email:"));
        formPanel.add(emailField);
        formPanel.add(new JLabel("Credit Limit (£):*"));
        formPanel.add(creditLimitField);
        formPanel.add(new JLabel("Discount Type:"));
        formPanel.add(discountTypeBox);
        formPanel.add(new JLabel("Discount Rate (%) [fixed only]:"));
        formPanel.add(discountRateField);

        int result = JOptionPane.showConfirmDialog(this, formPanel,
            "Add New Customer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String firstName = firstNameField.getText().trim();
                String lastName = lastNameField.getText().trim();

                if (firstName.isEmpty() || lastName.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "First Name and Last Name are required.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double creditLimit = Double.parseDouble(creditLimitField.getText().trim());
                if (creditLimit < 0) {
                    JOptionPane.showMessageDialog(this, "Credit limit cannot be negative.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                AccountHolder holder = new AccountHolder();
                holder.setFirstName(firstName);
                holder.setLastName(lastName);
                holder.setAddress(addressField.getText().trim());
                holder.setPhone(phoneField.getText().trim());
                holder.setEmail(emailField.getText().trim());
                holder.setCreditLimit(creditLimit);
                holder.setDiscountType((String) discountTypeBox.getSelectedItem());
                holder.setDiscountRate(Double.parseDouble(discountRateField.getText().trim()));

                int newId = customerDAO.createAccountHolder(holder);
                if (newId > 0) {
                    JOptionPane.showMessageDialog(this,
                        "Customer created successfully. Account ID: " + newId);
                    refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to create customer.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Please enter valid numbers for Credit Limit and Discount Rate.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Shows a dialog to edit the selected customer's details.
     */
    private void showEditCustomerDialog() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer to edit.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int accountId = (int) tableModel.getValueAt(selectedRow, 0);
        AccountHolder holder = customerDAO.getAccountHolderById(accountId);

        if (holder == null) {
            JOptionPane.showMessageDialog(this, "Customer not found.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Pre-fill form with existing data
        JTextField firstNameField = new JTextField(holder.getFirstName(), 15);
        JTextField lastNameField = new JTextField(holder.getLastName(), 15);
        JTextField addressField = new JTextField(holder.getAddress() != null ? holder.getAddress() : "", 20);
        JTextField phoneField = new JTextField(holder.getPhone() != null ? holder.getPhone() : "", 15);
        JTextField emailField = new JTextField(holder.getEmail() != null ? holder.getEmail() : "", 20);
        JTextField creditLimitField = new JTextField(String.valueOf(holder.getCreditLimit()), 10);
        JComboBox<String> discountTypeBox = new JComboBox<>(new String[]{"none", "fixed", "flexible"});
        discountTypeBox.setSelectedItem(holder.getDiscountType());
        JTextField discountRateField = new JTextField(String.valueOf(holder.getDiscountRate()), 5);
        discountRateField.setEnabled("fixed".equals(holder.getDiscountType()));

        discountTypeBox.addActionListener(e -> {
            String selected = (String) discountTypeBox.getSelectedItem();
            discountRateField.setEnabled("fixed".equals(selected));
            if (!"fixed".equals(selected)) {
                discountRateField.setText("0.0");
            }
        });

        JPanel formPanel = new JPanel(new GridLayout(8, 2, 5, 5));
        formPanel.add(new JLabel("First Name:"));
        formPanel.add(firstNameField);
        formPanel.add(new JLabel("Last Name:"));
        formPanel.add(lastNameField);
        formPanel.add(new JLabel("Address:"));
        formPanel.add(addressField);
        formPanel.add(new JLabel("Phone:"));
        formPanel.add(phoneField);
        formPanel.add(new JLabel("Email:"));
        formPanel.add(emailField);
        formPanel.add(new JLabel("Credit Limit (£):"));
        formPanel.add(creditLimitField);
        formPanel.add(new JLabel("Discount Type:"));
        formPanel.add(discountTypeBox);
        formPanel.add(new JLabel("Discount Rate (%):"));
        formPanel.add(discountRateField);

        int result = JOptionPane.showConfirmDialog(this, formPanel,
            "Edit Customer: " + holder.getFullName(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                holder.setFirstName(firstNameField.getText().trim());
                holder.setLastName(lastNameField.getText().trim());
                holder.setAddress(addressField.getText().trim());
                holder.setPhone(phoneField.getText().trim());
                holder.setEmail(emailField.getText().trim());
                holder.setCreditLimit(Double.parseDouble(creditLimitField.getText().trim()));
                holder.setDiscountType((String) discountTypeBox.getSelectedItem());
                holder.setDiscountRate(Double.parseDouble(discountRateField.getText().trim()));

                if (customerDAO.updateAccountHolder(holder)) {
                    JOptionPane.showMessageDialog(this, "Customer updated successfully.");
                    refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update customer.",
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
     * Deletes the selected customer after confirmation.
     */
    private void deleteSelectedCustomer() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer to delete.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int accountId = (int) tableModel.getValueAt(selectedRow, 0);
        String firstName = (String) tableModel.getValueAt(selectedRow, 1);
        String lastName = (String) tableModel.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete customer:\n" + firstName + " " + lastName +
            " (Account ID: " + accountId + ")?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (customerDAO.deleteAccountHolder(accountId)) {
                JOptionPane.showMessageDialog(this, "Customer deleted successfully.");
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete customer.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Shows a detailed view of the selected customer.
     */
    private void viewCustomerDetails() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer to view.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int accountId = (int) tableModel.getValueAt(selectedRow, 0);
        AccountHolder holder = customerDAO.getAccountHolderById(accountId);

        if (holder == null) {
            JOptionPane.showMessageDialog(this, "Customer not found.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String details = String.format(
            "Account ID: %d\n" +
            "Name: %s\n" +
            "Address: %s\n" +
            "Phone: %s\n" +
            "Email: %s\n" +
            "Credit Limit: £%.2f\n" +
            "Outstanding Balance: £%.2f\n" +
            "Available Credit: £%.2f\n" +
            "Discount Type: %s\n" +
            "Discount Rate: %.1f%%\n" +
            "Account Status: %s\n" +
            "1st Reminder Status: %s\n" +
            "2nd Reminder Status: %s\n" +
            "2nd Reminder Date: %s\n" +
            "Account Created: %s",
            holder.getAccountId(),
            holder.getFullName(),
            holder.getAddress() != null ? holder.getAddress() : "N/A",
            holder.getPhone() != null ? holder.getPhone() : "N/A",
            holder.getEmail() != null ? holder.getEmail() : "N/A",
            holder.getCreditLimit(),
            holder.getOutstandingBalance(),
            holder.getCreditLimit() - holder.getOutstandingBalance(),
            holder.getDiscountType(),
            holder.getDiscountRate(),
            holder.getAccountStatus(),
            holder.getStatus1stReminder(),
            holder.getStatus2ndReminder(),
            holder.getDate2ndReminder() != null ? holder.getDate2ndReminder() : "N/A",
            holder.getCreatedAt() != null ? holder.getCreatedAt() : "N/A"
        );

        JOptionPane.showMessageDialog(this, details,
            "Customer Details - " + holder.getFullName(), JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a dialog to record a payment from the selected account holder.
     */
    private void showRecordPaymentDialog() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer to record payment for.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int accountId = (int) tableModel.getValueAt(selectedRow, 0);
        AccountHolder holder = customerDAO.getAccountHolderById(accountId);

        if (holder == null) {
            JOptionPane.showMessageDialog(this, "Customer not found.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (holder.getOutstandingBalance() <= 0) {
            JOptionPane.showMessageDialog(this,
                holder.getFullName() + " has no outstanding balance.",
                "No Balance", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Payment form
        JTextField amountField = new JTextField(String.format("%.2f", holder.getOutstandingBalance()), 10);
        JComboBox<String> methodBox = new JComboBox<>(new String[]{"card", "cash"});
        JTextField cardTypeField = new JTextField(10);
        JTextField cardFirst4Field = new JTextField(4);
        JTextField cardLast4Field = new JTextField(4);
        JTextField cardExpiryField = new JTextField(7);

        JPanel formPanel = new JPanel(new GridLayout(8, 2, 5, 5));
        formPanel.add(new JLabel("Customer:"));
        formPanel.add(new JLabel(holder.getFullName()));
        formPanel.add(new JLabel("Outstanding Balance:"));
        formPanel.add(new JLabel("£" + String.format("%.2f", holder.getOutstandingBalance())));
        formPanel.add(new JLabel("Payment Amount (£):"));
        formPanel.add(amountField);
        formPanel.add(new JLabel("Payment Method:"));
        formPanel.add(methodBox);
        formPanel.add(new JLabel("Card Type (if card):"));
        formPanel.add(cardTypeField);
        formPanel.add(new JLabel("Card First 4 Digits:"));
        formPanel.add(cardFirst4Field);
        formPanel.add(new JLabel("Card Last 4 Digits:"));
        formPanel.add(cardLast4Field);
        formPanel.add(new JLabel("Card Expiry (MM/YY):"));
        formPanel.add(cardExpiryField);

        int result = JOptionPane.showConfirmDialog(this, formPanel,
            "Record Payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                double amount = Double.parseDouble(amountField.getText().trim());

                if (amount <= 0) {
                    JOptionPane.showMessageDialog(this, "Payment amount must be greater than 0.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (amount > holder.getOutstandingBalance()) {
                    JOptionPane.showMessageDialog(this,
                        "Payment amount cannot exceed the outstanding balance of £" +
                        String.format("%.2f", holder.getOutstandingBalance()),
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String method = (String) methodBox.getSelectedItem();
                String cardType = cardTypeField.getText().trim();
                String cardFirst4 = cardFirst4Field.getText().trim();
                String cardLast4 = cardLast4Field.getText().trim();
                String cardExpiry = cardExpiryField.getText().trim();

                // Make card fields null if paying by cash
                if ("cash".equals(method)) {
                    cardType = null;
                    cardFirst4 = null;
                    cardLast4 = null;
                    cardExpiry = null;
                }

                if (customerDAO.recordPayment(accountId, amount, method,
                        cardType, cardFirst4, cardLast4, cardExpiry)) {

                    String message = "Payment of £" + String.format("%.2f", amount) + " recorded.";

                    // Check if fully paid
                    if (amount >= holder.getOutstandingBalance()) {
                        message += "\nBalance fully cleared.";
                        if (!"in default".equals(holder.getAccountStatus())) {
                            message += "\nAccount status restored to normal.";
                        }
                    }

                    JOptionPane.showMessageDialog(this, message);
                    refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to record payment.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid payment amount.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Restores the selected account from "suspended" or "in default" to "normal".
     * Only Managers should be able to do this (role check will be added with login system).
     */
    private void restoreSelectedAccount() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer account to restore.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int accountId = (int) tableModel.getValueAt(selectedRow, 0);
        AccountHolder holder = customerDAO.getAccountHolderById(accountId);

        if (holder == null) {
            JOptionPane.showMessageDialog(this, "Customer not found.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if ("normal".equals(holder.getAccountStatus())) {
            JOptionPane.showMessageDialog(this,
                "This account is already in normal status.",
                "No Action Needed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Restore account for " + holder.getFullName() + "?\n" +
            "Current status: " + holder.getAccountStatus() + "\n" +
            "Outstanding balance: £" + String.format("%.2f", holder.getOutstandingBalance()),
            "Confirm Restore", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (customerDAO.restoreAccountStatus(accountId)) {
                JOptionPane.showMessageDialog(this,
                    "Account restored to normal for " + holder.getFullName());
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to restore account.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Runs the automatic account status update process.
     * In a real system this would run on a timer, but in our prototype
     * the user triggers it manually.
     */
    private void runStatusUpdate() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "This will update account statuses based on today's date.\n" +
            "Accounts with unpaid balances may be suspended or set to default.\n\n" +
            "Continue?",
            "Update Account Statuses", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            customerDAO.runAccountStatusUpdate();
            JOptionPane.showMessageDialog(this, "Account statuses have been updated.");
            refreshTable();
        }
    }

    /**
     * Generates payment reminders for accounts that have reminders due.
     */
    private void generateReminders() {
        List<String> reminders = customerDAO.generateReminders();

        if (reminders.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No reminders are due at this time.",
                "Generate Reminders", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show all generated reminders in a scrollable text area
        StringBuilder allReminders = new StringBuilder();
        for (String reminder : reminders) {
            allReminders.append(reminder).append("\n");
        }

        JTextArea textArea = new JTextArea(allReminders.toString());
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this, scrollPane,
            reminders.size() + " Reminder(s) Generated",
            JOptionPane.INFORMATION_MESSAGE);

        refreshTable();
    }

    /**
     * Generates monthly statements for account holders with outstanding balances.
     * Only works between the 5th and 15th of the month.
     */
    private void generateStatements() {
        List<String> statements = customerDAO.generateMonthlyStatements();

        if (statements == null) {
            JOptionPane.showMessageDialog(this,
                "Monthly statements can only be generated between the 5th and 15th of the month.",
                "Outside Statement Period", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (statements.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No account holders have outstanding balances.",
                "Monthly Statements", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show all statements in a scrollable text area
        StringBuilder allStatements = new StringBuilder();
        for (String statement : statements) {
            allStatements.append(statement).append("\n");
        }

        JTextArea textArea = new JTextArea(allStatements.toString());
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this, scrollPane,
            statements.size() + " Statement(s) Generated",
            JOptionPane.INFORMATION_MESSAGE);
    }
}
