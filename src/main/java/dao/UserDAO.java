package dao;

import database.DatabaseHelper;
import model.ERole;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * UserDAO - JDBC Data Access Object untuk entitas User
 */
public class UserDAO {

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByEmail error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM user WHERE id_user = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user ORDER BY id_user";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll error: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Insert user baru, return user dengan id yang digenerate
     */
    public User insert(User user) {
        String sql = "INSERT INTO user (email, password, name, alamat, role, is_approved) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getName());
            ps.setString(4, user.getAlamat());
            ps.setString(5, user.getRole().name());
            ps.setBoolean(6, user.isApproved());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setIdUser(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert user error: " + e.getMessage(), e);
        }
        return user;
    }

    public void update(User user) {
        String sql = "UPDATE user SET email=?, password=?, name=?, alamat=?, role=?, is_approved=? WHERE id_user=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getName());
            ps.setString(4, user.getAlamat());
            ps.setString(5, user.getRole().name());
            ps.setBoolean(6, user.isApproved());
            ps.setLong(7, user.getIdUser());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update user error: " + e.getMessage(), e);
        }
    }

    public void approve(long userId) {
        String sql = "UPDATE user SET is_approved = TRUE WHERE id_user = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("approve user error: " + e.getMessage(), e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM user WHERE id_user = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete user error: " + e.getMessage(), e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setIdUser(rs.getLong("id_user"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setName(rs.getString("name"));
        u.setAlamat(rs.getString("alamat"));
        u.setRole(ERole.valueOf(rs.getString("role")));
        u.setApproved(rs.getBoolean("is_approved"));
        return u;
    }
}
