package dao;

import database.DatabaseHelper;
import model.Product;
import model.Transaction;
import model.TransactionItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TransactionItemDAO - JDBC Data Access Object untuk entitas TransactionItem
 */
public class TransactionItemDAO {

    private final ProductDAO     productDAO     = new ProductDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    public TransactionItem insert(TransactionItem item) {
        String sql = "INSERT INTO transaction_items (id_transaction, id_product, quantity) VALUES (?,?,?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getTransaction().getIdTransaction());
            ps.setInt(2, item.getProduct().getIdProduct());
            ps.setInt(3, item.getQuantity());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setIdTransactionItem(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert transactionItem error: " + e.getMessage(), e);
        }
        return item;
    }

    public List<TransactionItem> findByTransaction(Transaction transaction) {
        List<TransactionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM transaction_items WHERE id_transaction = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transaction.getIdTransaction());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransactionItem item = new TransactionItem();
                    item.setIdTransactionItem(rs.getInt("id_transaction_item"));
                    item.setTransaction(transaction);
                    item.setQuantity(rs.getInt("quantity"));
                    int productId = rs.getInt("id_product");
                    productDAO.findById(productId).ifPresent(item::setProduct);
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByTransaction error: " + e.getMessage(), e);
        }
        return list;
    }
}
