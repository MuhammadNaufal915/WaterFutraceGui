package app;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import database.DatabaseHelper;
import database.DatabaseInit;
import gui.LoginFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Main - Entry point aplikasi WaterFutrace Desktop GUI.
 *
 * Cara menjalankan:
 *   1. mvn clean package
 *   2. java -jar target/WaterFutrace.jar
 *
 * Atau langsung dari IDE: Run this class as Java Application.
 */
public class Main {

    public static void main(String[] args) {
        // 1. Terapkan FlatLaf Dark theme
        try {
            // Set UI properties untuk tampilan lebih modern
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.width", 8);
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 1));

            FlatDarkLaf.setup();
            UIManager.put("Table.background", new Color(18, 28, 50));
            UIManager.put("Table.alternateRowColor", new Color(22, 34, 58));
        } catch (Exception e) {
            System.err.println("[Main] FlatLaf gagal dimuat, menggunakan L&F default: " + e.getMessage());
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
        }

        // 2. Cek koneksi database
        System.out.println("[Main] Memeriksa koneksi database...");
        if (!DatabaseHelper.testConnection()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                    "<html><b>Gagal terhubung ke database!</b><br><br>" +
                    "Pastikan:<br>" +
                    "• MySQL server berjalan<br>" +
                    "• Database 'waterfutrace' sudah dibuat<br>" +
                    "• Kredensial di DatabaseHelper.java sudah benar<br><br>" +
                    "<small>Edit file: src/main/java/database/DatabaseHelper.java</small></html>",
                    "Kesalahan Koneksi Database",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            });
            return;
        }
        System.out.println("[Main] Koneksi database berhasil.");

        // 3. Inisialisasi skema database (CREATE TABLE IF NOT EXISTS)
        System.out.println("[Main] Menginisialisasi database...");
        try {
            DatabaseInit.initializeDatabase();
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                    "<html><b>Gagal menginisialisasi database!</b><br>" + e.getMessage() + "</html>",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            });
            return;
        }

        // 4. Jalankan aplikasi di Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            System.out.println("[Main] Memulai GUI WaterFutrace...");
            new LoginFrame();
        });
    }
}
