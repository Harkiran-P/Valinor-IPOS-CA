package com.valinor.iposca.gui;

import com.valinor.iposca.dao.CustomerDAO;
import com.valinor.iposca.dao.SalesDAO;
import com.valinor.iposca.dao.StockDAO;
import com.valinor.iposca.model.AccountHolder;
import com.valinor.iposca.model.Sale;
import com.valinor.iposca.model.SaleItem;
import com.valinor.iposca.model.StockItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI panel for the IPOS-CA-Sales package.
 * Lets the pharmacist record sales to customers (account holders
 * and walk-ins), choose payment method, and view/print receipts.
 */
public class SalesPanel extends JPanel {

    private SalesDAO salesDAO;
    private StockDAO stockDAO;
    private CustomerDAO customerDAO;

    // left side - new sale form
    private JComboBox<String> customerTypeBox;
    private JComboBox<AccountHolder> accountHolderBox;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel subtotalLabel;
    private JLabel discountLabel;
    private JLabel vatLabel;
    private JLabel totalLabel;

    // right side - past sales list
    private JTable salesHistoryTable;
    private DefaultTableModel historyModel;

    // items currently in the cart
    private List<SaleItem> cartItems;

    private final String[] cartColumns = {"Item ID", "Description", "Qty", "Unit Price (£)", "Line Total (£)"};
    private final String[] historyColumns = {"Sale ID", "Date", "Customer", "Total (£)", "Payment"};

    public SalesPanel() {
        salesDAO = new SalesDAO();
        stockDAO = new StockDAO();
        customerDAO = new CustomerDAO();
        cartItems = new ArrayList<>();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // split screen: left = new sale, right = history
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createNewSalePanel(), createHistoryPanel());
        splitPane.setDividerLocation(620);
        splitPane.setResizeWeight(0.6);

        add(splitPane, BorderLayout.CENTER);

