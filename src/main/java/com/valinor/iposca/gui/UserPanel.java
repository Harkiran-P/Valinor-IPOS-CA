package com.valinor.iposca.gui;

import com.valinor.iposca.dao.UserDAO;
import com.valinor.iposca.model.ApplicationUser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * GUI panel for the IPOS-CA-USER package.
 * Lets an admin manage user accounts.
 */
public class UserPanel extends JPanel{
    private UserDAO userDAO;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    private final String[] columnNames = {
            "User ID", "Username", "Password", "Role", "Creation Date"
    };

    public UserPanel() {
        userDAO = new UserDAO();
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

        JLabel titleLabel = new JLabel("User Account Management");
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
     * Creates the table that displays all users.
     */
    private JScrollPane createTablePanel() {
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setRowHeight(25);
        userTable.getTableHeader().setReorderingAllowed(false);

        return new JScrollPane(userTable);
    }

    /**
     * Creates buttons at the bottom of the screen.
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));

        JButton addButton = new JButton("Add User");
        addButton.addActionListener(e -> showAddUserDialog());
        buttonPanel.add(addButton);

        JButton deleteButton = new JButton("Delete User");
        deleteButton.addActionListener(e -> deleteSelectedUser());
        buttonPanel.add(deleteButton);

        return buttonPanel;
    }

    /**
     * Reloads all user data from the database into the table.
     */
    private void refreshTable() {
        tableModel.setRowCount(0);
        List<ApplicationUser> users = userDAO.getAllUsers();

        for (ApplicationUser user : users) {
            addUserToTable(user);
        }
    }

    /**
     * Adds one user as a row in the table.
     */
    private void addUserToTable(ApplicationUser user) {
        tableModel.addRow(new Object[]{
                user.getUserID(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getCreatedAt()
        });
    }

    /**
     * Searches users by the keyword in the search box.
     */
    private void performSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            refreshTable();
            return;
        }

        tableModel.setRowCount(0);
        List<ApplicationUser> users = userDAO.searchUsers(keyword);

        for (ApplicationUser user : users) {
            addUserToTable(user);
        }

        if (users.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No users found matching: " + keyword,
                    "Search Results", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Shows a dialog to create a new user.
     */
    private void showAddUserDialog() {
        JTextField usernameField = new JTextField(15);
        JTextField passwordField = new JTextField(20);

        // Role type dropdown
        JComboBox<String> roleTypeBox = new JComboBox<>(new String[]{"Pharmacist", "Manager", "Admin"});

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        formPanel.add(new JLabel("Username:*"));
        formPanel.add(usernameField);
        formPanel.add(new JLabel("Password:*"));
        formPanel.add(passwordField);
        formPanel.add(new JLabel("Role:"));
        formPanel.add(roleTypeBox);

        int result = JOptionPane.showConfirmDialog(this, formPanel,
                "Add New User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String username = usernameField.getText().trim();
                String password = passwordField.getText().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Username and Password are required.",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ApplicationUser user = new ApplicationUser();
                user.setUsername(username);
                user.setPassword(password);
                user.setRole((String) roleTypeBox.getSelectedItem());

                int newId = userDAO.createUser(user);
                if (newId > 0) {
                    JOptionPane.showMessageDialog(this,
                            "User created successfully. User ID: " + newId);
                    refreshTable();
                } else if(newId == -1){
                    JOptionPane.showMessageDialog(this, "Username is taken.",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                } else{
                    JOptionPane.showMessageDialog(this, "Failed to create user.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to create user.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Deletes the selected user after confirmation.
     */
    private void deleteSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int userId = (int) tableModel.getValueAt(selectedRow, 0);
        String username = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete user:\n" + username +
                        " (User ID: " + userId + ")?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (userDAO.deleteUser(userId)) {
                JOptionPane.showMessageDialog(this, "User deleted successfully.");
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete user.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
