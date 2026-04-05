package com.valinor.iposca.gui;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.ApplicationUser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The main application window for IPOS-CA.
 * Uses tabs to organise the different packages (Stock, Sales, Customers, etc.).
 * Which tabs are visible depends on the user's role.
 */
public class MainFrame extends JFrame {

    private JTabbedPane tabbedPane;

    public MainFrame(ApplicationUser user) {
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

        JPanel mainPanel = new JPanel(new BorderLayout());

        // Top bar showing current user and sign out button
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel userLabel = new JLabel("USER: " + user.getUsername() + "  |  ROLE: " + user.getRole());
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        JButton signOutButton = new JButton("Sign Out");
        signOutButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        signOutButton.addActionListener(e -> {
            SignInFrame frame = new SignInFrame();
            frame.setVisible(true);
            this.dispose();
        });

        topPanel.add(userLabel, BorderLayout.WEST);
        topPanel.add(signOutButton, BorderLayout.EAST);

        // Create the tabbed panel that holds the different screens
        tabbedPane = new JTabbedPane();

        // Show tabs based on the user's role
        if (user.getRole().equals("Admin")) {
            tabbedPane.addTab("Users", new UserPanel());
        } else {
            tabbedPane.addTab("Stock Management", new StockPanel());
            tabbedPane.addTab("Customers", new CustomerPanel());
            tabbedPane.addTab("Sales", new SalesPanel());
            tabbedPane.addTab("Orders (IPOS-SA)", createPlaceholderPanel("Orders module - coming soon"));
            tabbedPane.addTab("Templates", createPlaceholderPanel("Template management - coming soon"));

            if (user.getRole().equals("Manager")) {
                tabbedPane.addTab("Reports", createPlaceholderPanel("Report generation - coming soon"));
            }
        }

        // Test role gives access to everything - remove before final demo
        if (user.getRole().equals("Test")) {
            tabbedPane.addTab("Reports", createPlaceholderPanel("Report generation - coming soon"));
            tabbedPane.addTab("Users", new UserPanel());
        }

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        add(mainPanel);
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