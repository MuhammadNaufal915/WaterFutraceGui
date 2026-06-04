package utils;

import model.User;

/**
 * SessionManager - Singleton untuk menyimpan user yang sedang login.
 * Menggantikan SecurityContext pada Spring Security.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    /** Mendapatkan instance singleton */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /** Set user yang berhasil login */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /** Mendapatkan user yang sedang login */
    public User getCurrentUser() {
        return currentUser;
    }

    /** Cek apakah ada user yang login */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Logout: hapus session */
    public void logout() {
        this.currentUser = null;
    }
}
