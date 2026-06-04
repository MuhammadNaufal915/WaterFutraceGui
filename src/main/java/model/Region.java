package model;

/**
 * Region - Model wilayah/daerah
 */
public class Region {

    private Integer idRegion;
    private String  name;

    public Region() {}

    public Region(Integer idRegion, String name) {
        this.idRegion = idRegion;
        this.name     = name;
    }

    public Region(String name) {
        this.name = name;
    }

    // Getter & Setter
    public Integer getIdRegion() { return idRegion; }
    public void setIdRegion(Integer idRegion) { this.idRegion = idRegion; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() { return name != null ? name : ""; }
}
