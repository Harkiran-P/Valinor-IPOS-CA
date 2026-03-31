package com.valinor.iposca.gui;

import com.valinor.iposca.Main;
import com.valinor.iposca.dao.UserDAO;
import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.ApplicationUser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The account sign in window for IPOS-CA-USER package.
 * Checks username and password with database before opening main application
 * User information is also passed to main application to allow access level handling
 * Test button is for testing purposes only and should be removed in the final version
 */
public class SignInFrame extends JFrame{

    public SignInFrame() {
        UserDAO userDAO = new UserDAO();

        setTitle("IPOS-CA SIGN IN");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(300, 200);
        setMinimumSize(new Dimension(300, 200));
        setLocationRelativeTo(null);

        // Close the database connection properly when the window is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatabaseManager.closeConnection();
                System.exit(0);
            }
        });

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);

        // Create input fields
        JTextField usernameField = new JTextField(16);
        JPasswordField passwordField = new JPasswordField(16);
        JButton submitButton = new JButton("SUBMIT");
        JLabel messageLabel = new JLabel("");

        JButton testButton = new JButton("SKIP LOGIN FOR TESTING");

        // Uses grid bag constraints to evenly space panel components
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        formPanel.add(messageLabel, gbc);
        gbc.gridy = 3;
        formPanel.add(submitButton, gbc);

        // REMOVE TEST BUTTON FOR FINAL VERSION
        gbc.gridy = 4;
        formPanel.add(testButton,gbc);

        this.getContentPane().add(formPanel);

        // Searches database for matching username and password
        // If username and password are correct then application opens
        submitButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if(username.equals("") || password.equals("")){
                messageLabel.setText("Missing Username Or Password");
            }
            else{
                ApplicationUser user = userDAO.getUserFromUsername(username);
                if(user == null){
                    messageLabel.setText("Invalid Username");
                }
                else{
                    if(!user.getPassword().equals(password)){
                        messageLabel.setText("Incorrect Password");
                    }
                    else{
                        MainFrame frame = new MainFrame(user);
                        frame.setVisible(true);
                        this.dispose();
                    }
                }
            }
        });

        // REMOVE TEST BUTTON FOR FINAL VERSION
        // Button allows full access to all application packages without a log in
        // Useful for when testing code as you don't have to type a log in every time
        testButton.addActionListener(e -> {
            ApplicationUser user = new ApplicationUser();
            user.setRole("Test");
            user.setUsername("Test");
            MainFrame frame = new MainFrame(user);
            frame.setVisible(true);
            this.dispose();
        });
    }
}
