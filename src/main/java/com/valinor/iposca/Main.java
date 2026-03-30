package com.valinor.iposca;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.gui.MainFrame;

import javax.swing.*;

/**
 * Entry point for the IPOS-CA application.
 * Sets up the database and launches the main window.
 */
public class Main {

    public static void main(String[] args) {

        // Set the look and feel to match the operating system (Windows/Mac/Linux)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // If it fails, just use the default Java look - not a big deal
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        // Create all database tables (does nothing if they already exist)
        DatabaseManager.initialiseDatabase();

        // Launch the GUI on the Event Dispatch Thread (required by Swing)
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
