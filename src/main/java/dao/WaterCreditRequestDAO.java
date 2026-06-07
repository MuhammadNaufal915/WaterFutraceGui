package dao;

import database.DatabaseHelper;
import model.Individual;
import model.WaterCreditRequest;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * WaterCreditRequestDAO - JDBC Data Access Object untuk entitas WaterCreditRequest.
 */
public class WaterCreditRequestDAO {

    private final IndividualDAO individualDAO = new IndividualDAO();

    public WaterCreditRequest insert(WaterCreditRequest req) {
        String sql = """
            INSERT INTO water_credit_request (id_buyer, id_seller, amount, mode, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, req.getBuyer().getIdIndividual());
            if (req.getSeller() != null) {
                ps.setInt(2, req.getSeller().getIdIndividual());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setDouble(3, req.getAmount());
            ps.setString(4, req.getMode().name());
            ps.setString(5, req.getStatus().name());
            ps.setTimestamp(6, Timestamp.valueOf(req.getCreatedAt() != null ? req.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) req.setIdRequest(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert water credit request error: " + e.getMessage(), e);
        }
        return req;
    }

    public void update(WaterCreditRequest req) {
        String sql = """
            UPDATE water_credit_request 
            SET id_buyer = ?, id_seller = ?, amount = ?, mode = ?, status = ?, created_at = ?
            WHERE id_request = ?
        """;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, req.getBuyer().getIdIndividual());
            if (req.getSeller() != null) {
                ps.setInt(2, req.getSeller().getIdIndividual());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setDouble(3, req.getAmount());
            ps.setString(4, req.getMode().name());
            ps.setString(5, req.getStatus().name());
            ps.setTimestamp(6, Timestamp.valueOf(req.getCreatedAt() != null ? req.getCreatedAt() : LocalDateTime.now()));
            ps.setInt(7, req.getIdRequest());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update water credit request error: " + e.getMessage(), e);
        }
    }

    public Optional<WaterCreditRequest> findById(int id) {
        String sql = "SELECT * FROM water_credit_request WHERE id_request = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById water credit request error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<WaterCreditRequest> findByBuyer(int buyerId) {
        List<WaterCreditRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM water_credit_request WHERE id_buyer = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByBuyer water credit request error: " + e.getMessage(), e);
        }
        return list;
    }

    public List<WaterCreditRequest> findBySeller(int sellerId) {
        List<WaterCreditRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM water_credit_request WHERE id_seller = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findBySeller water credit request error: " + e.getMessage(), e);
        }
        return list;
    }

    public List<WaterCreditRequest> findPendingBroadcasts(int currentIndividualId) {
        List<WaterCreditRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM water_credit_request WHERE id_seller IS NULL AND status = 'PENDING' AND id_buyer != ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentIndividualId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findPendingBroadcasts error: " + e.getMessage(), e);
        }
        return list;
    }

    public List<WaterCreditRequest> findAll() {
        List<WaterCreditRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM water_credit_request ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll water credit request error: " + e.getMessage(), e);
        }
        return list;
    }

    private WaterCreditRequest mapRow(ResultSet rs) throws SQLException {
        WaterCreditRequest req = new WaterCreditRequest();
        req.setIdRequest(rs.getInt("id_request"));
        req.setAmount(rs.getDouble("amount"));
        req.setMode(WaterCreditRequest.RequestMode.valueOf(rs.getString("mode")));
        req.setStatus(WaterCreditRequest.RequestStatus.valueOf(rs.getString("status")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) req.setCreatedAt(ts.toLocalDateTime());

        int buyerId = rs.getInt("id_buyer");
        individualDAO.findById(buyerId).ifPresent(req::setBuyer);

        int sellerId = rs.getInt("id_seller");
        if (!rs.wasNull()) {
            individualDAO.findById(sellerId).ifPresent(req::setSeller);
        }

        return req;
    }
}
