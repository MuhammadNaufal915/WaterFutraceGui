package model;

/**
 * WaterFootprint - Hasil perhitungan Blue Water Footprint
 */
public class WaterFootprint {

    private int    idWaterfootprint;
    private float  totalUsage;
    private String category;
    private float  calculateFootprint;

    public WaterFootprint() {}

    public WaterFootprint(float totalUsage, String category, float calculateFootprint) {
        this.totalUsage         = totalUsage;
        this.category           = category;
        this.calculateFootprint = calculateFootprint;
    }

    // Getter & Setter
    public int getIdWaterfootprint() { return idWaterfootprint; }
    public void setIdWaterfootprint(int idWaterfootprint) { this.idWaterfootprint = idWaterfootprint; }

    public float getTotalUsage() { return totalUsage; }
    public void setTotalUsage(float totalUsage) { this.totalUsage = totalUsage; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public float getCalculateFootprint() { return calculateFootprint; }
    public void setCalculateFootprint(float calculateFootprint) { this.calculateFootprint = calculateFootprint; }
}
