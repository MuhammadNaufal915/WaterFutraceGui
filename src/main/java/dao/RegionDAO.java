package dao;

import database.DatabaseHelper;
import model.Region;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RegionDAO - JDBC Data Access Object untuk entitas Region
 */
public class RegionDAO {

    public List<Region> findAll() {
        List<Region> list = new ArrayList<>();
        String sql = "SELECT * FROM region ORDER BY name";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll region error: " + e.getMessage(), e);
        }
        return list;
    }

    public Optional<Region> findByName(String name) {
        String sql = "SELECT * FROM region WHERE name = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByName region error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Region> findById(int id) {
        String sql = "SELECT * FROM region WHERE id_region = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById region error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    private Region mapRow(ResultSet rs) throws SQLException {
        return new Region(rs.getInt("id_region"), rs.getString("name"));
    }
}