        refreshHistory();
    }

    /**
     * Builds the left side - the new sale form with cart.
     */
    private JPanel createNewSalePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // title
        JLabel title = new JLabel("New Sale");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(title, BorderLayout.NORTH);

        // middle - customer selection + cart
        JPanel middlePanel = new JPanel(new BorderLayout(5, 5));

        // customer selection row
        JPanel customerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        customerRow.add(new JLabel("Customer:"));
        customerTypeBox = new JComboBox<>(new String[]{"Walk-in (Occasional)", "Account Holder"});
        customerTypeBox.addActionListener(e -> toggleAccountHolderDropdown());
        customerRow.add(customerTypeBox);

        accountHolderBox = new JComboBox<>();
        accountHolderBox.setEnabled(false);
        loadAccountHolders();
        customerRow.add(accountHolderBox);

        JButton refreshCustomers = new JButton("Refresh");
        refreshCustomers.addActionListener(e -> loadAccountHolders());
        customerRow.add(refreshCustomers);

        middlePanel.add(customerRow, BorderLayout.NORTH);

        // shopping cart table
        cartModel = new DefaultTableModel(cartColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(22);
        middlePanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        // totals panel below the cart
        JPanel totalsPanel = new JPanel(new GridLayout(4, 1));
        subtotalLabel = new JLabel("Subtotal: £0.00");
        discountLabel = new JLabel("Discount: £0.00");
        vatLabel = new JLabel("VAT: £0.00");
        totalLabel = new JLabel("TOTAL: £0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        totalsPanel.add(subtotalLabel);
        totalsPanel.add(discountLabel);
        totalsPanel.add(vatLabel);
        totalsPanel.add(totalLabel);
        middlePanel.add(totalsPanel, BorderLayout.SOUTH);

        panel.add(middlePanel, BorderLayout.CENTER);

        // bottom buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));

        JButton addItemBtn = new JButton("Add Item to Cart");
        addItemBtn.addActionListener(e -> addItemToCart());
        buttonPanel.add(addItemBtn);

        JButton removeItemBtn = new JButton("Remove Item");
        removeItemBtn.addActionListener(e -> removeItemFromCart());
        buttonPanel.add(removeItemBtn);

        JButton clearBtn = new JButton("Clear Cart");
        clearBtn.addActionListener(e -> clearCart());
        buttonPanel.add(clearBtn);

        JButton checkoutBtn = new JButton("Checkout");
        checkoutBtn.addActionListener(e -> processCheckout());
        buttonPanel.add(checkoutBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Builds the right side - sales history list.
     */
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JLabel title = new JLabel("Sales History");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(title, BorderLayout.NORTH);

        historyModel = new DefaultTableModel(historyColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        salesHistoryTable = new JTable(historyModel);
        salesHistoryTable.setRowHeight(22);
        panel.add(new JScrollPane(salesHistoryTable), BorderLayout.CENTER);

        // buttons for history
        JPanel historyButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));

        JButton viewReceiptBtn = new JButton("View Receipt");
        viewReceiptBtn.addActionListener(e -> viewSelectedReceipt());
        historyButtons.add(viewReceiptBtn);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshHistory());
        historyButtons.add(refreshBtn);

        panel.add(historyButtons, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Shows/hides the account holder dropdown based on customer type.
     */
    private void toggleAccountHolderDropdown() {
        boolean isAccountHolder = customerTypeBox.getSelectedIndex() == 1;
        accountHolderBox.setEnabled(isAccountHolder);
    }

    /**
     * Loads all account holders into the dropdown.
     */
    private void loadAccountHolders() {
        accountHolderBox.removeAllItems();
        List<AccountHolder> holders = customerDAO.getAllAccountHolders();
        for (AccountHolder h : holders) {
            accountHolderBox.addItem(h);
        }
    }

    /**
     * Prompts user to pick a stock item and quantity, then adds it to the cart.
     */
    private void addItemToCart() {
        // let the user search for a stock item
        String searchTerm = JOptionPane.showInputDialog(this,
                "Enter item ID or description to search:");

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return;
        }

        List<StockItem> results = stockDAO.searchStockItems(searchTerm.trim());
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items found matching: " + searchTerm,
                    "Not Found", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // if multiple results, let user pick one
        StockItem selected;
        if (results.size() == 1) {
            selected = results.get(0);
        } else {
            StockItem[] options = results.toArray(new StockItem[0]);
            selected = (StockItem) JOptionPane.showInputDialog(this,
                    "Multiple items found. Select one:",
                    "Select Item", JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);
            if (selected == null) return;
        }

        // check there's actually stock available
        if (selected.getAvailability() <= 0) {
            JOptionPane.showMessageDialog(this,
                    selected.getDescription() + " is out of stock.",
                    "Out of Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ask for quantity
        String qtyStr = JOptionPane.showInputDialog(this,
                selected.getDescription() + "\nAvailable: " + selected.getAvailability() +
                        "\nRetail price: £" + String.format("%.2f", selected.getRetailPrice()) +
                        "\n\nEnter quantity:");

        if (qtyStr == null || qtyStr.trim().isEmpty()) return;

        try {
            int qty = Integer.parseInt(qtyStr.trim());
            if (qty <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be at least 1.",
                        "Invalid Quantity", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (qty > selected.getAvailability()) {
                JOptionPane.showMessageDialog(this,
                        "Not enough stock. Only " + selected.getAvailability() + " available.",
                        "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // add to cart
            double unitPrice = selected.getRetailPrice();
            SaleItem saleItem = new SaleItem(selected.getItemId(),
                    selected.getDescription(), qty, unitPrice);
            cartItems.add(saleItem);

            // update the cart display
            refreshCart();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Removes the selected item from the cart.
     */
    private void removeItemFromCart() {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an item to remove.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        cartItems.remove(selectedRow);
        refreshCart();
    }

    /**
     * Empties the cart completely.
     */
    private void clearCart() {
        cartItems.clear();
        refreshCart();
    }

    /**
     * Redraws the cart table and recalculates totals.
     */
    private void refreshCart() {
        cartModel.setRowCount(0);
        double vatRate = stockDAO.getVATRate();

        double subtotal = 0;
        for (SaleItem item : cartItems) {
            cartModel.addRow(new Object[]{
                    item.getItemId(),
                    item.getItemDescription(),
                    item.getQuantity(),
                    String.format("%.2f", item.getUnitPrice()),
                    String.format("%.2f", item.getLineTotal())
            });
            subtotal += item.getLineTotal();
        }

        // work out discount (only for account holders with fixed discount)
        double discountPercent = 0;
        if (customerTypeBox.getSelectedIndex() == 1 && accountHolderBox.getSelectedItem() != null) {
            AccountHolder holder = (AccountHolder) accountHolderBox.getSelectedItem();
            if ("fixed".equals(holder.getDiscountType())) {
                discountPercent = holder.getDiscountRate();
            }
            // flexible discount is calculated at month end, not per sale
        }

        double discountAmount = subtotal * (discountPercent / 100.0);
        double afterDiscount = subtotal - discountAmount;
        double vatAmount = afterDiscount * (vatRate / 100.0);
        double total = afterDiscount + vatAmount;

        subtotalLabel.setText(String.format("Subtotal: £%.2f", subtotal));
        discountLabel.setText(String.format("Discount: £%.2f (%.1f%%)", discountAmount, discountPercent));
        vatLabel.setText(String.format("VAT: £%.2f (%.1f%%)", vatAmount, vatRate));
        totalLabel.setText(String.format("TOTAL: £%.2f", total));
    }

    /**
     * Processes the checkout-asks for payment method and records the sale.
     * Enforces the payment rules from the brief:
     *   - Walk-in customers must pay in full (cash or card)
     *   - Account holders can only use card (not cash)
     *   - Account holders can pay on credit if their status is normal
     *     and the amount doesn't exceed their credit limit
     */
    private void processCheckout() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty. Add items first.",
                    "Empty Cart", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double vatRate = stockDAO.getVATRate();
        boolean isAccountHolder = customerTypeBox.getSelectedIndex() == 1;
        AccountHolder holder = null;

        if (isAccountHolder) {
            holder = (AccountHolder) accountHolderBox.getSelectedItem();
            if (holder == null) {
                JOptionPane.showMessageDialog(this, "Please select an account holder.",
                        "No Customer", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // calculate totals
        double subtotal = 0;
        for (SaleItem item : cartItems) {
            subtotal += item.getLineTotal();
        }

        double discountPercent = 0;
        if (holder != null && "fixed".equals(holder.getDiscountType())) {
            discountPercent = holder.getDiscountRate();
        }

        double discountAmount = subtotal * (discountPercent / 100.0);
        double afterDiscount = subtotal - discountAmount;
        double vatAmount = afterDiscount * (vatRate / 100.0);
        double total = afterDiscount + vatAmount;

        // build payment options based on customer type
        String[] paymentOptions;
        if (isAccountHolder) {
            // account holders: card or credit (no cash per the brief)
            paymentOptions = new String[]{"Card", "Credit (add to balance)"};
        } else {
            // walk-in: cash or card, must pay now
            paymentOptions = new String[]{"Cash", "Card"};
        }

        String paymentChoice = (String) JOptionPane.showInputDialog(this,
                String.format("Total: £%.2f\n\nSelect payment method:", total),
                "Payment", JOptionPane.PLAIN_MESSAGE, null,
                paymentOptions, paymentOptions[0]);

        if (paymentChoice == null) return;

        // map the choice to our database values
        String paymentMethod;
        if (paymentChoice.startsWith("Credit")) {
            paymentMethod = "credit";
        } else if (paymentChoice.equals("Card")) {
            paymentMethod = "card";
        } else {
            paymentMethod = "cash";
        }

        // if credit, check account status and credit limit
        if ("credit".equals(paymentMethod) && holder != null) {
            if (!"normal".equals(holder.getAccountStatus())) {
                JOptionPane.showMessageDialog(this,
                        "Cannot use credit - account is " + holder.getAccountStatus() + ".",
                        "Credit Denied", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!holder.canPurchaseOnCredit(total)) {
                JOptionPane.showMessageDialog(this,
                        "Credit limit would be exceeded.\n" +
                                "Credit limit: £" + String.format("%.2f", holder.getCreditLimit()) + "\n" +
                                "Current balance: £" + String.format("%.2f", holder.getOutstandingBalance()) + "\n" +
                                "This sale: £" + String.format("%.2f", total),
                        "Credit Denied", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // collect card details if paying by card
        String cardType = null, cardFirst4 = null, cardLast4 = null, cardExpiry = null;

        if ("card".equals(paymentMethod)) {
            JTextField cardTypeField = new JTextField(10);
            JTextField first4Field = new JTextField(4);
            JTextField last4Field = new JTextField(4);
            JTextField expiryField = new JTextField(7);

            JPanel cardPanel = new JPanel(new GridLayout(4, 2, 5, 5));
            cardPanel.add(new JLabel("Card Type (e.g. Visa):"));
            cardPanel.add(cardTypeField);
            cardPanel.add(new JLabel("First 4 Digits:"));
            cardPanel.add(first4Field);
            cardPanel.add(new JLabel("Last 4 Digits:"));
            cardPanel.add(last4Field);
            cardPanel.add(new JLabel("Expiry (MM/YY):"));
            cardPanel.add(expiryField);

            int cardResult = JOptionPane.showConfirmDialog(this, cardPanel,
                    "Card Details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (cardResult != JOptionPane.OK_OPTION) return;

            cardType = cardTypeField.getText().trim();
            cardFirst4 = first4Field.getText().trim();
            cardLast4 = last4Field.getText().trim();
            cardExpiry = expiryField.getText().trim();

            if (cardType.isEmpty() || cardFirst4.isEmpty() || cardLast4.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all card details.",
                        "Missing Details", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // build the Sale object
        Sale sale = new Sale();
        sale.setAccountId(holder != null ? holder.getAccountId() : null);
        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(discountAmount);
        sale.setVatAmount(vatAmount);
        sale.setTotal(total);
        sale.setPaymentMethod(paymentMethod);
        sale.setCardType(cardType);
        sale.setCardFirstFour(cardFirst4);
        sale.setCardLastFour(cardLast4);
        sale.setCardExpiry(cardExpiry);
        sale.setOnline(false);
        sale.setItems(new ArrayList<>(cartItems));

        // save it
        int saleId = salesDAO.recordSale(sale);

        if (saleId > 0) {
            // show the receipt
            String receipt = salesDAO.generateReceipt(saleId);

            JTextArea receiptArea = new JTextArea(receipt);
            receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
            receiptArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(receiptArea);
            scrollPane.setPreferredSize(new Dimension(450, 400));

            JOptionPane.showMessageDialog(this, scrollPane,
                    "Sale Complete - Receipt #" + saleId, JOptionPane.INFORMATION_MESSAGE);

            // clear the cart and refresh history
            clearCart();
            refreshHistory();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to record sale.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Shows the receipt for the selected sale in the history table.
     */
    private void viewSelectedReceipt() {
        int selectedRow = salesHistoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a sale to view its receipt.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int saleId = (int) historyModel.getValueAt(selectedRow, 0);
        String receipt = salesDAO.generateReceipt(saleId);

        JTextArea receiptArea = new JTextArea(receipt);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        receiptArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(receiptArea);
        scrollPane.setPreferredSize(new Dimension(450, 400));

        JOptionPane.showMessageDialog(this, scrollPane,
                "Receipt #" + saleId, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Reloads the sales history table.
     */
    private void refreshHistory() {
        historyModel.setRowCount(0);
        List<Sale> sales = salesDAO.getAllSales();

        for (Sale sale : sales) {
            String customerDisplay;
            if (sale.getAccountId() != null) {
                AccountHolder h = customerDAO.getAccountHolderById(sale.getAccountId());
                customerDisplay = h != null ? h.getFullName() : "Account #" + sale.getAccountId();
            } else {
                customerDisplay = "Walk-in";
            }

            historyModel.addRow(new Object[]{
                    sale.getSaleId(),
                    sale.getSaleDate(),
                    customerDisplay,
                    String.format("%.2f", sale.getTotal()),
                    sale.getPaymentMethod()
            });
        }
    }
}