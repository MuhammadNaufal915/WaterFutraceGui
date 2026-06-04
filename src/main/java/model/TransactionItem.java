package model;

/**
 * TransactionItem - Detail item dalam satu transaksi
 */
public class TransactionItem {

    private int         idTransactionItem;
    private Transaction transaction;
    private Product     product;
    private int         quantity;

    public TransactionItem() {}

    public TransactionItem(Transaction transaction, Product product, int quantity) {
        this.transaction = transaction;
        this.product     = product;
        this.quantity    = quantity;
    }

    // Getter & Setter
    public int getIdTransactionItem() { return idTransactionItem; }
    public void setIdTransactionItem(int idTransactionItem) { this.idTransactionItem = idTransactionItem; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
