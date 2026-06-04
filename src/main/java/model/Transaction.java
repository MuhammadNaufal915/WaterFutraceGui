package model;

import java.time.LocalDateTime;

/**
 * Transaction - Catatan transaksi pembelian produk oleh Individual
 */
public class Transaction {

    private Integer       idTransaction;
    private Individual    buyer;
    private Product       product;
    private Integer       quantity;
    private Float         totalPrice;
    private LocalDateTime createdAt;

    public Transaction() {
        this.createdAt = LocalDateTime.now();
    }

    // Getter & Setter
    public Integer getIdTransaction() { return idTransaction; }
    public void setIdTransaction(Integer idTransaction) { this.idTransaction = idTransaction; }

    public Individual getBuyer() { return buyer; }
    public void setBuyer(Individual buyer) { this.buyer = buyer; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Float getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Float totalPrice) { this.totalPrice = totalPrice; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
