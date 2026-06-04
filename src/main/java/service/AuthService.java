package service;

import dao.*;
import model.*;
import utils.PasswordUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * AuthService - Menangani login, registrasi, dan approval user.
 * Menggantikan authController + Spring Security.
 */
public class AuthService {

    private final UserDAO       userDAO       = new UserDAO();
    private final CompanyDAO    companyDAO    = new CompanyDAO();
    private final GovernmentDAO governmentDAO = new GovernmentDAO();
    private final IndividualDAO individualDAO = new IndividualDAO();
    private final RegionDAO     regionDAO     = new RegionDAO();

    /**
     * Login: verifikasi email & password.
     * @return User jika berhasil
     * @throws RuntimeException jika gagal
     */
    public User login(String email, String password) {
        Optional<User> opt = userDAO.findByEmail(email.trim());
        if (opt.isEmpty()) {
            throw new RuntimeException("Email atau password salah.");
        }
        User user = opt.get();
        if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
            throw new RuntimeException("Email atau password salah.");
        }
        if (!user.isApproved() && user.getRole() != ERole.GOVERMENT) {
            throw new RuntimeException("Akun Anda belum disetujui oleh Government.\nSilakan tunggu persetujuan.");
        }
        return user;
    }

    /**
     * Registrasi user baru.
     * @param name nama
     * @param email email
     * @param password password plain
     * @param alamat alamat
     * @param roleName "INDIVIDUAL", "COMPANY", "GOVERMENT"
     * @param regionName nama region
     * @param extraParam1 sector (untuk COMPANY) / waterbasin (untuk GOVERMENT) / pekerjaan (untuk INDIVIDUAL)
     * @param usia usia (untuk INDIVIDUAL, 0 jika tidak ada)
     */
    public void register(String name, String email, String password,
                         String alamat, String roleName, String regionName,
                         String extraParam1, int usia) {

        // Cek email duplikat
        if (userDAO.findByEmail(email.trim()).isPresent()) {
            throw new RuntimeException("Email sudah digunakan. Gunakan email lain.");
        }

        // Cek region
        Optional<Region> regionOpt = regionDAO.findByName(regionName);
        if (regionOpt.isEmpty()) {
            throw new RuntimeException("Region tidak ditemukan: " + regionName);
        }
        Region region = regionOpt.get();

        // Parse role
        ERole role;
        try {
            role = ERole.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Role tidak valid: " + roleName);
        }

        // Buat user
        User user = new User();
        user.setName(name.trim());
        user.setEmail(email.trim());
        user.setPassword(PasswordUtil.hashPassword(password));
        user.setAlamat(alamat.trim());
        user.setRole(role);
        // Government langsung approved, lainnya perlu approval
        user.setApproved(role == ERole.GOVERMENT);
        userDAO.insert(user);

        // Buat entitas spesifik role
        if (role == ERole.COMPANY) {
            Optional<Government> govOpt = governmentDAO.findFirstByRegion(region);
            Company comp = new Company(user, extraParam1 != null ? extraParam1 : "General");
            comp.setRegion(region);
            comp.setWatercredit(0.0);
            govOpt.ifPresent(comp::setGovernment);
            companyDAO.insert(comp);

        } else if (role == ERole.GOVERMENT) {
            Government gov = new Government();
            gov.setIdGovernment(UUID.randomUUID().toString());
            gov.setUser(user);
            gov.setRegion(region);
            gov.setWaterbasin(extraParam1 != null ? extraParam1 : "");
            governmentDAO.insert(gov);

        } else if (role == ERole.INDIVIDUAL) {
            Optional<Government> govOpt = governmentDAO.findFirstByRegion(region);
            Individual ind = new Individual(user, usia, extraParam1 != null ? extraParam1 : "");
            ind.setRegion(region);
            ind.setWaterCredit(0.0);
            govOpt.ifPresent(ind::setGovernment);
            individualDAO.insert(ind);
        }
    }

    /**
     * Approve user (oleh Government).
     */
    public void approveUser(long userId) {
        userDAO.approve(userId);
    }

    /**
     * Ambil semua user yang belum diapprove di region tertentu.
     */
    public java.util.List<User> getPendingUsersInRegion(Region region) {
        java.util.List<User> pending = new java.util.ArrayList<>();

        companyDAO.findByRegion(region).stream()
            .filter(c -> c.getUser() != null && !c.getUser().isApproved())
            .forEach(c -> pending.add(c.getUser()));

        individualDAO.findByRegion(region).stream()
            .filter(i -> i.getUser() != null && !i.getUser().isApproved())
            .forEach(i -> pending.add(i.getUser()));

        return pending;
    }
}
