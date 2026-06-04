package model;

/**
 * Company - Entitas perusahaan pengguna water credit
 */
public class Company {

    private int        idCompany;
    private User       user;
    private String     sector;
    private Double     watercredit;
    private Float      eta;   // Efisiensi penggunaan air (0-1)
    private Float      sl;    // Kelangkaan air (0-1)
    private Government government;
    private Region     region;

    public Company() {}

    public Company(User user, String sector) {
        this.user       = user;
        this.sector     = sector;
        this.watercredit = 0.0;
    }

    // Getter & Setter
    public int getIdCompany() { return idCompany; }
    public void setIdCompany(int idCompany) { this.idCompany = idCompany; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public Double getWatercredit() { return watercredit; }
    public void setWatercredit(Double watercredit) { this.watercredit = watercredit; }

    public Float getEta() { return eta; }
    public void setEta(Float eta) { this.eta = eta; }

    public Float getSl() { return sl; }
    public void setSl(Float sl) { this.sl = sl; }

    public Government getGovernment() { return government; }
    public void setGovernment(Government government) { this.government = government; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    @Override
    public String toString() {
        return (user != null ? user.getName() : "?") + " [" + sector + "]";
    }
}
