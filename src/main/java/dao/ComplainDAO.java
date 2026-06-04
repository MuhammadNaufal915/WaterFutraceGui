package dao;

import database.DatabaseHelper;
import model.Complain;
import model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ComplainDAO - JDBC Data Access Object untuk entitas Complain
 */
public class ComplainDAO {

    private final UserDAO userDAO = new UserDAO();

    public Complain insert(Complain complain) {
        String sql = """
            INSERT INTO complains
                (id_complain, id_sender, id_receiver, title, description, status, created_at)
            VALUES (?,?,?,?,?,?,?)
        """;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, complain.getIdComplaint());
            ps.setLong(2, complain.getSender().getIdUser());
            ps.setLong(3, complain.getReceiver().getIdUser());
            ps.setString(4, complain.getTitle());
            ps.setString(5, complain.getDescription());
            ps.setString(6, complain.getStatus().name());
            ps.setTimestamp(7, Timestamp.valueOf(
                complain.getCreatedAt() != null ? complain.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insert complain error: " + e.getMessage(), e);
        }
        return complain;
    }

    public void update(Complain complain) {
        String sql = """
            UPDATE complains SET
                title=?, description=?, status=?, updated_at=?, reply=?, replied_at=?
            WHERE id_complain=?
        """;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, complain.getTitle());
            ps.setString(2, complain.getDescription());
            ps.setString(3, complain.getStatus().name());
            ps.setTimestamp(4, complain.getUpdatedAt() != null ? Timestamp.valueOf(complain.getUpdatedAt()) : null);
            ps.setString(5, complain.getReply());
            ps.setTimestamp(6, complain.getRepliedAt() != null ? Timestamp.valueOf(complain.getRepliedAt()) : null);
            ps.setString(7, complain.getIdComplaint());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update complain error: " + e.getMessage(), e);
        }
    }

    public Optional<Complain> findById(String id) {
        String sql = "SELECT * FROM complains WHERE id_complain = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById complain error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Complain> findBySender(User sender) {
        return findByUserColumn("id_sender", sender.getIdUser());
    }

    public List<Complain> findByReceiver(User receiver) {
        return findByUserColumn("id_receiver", receiver.getIdUser());
    }

    private List<Complain> findByUserColumn(String column, long userId) {
        List<Complain> list = new ArrayList<>();
        String sql = "SELECT * FROM complains WHERE " + column + " = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findBy" + column + " complain error: " + e.getMessage(), e);
        }
        return list;
    }

    private Complain mapRow(ResultSet rs) throws SQLException {
        Complain c = new Complain();
        c.setIdComplaint(rs.getString("id_complain"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setStatus(Complain.ComplaintStatus.valueOf(rs.getString("status")));
        c.setReply(rs.getString("reply"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) c.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) c.setUpdatedAt(updatedAt.toLocalDateTime());
        Timestamp repliedAt = rs.getTimestamp("replied_at");
        if (repliedAt != null) c.setRepliedAt(repliedAt.toLocalDateTime());

        long senderId = rs.getLong("id_sender");
        userDAO.findById(senderId).ifPresent(c::setSender);
        long receiverId = rs.getLong("id_receiver");
        userDAO.findById(receiverId).ifPresent(c::setReceiver);

        return c;
    }
}
