package com.complaints.swing;

import com.complaints.controller.ApiServer;
import com.complaints.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Application entry point.
 * Starts the REST API server, initialises the DB pool,
 * then shows the Swing launcher window.
 */
public class MainApp {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        // ── Init DB & REST ──────────────────
        try {
            System.out.println("Starting Complaint Management System...");
            System.out.println("Connecting to database...");
            DatabaseUtil.init();
            System.out.println("Database connected successfully!");
            System.out.println("Starting REST API server on port 8080...");
            ApiServer api = new ApiServer();
            api.start();
            System.out.println("REST API started on http://localhost:8080");

            // Shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                api.stop();
                DatabaseUtil.close();
                log.info("Application stopped.");
            }));
        } catch (Exception e) {
            System.out.println("FATAL ERROR: " + e.getMessage());
            System.out.println("Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "unknown"));
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Failed to start server:\n" + e.getMessage(),
                "Fatal Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // ── Launch Swing UI ─────────────────
        SwingUtilities.invokeLater(MainApp::showLauncher);
    }

    private static void showLauncher() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        JFrame frame = new JFrame("Complaint Management System — Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(520, 360);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        // ── Header ─────────────────────────
        JPanel header = new JPanel();
        header.setBackground(new Color(30, 41, 59));
        header.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 20));
        JLabel title = new JLabel("Complaint Management System");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        header.add(title);
        frame.add(header, BorderLayout.NORTH);

        // ── Body ───────────────────────────
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(new Color(248, 250, 252));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 16, 8, 16);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.gridx  = 0;

        JLabel status = new JLabel("✅  REST API running on http://localhost:8080");
        status.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        status.setForeground(new Color(22, 163, 74));
        gbc.gridy = 0;
        body.add(status, gbc);

        JLabel info = new JLabel("<html><center>Open the web UI in your browser, or use the<br>login form below to manage complaints.</center></html>");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1;
        body.add(info, gbc);

        JButton openBrowser = new JButton("🌐  Open Web Interface");
        openBrowser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        openBrowser.setBackground(new Color(59, 130, 246));
        openBrowser.setForeground(Color.WHITE);
        openBrowser.setFocusPainted(false);
        openBrowser.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openBrowser.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new java.net.URI("http://localhost:5173"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame,
                    "Could not open browser. Navigate to: http://localhost:5173",
                    "Browser Error", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        gbc.gridy = 2;
        body.add(openBrowser, gbc);

        JSeparator sep = new JSeparator();
        gbc.gridy = 3;
        body.add(sep, gbc);

        JLabel dbLabel = new JLabel("Database: complaint_db @ localhost:3306");
        dbLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        dbLabel.setForeground(Color.GRAY);
        dbLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 4;
        body.add(dbLabel, gbc);

        frame.add(body, BorderLayout.CENTER);

        // ── Footer ─────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        footer.setBackground(new Color(241, 245, 249));
        JButton exitBtn = new JButton("Exit");
        exitBtn.addActionListener(e -> System.exit(0));
        footer.add(exitBtn);
        frame.add(footer, BorderLayout.SOUTH);

        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { System.exit(0); }
        });

        frame.setVisible(true);
    }
}
