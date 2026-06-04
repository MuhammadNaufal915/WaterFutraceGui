package dao;

import database.DatabaseHelper;
import model.WaterFootprint;

import java.sql.*;
import java.util.Optional;

/**
 * WaterFootprintDAO - JDBC Data Access Object untuk entitas WaterFootprint
 */
public class WaterFootprintDAO {

    public WaterFootprint insert(WaterFootprint wf) {
        String sql = "INSERT INTO waterfootprint (total_usage, category, calculate_footprint) VALUES (?,?,?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setFloat(1, wf.getTotalUsage());
            ps.setString(2, wf.getCategory());
            ps.setFloat(3, wf.getCalculateFootprint());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) wf.setIdWaterfootprint(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert waterfootprint error: " + e.getMessage(), e);
        }
        return wf;
    }

    public void update(WaterFootprint wf) {
        String sql = "UPDATE waterfootprint SET total_usage=?, category=?, calculate_footprint=? WHERE id_waterfootprint=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setFloat(1, wf.getTotalUsage());
            ps.setString(2, wf.getCategory());
            ps.setFloat(3, wf.getCalculateFootprint());
            ps.setInt(4, wf.getIdWaterfootprint());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update waterfootprint error: " + e.getMessage(), e);
        }
    }

    public WaterFootprint save(WaterFootprint wf) {
        if (wf.getIdWaterfootprint() == 0) return insert(wf);
        update(wf);
        return wf;
    }

    public Optional<WaterFootprint> findById(int id) {
        String sql = "SELECT * FROM waterfootprint WHERE id_waterfootprint = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById waterfootprint error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public WaterFootprint mapRow(ResultSet rs) throws SQLException {
        WaterFootprint wf = new WaterFootprint();
        wf.setIdWaterfootprint(rs.getInt("id_waterfootprint"));
        wf.setTotalUsage(rs.getFloat("total_usage"));
        wf.setCategory(rs.getString("category"));
        wf.setCalculateFootprint(rs.getFloat("calculate_footprint"));
        return wf;
    }
}
