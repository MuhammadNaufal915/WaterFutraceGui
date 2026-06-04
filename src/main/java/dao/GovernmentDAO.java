package dao;

import database.DatabaseHelper;
import model.Government;
import model.Region;
import model.User;
import model.WaterFootprint;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * GovernmentDAO - JDBC Data Access Object untuk entitas Government
 */
public class GovernmentDAO {

    private final UserDAO            userDAO    = new UserDAO();
    private final RegionDAO          regionDAO  = new RegionDAO();
    private final WaterFootprintDAO  wfDAO      = new WaterFootprintDAO();

    public Government insert(Government gov) {
        String sql = "INSERT INTO government (id_government, id_user, waterbasin, id_region, id_waterfootprint) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, gov.getIdGovernment());
            ps.setLong(2, gov.getUser().getIdUser());
            ps.setString(3, gov.getWaterbasin());
            ps.setObject(4, gov.getRegion() != null ? gov.getRegion().getIdRegion() : null, Types.INTEGER);
            ps.setObject(5, gov.getWaterFootprint() != null ? gov.getWaterFootprint().getIdWaterfootprint() : null, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insert government error: " + e.getMessage(), e);
        }
        return gov;
    }

    public void update(Government gov) {
        String sql = "UPDATE government SET id_user=?, waterbasin=?, id_region=?, id_waterfootprint=? WHERE id_government=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gov.getUser().getIdUser());
            ps.setString(2, gov.getWaterbasin());
            ps.setObject(3, gov.getRegion() != null ? gov.getRegion().getIdRegion() : null, Types.INTEGER);
            ps.setObject(4, gov.getWaterFootprint() != null ? gov.getWaterFootprint().getIdWaterfootprint() : null, Types.INTEGER);
            ps.setString(5, gov.getIdGovernment());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update government error: " + e.getMessage(), e);
        }
    }

    public Optional<Government> findByUser(User user) {
        String sql = "SELECT * FROM government WHERE id_user = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, user.getIdUser());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs, user));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUser government error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Government> findFirstByRegion(Region region) {
        String sql = "SELECT * FROM government WHERE id_region = ? LIMIT 1";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, region.getIdRegion());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long userId = rs.getLong("id_user");
                    User user = userDAO.findById(userId).orElseThrow();
                    return Optional.of(mapRow(rs, user));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findFirstByRegion government error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Government> findAll() {
        List<Government> list = new ArrayList<>();
        String sql = "SELECT * FROM government";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long userId = rs.getLong("id_user");
                User user = userDAO.findById(userId).orElse(new User());
                list.add(mapRow(rs, user));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll government error: " + e.getMessage(), e);
        }
        return list;
    }

    private Government mapRow(ResultSet rs, User user) throws SQLException {
        Government gov = new Government();
        gov.setIdGovernment(rs.getString("id_government"));
        gov.setUser(user);
        gov.setWaterbasin(rs.getString("waterbasin"));

        int regionId = rs.getInt("id_region");
        if (!rs.wasNull()) regionDAO.findById(regionId).ifPresent(gov::setRegion);

        int wfId = rs.getInt("id_waterfootprint");
        if (!rs.wasNull()) wfDAO.findById(wfId).ifPresent(gov::setWaterFootprint);

        return gov;
    }
}
