package com.valinor.iposca.gui;

import com.valinor.iposca.db.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The main application window for IPOS-CA.
 * Uses tabs to organise the different packages (Stock, Sales, Customers, etc.).
 * More tabs will be added as we build the other packages.
 */
public class MainFrame extends JFrame {

    private JTabbedPane tabbedPane;

    public MainFrame() {
        setTitle("IPOS-CA - InfoPharma Client Application (Team 14 - Valinor)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 500));
        setLocationRelativeTo(null);

        // Close the database connection properly when the window is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatabaseManager.closeConnection();
                System.exit(0);
            }
        });

        // Create the tabbed panel that holds all the different screens
        tabbedPane = new JTabbedPane();

        // Add the Stock Management tab
        tabbedPane.addTab("Stock Management", new StockPanel());

        // Add the Customer Management tab
        tabbedPane.addTab("Customers", new CustomerPanel());

        // Add the Sales tab
        tabbedPane.addTab("Sales", new SalesPanel());

        // Placeholder tabs for future packages (will be built later)
        tabbedPane.addTab("Orders (IPOS-SA)", createPlaceholderPanel("Orders module - coming soon"));
        tabbedPane.addTab("Reports", createPlaceholderPanel("Report generation - coming soon"));
        tabbedPane.addTab("Templates", createPlaceholderPanel("Template management - coming soon"));

        add(tabbedPane);
    }

    /**
     * Creates a simple placeholder panel with a message.
     * Used for tabs that haven't been built yet.
     */
    private JPanel createPlaceholderPanel(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.ITALIC, 16));
        label.setForeground(Color.GRAY);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}