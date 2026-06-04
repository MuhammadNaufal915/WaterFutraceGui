package model;

/**
 * Individual - Entitas pengguna perorangan yang dapat membeli produk
 */
public class Individual {

    private int        idIndividual;
    private User       user;
    private int        age;
    private String     pekerjaan;
    private Double     waterCredit;
    private Government government;
    private Region     region;

    public Individual() {}

    public Individual(User user, int age, String pekerjaan) {
        this.user      = user;
        this.age       = age;
        this.pekerjaan = pekerjaan;
        this.waterCredit = 0.0;
    }

    // Getter & Setter
    public int getIdIndividual() { return idIndividual; }
    public void setIdIndividual(int idIndividual) { this.idIndividual = idIndividual; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getPekerjaan() { return pekerjaan; }
    public void setPekerjaan(String pekerjaan) { this.pekerjaan = pekerjaan; }

    public Double getWaterCredit() { return waterCredit; }
    public void setWaterCredit(Double waterCredit) { this.waterCredit = waterCredit; }

    public Government getGovernment() { return government; }
    public void setGovernment(Government government) { this.government = government; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    @Override
    public String toString() {
        return (user != null ? user.getName() : "?") + " [" + pekerjaan + "]";
    }
}
