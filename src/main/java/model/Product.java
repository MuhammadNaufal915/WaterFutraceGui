package model;

/**
 * Product - Produk air yang dijual perusahaan menggunakan water credit
 */
public class Product {

    private int     idProduct;
    private Company company;
    private String  productName;
    private float   waterCredit;
    private Integer entitas;    // Stok / jumlah unit
    private Float   hargaJual; // Harga per unit = waterCredit / entitas

    public Product() {
        this.entitas = 1;
    }

    public Product(Company company, String productName, float waterCredit, int entitas) {
        this.company     = company;
        this.productName = productName;
        this.waterCredit = waterCredit;
        this.entitas     = entitas;
        recalculateHargaJual();
    }

    public void recalculateHargaJual() {
        if (entitas != null && entitas > 0) {
            this.hargaJual = this.waterCredit / (float) this.entitas;
        } else {
            this.hargaJual = 0f;
        }
    }

    // Getter & Setter
    public int getIdProduct() { return idProduct; }
    public void setIdProduct(int idProduct) { this.idProduct = idProduct; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public float getWaterCredit() { return waterCredit; }
    public void setWaterCredit(float waterCredit) { this.waterCredit = waterCredit; }

    public Integer getEntitas() { return entitas; }
    public void setEntitas(Integer entitas) { this.entitas = entitas; }

    public Float getHargaJual() {
        if (hargaJual != null) return hargaJual;
        if (entitas == null || entitas == 0) return 0f;
        return waterCredit / (float) entitas;
    }
    public void setHargaJual(Float hargaJual) { this.hargaJual = hargaJual; }

    @Override
    public String toString() {
        return productName + " (stok: " + entitas + ")";
    }
}
