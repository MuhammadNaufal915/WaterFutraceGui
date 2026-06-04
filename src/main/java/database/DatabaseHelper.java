package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseHelper
 * Menyediakan koneksi JDBC ke MySQL.
 * Konfigurasi database disesuaikan di sini.
 */
public class DatabaseHelper {

    // =============================================
    //  KONFIGURASI DATABASE - Sesuaikan di sini
    // =============================================
    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    private static final String DB_NAME  = "waterfutrace";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private static final String URL =
        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
        + "?useSSL=false"
        + "&serverTimezone=UTC"
        + "&allowPublicKeyRetrieval=true"
        + "&useUnicode=true"
        + "&characterEncoding=UTF-8"
        + "&createDatabaseIfNotExist=true";

    /**
     * Mendapatkan koneksi baru ke database MySQL.
     * Gunakan try-with-resources untuk menutup koneksi secara otomatis.
     *
     * @return Connection JDBC
     * @throws SQLException jika koneksi gagal
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    /**
     * Test apakah koneksi ke database berhasil.
     * @return true jika berhasil
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[DatabaseHelper] Koneksi gagal: " + e.getMessage());
            return false;
        }
    }
}
