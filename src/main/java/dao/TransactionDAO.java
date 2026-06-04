package dao;

import database.DatabaseHelper;
import model.Individual;
import model.Product;
import model.Transaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TransactionDAO - JDBC Data Access Object untuk entitas Transaction
 */
public class TransactionDAO {

    private final IndividualDAO individualDAO = new IndividualDAO();
    private final ProductDAO    productDAO    = new ProductDAO();

    public Transaction insert(Transaction tx) {
        String sql = "INSERT INTO transactions (id_individual, id_product, quantity, total_price, created_at) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tx.getBuyer().getIdIndividual());
            ps.setInt(2, tx.getProduct().getIdProduct());
            ps.setInt(3, tx.getQuantity());
            ps.setFloat(4, tx.getTotalPrice());
            ps.setTimestamp(5, Timestamp.valueOf(tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) tx.setIdTransaction(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert transaction error: " + e.getMessage(), e);
        }
        return tx;
    }

    public List<Transaction> findByBuyer(Individual buyer) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE id_individual = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyer.getIdIndividual());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs, buyer));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByBuyer transaction error: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Transaction> findAll() {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs, null));
        } catch (SQLException e) {
            throw new RuntimeException("findAll transaction error: " + e.getMessage(), e);
        }
        return list;
    }

    public Optional<Transaction> findById(int id) {
        String sql = "SELECT * FROM transactions WHERE id_transaction = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs, null));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById transaction error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    private Transaction mapRow(ResultSet rs, Individual knownBuyer) throws SQLException {
        Transaction tx = new Transaction();
        tx.setIdTransaction(rs.getInt("id_transaction"));
        tx.setQuantity(rs.getInt("quantity"));
        tx.setTotalPrice(rs.getFloat("total_price"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) tx.setCreatedAt(ts.toLocalDateTime());

        if (knownBuyer != null) {
            tx.setBuyer(knownBuyer);
        } else {
            int indId = rs.getInt("id_individual");
            individualDAO.findById(indId).ifPresent(tx::setBuyer);
        }

        int productId = rs.getInt("id_product");
        productDAO.findById(productId).ifPresent(tx::setProduct);

        return tx;
    }
}
