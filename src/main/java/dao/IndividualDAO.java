package dao;

import database.DatabaseHelper;
import model.Government;
import model.Individual;
import model.Region;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * IndividualDAO - JDBC Data Access Object untuk entitas Individual
 */
public class IndividualDAO {

    private final UserDAO   userDAO   = new UserDAO();
    private final RegionDAO regionDAO = new RegionDAO();

    public Individual insert(Individual ind) {
        String sql = "INSERT INTO individual (id_user, age, pekerjaan, water_credit, id_government, id_region) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, ind.getUser().getIdUser());
            ps.setInt(2, ind.getAge());
            ps.setString(3, ind.getPekerjaan());
            ps.setDouble(4, ind.getWaterCredit() != null ? ind.getWaterCredit() : 0.0);
            ps.setString(5, ind.getGovernment() != null ? ind.getGovernment().getIdGovernment() : null);
            ps.setObject(6, ind.getRegion() != null ? ind.getRegion().getIdRegion() : null, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) ind.setIdIndividual(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert individual error: " + e.getMessage(), e);
        }
        return ind;
    }

    public void update(Individual ind) {
        String sql = "UPDATE individual SET id_user=?, age=?, pekerjaan=?, water_credit=?, id_government=?, id_region=? WHERE id_individual=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ind.getUser().getIdUser());
            ps.setInt(2, ind.getAge());
            ps.setString(3, ind.getPekerjaan());
            ps.setDouble(4, ind.getWaterCredit() != null ? ind.getWaterCredit() : 0.0);
            ps.setString(5, ind.getGovernment() != null ? ind.getGovernment().getIdGovernment() : null);
            ps.setObject(6, ind.getRegion() != null ? ind.getRegion().getIdRegion() : null, Types.INTEGER);
            ps.setInt(7, ind.getIdIndividual());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update individual error: " + e.getMessage(), e);
        }
    }

    public Optional<Individual> findByUser(User user) {
        String sql = "SELECT * FROM individual WHERE id_user = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, user.getIdUser());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs, user));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUser individual error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Individual> findById(int id) {
        String sql = "SELECT * FROM individual WHERE id_individual = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long userId = rs.getLong("id_user");
                    User user = userDAO.findById(userId).orElse(new User());
                    return Optional.of(mapRow(rs, user));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById individual error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Individual> findAll() {
        List<Individual> list = new ArrayList<>();
        String sql = "SELECT * FROM individual";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long userId = rs.getLong("id_user");
                User user = userDAO.findById(userId).orElse(new User());
                list.add(mapRow(rs, user));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll individual error: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Individual> findByRegion(Region region) {
        List<Individual> list = new ArrayList<>();
        String sql = "SELECT * FROM individual WHERE id_region = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, region.getIdRegion());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long userId = rs.getLong("id_user");
                    User user = userDAO.findById(userId).orElse(new User());
                    list.add(mapRow(rs, user));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByRegion individual error: " + e.getMessage(), e);
        }
        return list;
    }

    private Individual mapRow(ResultSet rs, User user) throws SQLException {
        Individual ind = new Individual();
        ind.setIdIndividual(rs.getInt("id_individual"));
        ind.setUser(user);
        ind.setAge(rs.getInt("age"));
        ind.setPekerjaan(rs.getString("pekerjaan"));
        ind.setWaterCredit(rs.getDouble("water_credit"));

        int regionId = rs.getInt("id_region");
        if (!rs.wasNull()) regionDAO.findById(regionId).ifPresent(ind::setRegion);

        String govId = rs.getString("id_government");
        if (govId != null) {
            Government gov = new Government();
            gov.setIdGovernment(govId);
            ind.setGovernment(gov);
        }
        return ind;
    }
}
