package service;

import dao.CompanyDAO;
import dao.ProductDAO;
import model.Company;
import model.Product;
import model.User;

import java.util.List;

/**
 * ProductService - CRUD produk perusahaan dengan validasi water credit.
 * Migrasi dari Spring @Service ProductService.
 */
public class ProductService {

    private final ProductDAO productDAO = new ProductDAO();
    private final CompanyDAO companyDAO = new CompanyDAO();

    private Company getOrCreateCompany(User user) {
        return companyDAO.findByUser(user).orElseGet(() -> {
            Company c = new Company(user, "General");
            c.setWatercredit(0.0);
            return companyDAO.insert(c);
        });
    }

    /**
     * Company membuat produk baru.
     * Mengurangi water credit perusahaan sebesar waterCredit produk.
     */
    public Product createProduct(User user, String productName, float waterCredit,
                                 int entitas) {
        Company company = getOrCreateCompany(user);

        double saldo = company.getWatercredit() != null ? company.getWatercredit() : 0.0;
        if (saldo < waterCredit) {
            throw new RuntimeException(
                String.format("Kredit air tidak mencukupi. Saldo: %.2f, Butuh: %.2f", saldo, waterCredit));
        }

        // Kurangi water credit
        company.setWatercredit(saldo - waterCredit);
        companyDAO.update(company);

        // Buat produk
        Product product = new Product();
        product.setCompany(company);
        product.setProductName(productName);
        product.setWaterCredit(waterCredit);
        product.setEntitas(entitas);
        product.setHargaJual(entitas > 0 ? waterCredit / (float) entitas : 0f);

        return productDAO.insert(product);
    }

    /**
     * Ambil semua produk milik company user yang login.
     */
    public List<Product> getMyProducts(User user) {
        Company company = getOrCreateCompany(user);
        return productDAO.findByCompany(company);
    }

    /**
     * Update produk milik company.
     */
    public Product updateProduct(User user, int productId, String productName,
                                 float waterCredit, int entitas) {
        Company company = getOrCreateCompany(user);
        Product product = productDAO.findById(productId)
            .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan."));

        if (product.getCompany() == null ||
            product.getCompany().getIdCompany() != company.getIdCompany()) {
            throw new RuntimeException("Anda tidak memiliki izin untuk mengubah produk ini.");
        }

        product.setProductName(productName);
        product.setWaterCredit(waterCredit);
        product.setEntitas(entitas);
        product.setHargaJual(entitas > 0 ? waterCredit / (float) entitas : 0f);

        productDAO.update(product);
        return product;
    }

    /**
     * Hapus produk milik company.
     */
    public void deleteProduct(User user, int productId) {
        Company company = getOrCreateCompany(user);
        Product product = productDAO.findById(productId)
            .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan."));

        if (product.getCompany() == null ||
            product.getCompany().getIdCompany() != company.getIdCompany()) {
            throw new RuntimeException("Anda tidak memiliki izin untuk menghapus produk ini.");
        }

        productDAO.delete(productId);
    }

    /**
     * Ambil semua produk (untuk semua user authenticated).
     */
    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }
}
