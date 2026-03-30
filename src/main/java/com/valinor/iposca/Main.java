package com.valinor.iposca;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.gui.MainFrame;

import javax.swing.*;

/**
 * entry point for the IPOS-CA application.
 * sets up the database and launches the main window.
 */
public class Main {

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {

            System.err.println("Could not set system appearance: " + e.getMessage());
        }

        // Create all database tables
        DatabaseManager.initialiseDatabase();

        // Launch the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
