package model;

/**
 * Government - Entitas pemerintah daerah pengelola water basin
 */
public class Government {

    private String        idGovernment;
    private User          user;
    private String        waterbasin;
    private Region        region;
    private WaterFootprint waterFootprint;

    public Government() {}

    public Government(String idGovernment, User user, String waterbasin, Region region) {
        this.idGovernment = idGovernment;
        this.user         = user;
        this.waterbasin   = waterbasin;
        this.region       = region;
    }

    // Getter & Setter
    public String getIdGovernment() { return idGovernment; }
    public void setIdGovernment(String idGovernment) { this.idGovernment = idGovernment; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getWaterbasin() { return waterbasin; }
    public void setWaterbasin(String waterbasin) { this.waterbasin = waterbasin; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public WaterFootprint getWaterFootprint() { return waterFootprint; }
    public void setWaterFootprint(WaterFootprint waterFootprint) { this.waterFootprint = waterFootprint; }

    @Override
    public String toString() {
        return "Gov:" + (user != null ? user.getName() : "?")
               + " [" + (region != null ? region.getName() : "?") + "]";
    }
}
