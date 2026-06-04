package database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseInit
 * Membuat semua tabel database secara otomatis saat pertama kali dijalankan.
 * Mengonversi JPA @Entity menjadi SQL CREATE TABLE IF NOT EXISTS.
 */
public class DatabaseInit {

    /**
     * Inisialisasi seluruh skema database dan seed data region.
     * Dipanggil sekali saat aplikasi startup.
     */
    public static void initializeDatabase() {
        System.out.println("[DatabaseInit] Menginisialisasi database...");
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement()) {

            // Disable foreign key checks sementara untuk kemudahan urutan CREATE
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            createTableRegion(stmt);
            createTableUser(stmt);
            createTableWaterFootprint(stmt);
            createTableGovernment(stmt);
            createTableCompany(stmt);
            createTableIndividual(stmt);
            createTableProduct(stmt);
            createTableTransactions(stmt);
            createTableTransactionItems(stmt);
            createTableComplains(stmt);

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            seedRegions(stmt);

            System.out.println("[DatabaseInit] Database siap.");
        } catch (SQLException e) {
            System.err.println("[DatabaseInit] ERROR: " + e.getMessage());
            throw new RuntimeException("Gagal menginisialisasi database: " + e.getMessage(), e);
        }
    }

    // ==================== TABLE CREATION ====================

    private static void createTableRegion(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS region (
                id_region INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100) NOT NULL UNIQUE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);
        System.out.println("[DatabaseInit] Tabel 'region' OK");
    }

    private static void createTableUser(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS user (
                id_user BIGINT AUTO_INCREMENT PRIMARY KEY,
                email   VARCHAR(255) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                name    VARCHAR(255),
                alamat  TEXT,
                role    ENUM('INDIVIDUAL','GOVERMENT','COMPANY') NOT NULL,
                is_approved BOOLEAN NOT NULL DEFAULT FALSE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);
        System.out.println("[DatabaseInit] Tabel 'user' OK");
    }

    private static void createTableWaterFootprint(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS waterfootprint (
                id_waterfootprint INT AUTO_INCREMENT PRIMARY KEY,
                total_usage       FLOAT,
                category          VARCHAR(100),
                calculate_footprint FLOAT
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);
        System.out.println("[DatabaseInit] Tabel 'waterfootprint' OK");
    }

    private static void createTableGovernment(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS government (
                id_government     VARCHAR(36) PRIMARY KEY,
                id_user           BIGINT NOT NULL,
                waterbasin        VARCHAR(255),
                id_region         INT,
                id_waterfootprint INT,
                FOREIGN KEY (id_user)           REFERENCES user(id_user)                     ON DELETE CASCADE,
                FOREIGN KEY (id_region)         REFERENCES region(id_region)                  ON DELETE SET NULL,
                FOREIGN KEY (id_waterfootprint) REFERENCES waterfootprint(id_waterfootprint)  ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);
        System.out.println("[DatabaseInit] Tabel 'government' OK");
    }

    private static void createTableCompany(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS company (
                id_company    INT AUTO_INCREMENT PRIMARY KEY,
                id_user       BIGINT NOT NULL,
                sector        VARCHAR(255),
                watercredit   DOUBLE DEFAULT 0.0,
                eta           FLOAT,
                sl            FLOAT,
                id_government VARCHAR(36),
                id_region     INT,
                FOREIGN KEY (id_user)       REFERENCES user(id_user)           ON DELETE CASCADE,
                FOREIGN KEY (id_government) REFERENCES government(id_government) ON DELETE SET NULL,
                FOREIGN KEY (id_region)     REFERENCES region(id_region)        ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);
        System.out.println("[DatabaseInit] Tabel 'company' OK");
    }

    private static void createTableIndividual(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS individual (
                id_individual INT AUTO_INCREMENT PRIMARY KEY,
                id_user       BIGINT NOT NULL,
                age           INT,
                pekerjaan     VARCHAR(255),
                water_credit  DOUBLE DEFAULT 0.0,
                id_government VARCHAR(36),
                id_region     INT,
                FOREIGN KEY (id_user)       REFERENCES user(id_user)           ON DELETE CASCADE,
                FOREIGN KEY (id_government) REFERENCES government(id_government) ON DELETE SET NULL,
                FOREIGN KEY (id_region)     REFERENCES region(id_region)        ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);
        System.out.println("[DatabaseInit] Tabel 'individual' OK");
    }

    private static void createTableProduct(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS product (
                id_product   INT AUTO_INCREMENT PRIMARY KEY,
                id_company   INT NOT NULL,
                product_name VARCHAR(255),
                water_credit FLOAT,
                entitas      INT DEFAULT 1,
                harga_jual   FLOAT,
                FOREIGN KEY (id_company) REFERENCES company(id_company) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);
        System.out.println("[DatabaseInit] Tabel 'product' OK");
    }

    private static void createTableTransactions(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS transactions (
                id_transaction INT AUTO_INCREMENT PRIMARY KEY,
                id_individual  INT NOT NULL,
                id_product     INT NOT NULL,
                quantity       INT NOT NULL,
                total_price    FLOAT NOT NULL,
                created_at     DATETIME NOT NULL,
                FOREIGN KEY (id_individual) REFERENCES individual(id_individual) ON DELETE CASCADE,
                FOREIGN KEY (id_product)    REFERENCES product(id_product)       ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);
        System.out.println("[DatabaseInit] Tabel 'transactions' OK");
    }

    private static void createTableTransactionItems(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS transaction_items (
                id_transaction_item INT AUTO_INCREMENT PRIMARY KEY,
                id_transaction      INT NOT NULL,
                id_product          INT NOT NULL,
                quantity            INT NOT NULL,
                FOREIGN KEY (id_transaction) REFERENCES transactions(id_transaction) ON DELETE CASCADE,
                FOREIGN KEY (id_product)     REFERENCES product(id_product)         ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);
        System.out.println("[DatabaseInit] Tabel 'transaction_items' OK");
    }

    private static void createTableComplains(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS complains (
                id_complain  VARCHAR(36) PRIMARY KEY,
                id_sender    BIGINT NOT NULL,
                id_receiver  BIGINT NOT NULL,
                title        VARCHAR(255) NOT NULL,
                description  TEXT NOT NULL,
                status       ENUM('PENDING','IN_PROGRESS','RESOLVED','REJECTED') NOT NULL DEFAULT 'PENDING',
                created_at   DATETIME NOT NULL,
                updated_at   DATETIME,
                reply        TEXT,
                replied_at   DATETIME,
                FOREIGN KEY (id_sender)   REFERENCES user(id_user) ON DELETE CASCADE,
                FOREIGN KEY (id_receiver) REFERENCES user(id_user) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);
        System.out.println("[DatabaseInit] Tabel 'complains' OK");
    }

    // ==================== SEED DATA ====================

    private static void seedRegions(Statement stmt) throws SQLException {
        String[] regions = {
            "DKI Jakarta", "Jawa Barat", "Jawa Tengah", "Jawa Timur",
            "Banten", "DI Yogyakarta", "Sumatera Utara", "Sumatera Selatan",
            "Kalimantan Barat", "Kalimantan Timur", "Sulawesi Selatan", "Bali"
        };

        for (String region : regions) {
            try {
                stmt.execute("INSERT IGNORE INTO region (name) VALUES ('" + region + "')");
            } catch (SQLException e) {
                // Abaikan jika sudah ada (IGNORE harusnya cukup)
            }
        }
        System.out.println("[DatabaseInit] Seed region selesai.");
    }
}
