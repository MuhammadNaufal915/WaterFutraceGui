package dao;

import database.DatabaseHelper;
import model.Company;
import model.Government;
import model.Region;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CompanyDAO - JDBC Data Access Object untuk entitas Company
 */
public class CompanyDAO {

    private final UserDAO       userDAO    = new UserDAO();
    private final RegionDAO     regionDAO  = new RegionDAO();
    private final GovernmentDAO govDAO     = new GovernmentDAO();

    public Company insert(Company company) {
        String sql = "INSERT INTO company (id_user, sector, watercredit, eta, sl, id_government, id_region) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, company.getUser().getIdUser());
            ps.setString(2, company.getSector());
            ps.setDouble(3, company.getWatercredit() != null ? company.getWatercredit() : 0.0);
            ps.setObject(4, company.getEta(), Types.FLOAT);
            ps.setObject(5, company.getSl(), Types.FLOAT);
            ps.setString(6, company.getGovernment() != null ? company.getGovernment().getIdGovernment() : null);
            ps.setObject(7, company.getRegion() != null ? company.getRegion().getIdRegion() : null, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) company.setIdCompany(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert company error: " + e.getMessage(), e);
        }
        return company;
    }

    public void update(Company company) {
        String sql = "UPDATE company SET id_user=?, sector=?, watercredit=?, eta=?, sl=?, id_government=?, id_region=? WHERE id_company=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, company.getUser().getIdUser());
            ps.setString(2, company.getSector());
            ps.setDouble(3, company.getWatercredit() != null ? company.getWatercredit() : 0.0);
            ps.setObject(4, company.getEta(), Types.FLOAT);
            ps.setObject(5, company.getSl(), Types.FLOAT);
            ps.setString(6, company.getGovernment() != null ? company.getGovernment().getIdGovernment() : null);
            ps.setObject(7, company.getRegion() != null ? company.getRegion().getIdRegion() : null, Types.INTEGER);
            ps.setInt(8, company.getIdCompany());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update company error: " + e.getMessage(), e);
        }
    }

    public Optional<Company> findByUser(User user) {
        String sql = "SELECT * FROM company WHERE id_user = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, user.getIdUser());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUser company error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Company> findById(int id) {
        String sql = "SELECT * FROM company WHERE id_company = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById company error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Company> findAll() {
        List<Company> list = new ArrayList<>();
        String sql = "SELECT * FROM company";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll company error: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Company> findByRegion(Region region) {
        List<Company> list = new ArrayList<>();
        String sql = "SELECT * FROM company WHERE id_region = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, region.getIdRegion());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByRegion company error: " + e.getMessage(), e);
        }
        return list;
    }

    private Company mapRow(ResultSet rs) throws SQLException {
        Company c = new Company();
        c.setIdCompany(rs.getInt("id_company"));
        c.setSector(rs.getString("sector"));
        c.setWatercredit(rs.getDouble("watercredit"));
        float eta = rs.getFloat("eta"); if (!rs.wasNull()) c.setEta(eta);
        float sl  = rs.getFloat("sl");  if (!rs.wasNull()) c.setSl(sl);

        long userId = rs.getLong("id_user");
        userDAO.findById(userId).ifPresent(c::setUser);

        int regionId = rs.getInt("id_region");
        if (!rs.wasNull()) regionDAO.findById(regionId).ifPresent(c::setRegion);

        String govId = rs.getString("id_government");
        if (govId != null && c.getUser() != null) {
            // Load government lightly to avoid deep recursion
            String govSql = "SELECT * FROM government WHERE id_government = ?";
            try (Connection conn2 = DatabaseHelper.getConnection();
                 PreparedStatement ps2 = conn2.prepareStatement(govSql)) {
                ps2.setString(1, govId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) {
                        Government gov = new Government();
                        gov.setIdGovernment(rs2.getString("id_government"));
                        gov.setWaterbasin(rs2.getString("waterbasin"));
                        c.setGovernment(gov);
                    }
                }
            }
        }
        return c;
    }
}
