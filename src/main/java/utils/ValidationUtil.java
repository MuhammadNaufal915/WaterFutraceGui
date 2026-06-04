package utils;

import java.util.regex.Pattern;

/**
 * ValidationUtil - Utilitas validasi input form
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Cek apakah string kosong atau hanya whitespace */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Cek apakah email valid */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /** Cek panjang minimum string */
    public static boolean hasMinLength(String value, int min) {
        if (isEmpty(value)) return false;
        return value.trim().length() >= min;
    }

    /** Cek apakah string adalah angka positif */
    public static boolean isPositiveDouble(String value) {
        if (isEmpty(value)) return false;
        try {
            double d = Double.parseDouble(value.trim());
            return d > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Cek apakah string adalah bilangan bulat positif */
    public static boolean isPositiveInt(String value) {
        if (isEmpty(value)) return false;
        try {
            int i = Integer.parseInt(value.trim());
            return i > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Cek range nilai antara 0 dan 1 (inklusif) */
    public static boolean isRatio(String value) {
        if (isEmpty(value)) return false;
        try {
            double d = Double.parseDouble(value.trim());
            return d >= 0 && d <= 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Return pesan error untuk field wajib */
    public static String requiredField(String fieldName) {
        return fieldName + " wajib diisi.";
    }
}
