package dao;

import database.DatabaseHelper;
import model.Company;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ProductDAO - JDBC Data Access Object untuk entitas Product
 */
public class ProductDAO {

    private final CompanyDAO companyDAO = new CompanyDAO();

    public Product insert(Product product) {
        String sql = "INSERT INTO product (id_company, product_name, water_credit, entitas, harga_jual) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, product.getCompany().getIdCompany());
            ps.setString(2, product.getProductName());
            ps.setFloat(3, product.getWaterCredit());
            ps.setInt(4, product.getEntitas() != null ? product.getEntitas() : 1);
            ps.setObject(5, product.getHargaJual(), Types.FLOAT);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) product.setIdProduct(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert product error: " + e.getMessage(), e);
        }
        return product;
    }

    public void update(Product product) {
        String sql = "UPDATE product SET id_company=?, product_name=?, water_credit=?, entitas=?, harga_jual=? WHERE id_product=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, product.getCompany().getIdCompany());
            ps.setString(2, product.getProductName());
            ps.setFloat(3, product.getWaterCredit());
            ps.setInt(4, product.getEntitas() != null ? product.getEntitas() : 1);
            ps.setObject(5, product.getHargaJual(), Types.FLOAT);
            ps.setInt(6, product.getIdProduct());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update product error: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM product WHERE id_product = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete product error: " + e.getMessage(), e);
        }
    }

    public Optional<Product> findById(int id) {
        String sql = "SELECT * FROM product WHERE id_product = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById product error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM product";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll product error: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Product> findByCompany(Company company) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM product WHERE id_company = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, company.getIdCompany());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByCompany product error: " + e.getMessage(), e);
        }
        return list;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setIdProduct(rs.getInt("id_product"));
        p.setProductName(rs.getString("product_name"));
        p.setWaterCredit(rs.getFloat("water_credit"));
        p.setEntitas(rs.getInt("entitas"));
        float hj = rs.getFloat("harga_jual");
        if (!rs.wasNull()) p.setHargaJual(hj);

        int companyId = rs.getInt("id_company");
        companyDAO.findById(companyId).ifPresent(p::setCompany);
        return p;
    }
}
