package model;

/**
 * User - Model pengguna sistem
 * POJO murni tanpa annotation JPA/Spring
 */
public class User {

    private Long    idUser;
    private String  email;
    private String  password;
    private String  name;
    private String  alamat;
    private ERole   role;
    private boolean isApproved;

    public User() {}

    public User(String email, String password, String name, String alamat, ERole role) {
        this.email      = email;
        this.password   = password;
        this.name       = name;
        this.alamat     = alamat;
        this.role       = role;
        this.isApproved = false;
    }

    // Getter & Setter
    public Long getIdUser() { return idUser; }
    public void setIdUser(Long idUser) { this.idUser = idUser; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAlamat() { return alamat; }
    public void setAlamat(String alamat) { this.alamat = alamat; }

    public ERole getRole() { return role; }
    public void setRole(ERole role) { this.role = role; }

    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { this.isApproved = approved; }

    @Override
    public String toString() {
        return name + " (" + (role != null ? role.name() : "?") + ")";
    }
}
