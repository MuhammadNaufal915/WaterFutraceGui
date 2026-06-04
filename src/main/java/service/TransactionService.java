package service;

import dao.IndividualDAO;
import dao.ProductDAO;
import dao.TransactionDAO;
import model.Individual;
import model.Product;
import model.Transaction;
import model.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TransactionService - Logika pembelian produk oleh Individual.
 * Migrasi dari Spring @Service TransactionService + TransactionController.
 */
public class TransactionService {

    private final IndividualDAO  individualDAO  = new IndividualDAO();
    private final ProductDAO     productDAO     = new ProductDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    /**
     * Individual membeli produk.
     * Mengurangi water credit pembeli dan stok produk.
     *
     * @param user      user yang sedang login (harus INDIVIDUAL)
     * @param productId ID produk yang dibeli
     * @param quantity  jumlah yang dibeli
     * @return Transaction yang berhasil dibuat
     */
    public Transaction purchase(User user, int productId, int quantity) {
        // Dapatkan profil individual
        Individual buyer = individualDAO.findByUser(user)
            .orElseThrow(() -> new RuntimeException(
                "Profil individual tidak ditemukan.\nPastikan akun Anda sudah disetujui oleh Government."));

        if (quantity <= 0)
            throw new RuntimeException("Jumlah pembelian harus lebih dari 0.");

        // Dapatkan produk
        Product product = productDAO.findById(productId)
            .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan."));

        int available = product.getEntitas() != null ? product.getEntitas() : 0;
        if (available < quantity)
            throw new RuntimeException(
                "Stok produk tidak mencukupi. Tersedia: " + available);

        Float hargaPerUnit = product.getHargaJual();
        if (hargaPerUnit == null || hargaPerUnit <= 0)
            throw new RuntimeException("Harga jual produk tidak tersedia.");

        float totalPrice = hargaPerUnit * quantity;
        double buyerCredit = buyer.getWaterCredit() != null ? buyer.getWaterCredit() : 0.0;

        if (buyerCredit < totalPrice)
            throw new RuntimeException(
                String.format("Water credit tidak mencukupi. Kredit Anda: %.2f, Total harga: %.2f",
                    buyerCredit, totalPrice));

        // Kurangi credit pembeli
        buyer.setWaterCredit(buyerCredit - totalPrice);
        individualDAO.update(buyer);

        // Kurangi stok produk
        product.setEntitas(available - quantity);
        productDAO.update(product);

        // Buat transaksi
        Transaction tx = new Transaction();
        tx.setBuyer(buyer);
        tx.setProduct(product);
        tx.setQuantity(quantity);
        tx.setTotalPrice(totalPrice);
        tx.setCreatedAt(LocalDateTime.now());

        return transactionDAO.insert(tx);
    }

    /**
     * Ambil riwayat transaksi milik user yang login.
     */
    public List<Transaction> getMyTransactions(User user) {
        Individual individual = individualDAO.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Profil individual tidak ditemukan."));
        return transactionDAO.findByBuyer(individual);
    }

    /**
     * Ambil semua transaksi (untuk admin/gov).
     */
    public List<Transaction> getAllTransactions() {
        return transactionDAO.findAll();
    }
}
