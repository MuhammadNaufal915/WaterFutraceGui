import dao.*;
import database.DatabaseHelper;
import model.*;
import org.testng.Assert;
import org.testng.annotations.*;
import service.*;
import utils.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TesApp - TestNG Integration Test Suite untuk aplikasi WaterFutrace
 *
 * Test ini melakukan pengujian end-to-end dengan database nyata (MySQL waterfutrace).
 * Setiap test group berjalan secara berurutan sesuai dependensi:
 *   1. Koneksi database
 *   2. PasswordUtil (unit)
 *   3. RegionDAO
 *   4. AuthService (register & login)
 *   5. AuthService (approve)
 *   6. FootprintService (hitung BWF & distribusi)
 *   7. ProductService (CRUD produk)
 *   8. TransactionService (pembelian produk)
 *   9. WaterCreditRequestService (transaksi antar individual)
 *  10. ComplainDAO (pengaduan)
 *  11. Cleanup (hapus data uji)
 */
@Test(suiteName = "WaterFutraceTestSuite")
public class TesApp {

    // ===================================================
    //  Suffix unik agar tidak bentrok dengan data lain
    // ===================================================
    private static final String SUFFIX = UUID.randomUUID().toString().substring(0, 8);

    // Email dummy untuk setiap role
    private static final String GOV_EMAIL  = "gov_test_"  + SUFFIX + "@test.com";
    private static final String COMP_EMAIL = "comp_test_" + SUFFIX + "@test.com";
    private static final String IND1_EMAIL = "ind1_test_" + SUFFIX + "@test.com";
    private static final String IND2_EMAIL = "ind2_test_" + SUFFIX + "@test.com";
    private static final String PASSWORD   = "TestPass123!";

    // Nama region yang SUDAH ADA di DB (sesuai data seed aplikasi)
    // Ganti jika nama region di database Anda berbeda
    private static final String REGION_NAME = "Jawa Barat";

    // Service instances
    private final AuthService              authService    = new AuthService();
    private final FootprintService         fpService      = new FootprintService();
    private final ProductService           productService = new ProductService();
    private final TransactionService       txService      = new TransactionService();
    private final WaterCreditRequestService wcService     = new WaterCreditRequestService();

    // DAO instances (untuk cleanup & direct verification)
    private final UserDAO       userDAO       = new UserDAO();
    private final CompanyDAO    companyDAO    = new CompanyDAO();
    private final IndividualDAO individualDAO = new IndividualDAO();
    private final GovernmentDAO governmentDAO = new GovernmentDAO();
    private final ComplainDAO   complainDAO   = new ComplainDAO();
    private final ProductDAO    productDAO    = new ProductDAO();
    private final TransactionDAO txDAO        = new TransactionDAO();
    private final WaterCreditRequestDAO wcDAO = new WaterCreditRequestDAO();
    private final RegionDAO     regionDAO     = new RegionDAO();

    // State yang dibagikan antar test method
    private User govUser;
    private User compUser;
    private User ind1User;
    private User ind2User;
    private Product testProduct;
    private int    testProductId;
    private String complainId;
    private int    wcRequestId;

    // ===================================================
    //  GROUP 1 : Koneksi Database
    // ===================================================

    /**
     * Test koneksi ke database MySQL waterfutrace.
     * Jika test ini gagal, semua test lain tidak dapat berjalan.
     */
    @Test(groups = "db",
          description = "Verifikasi koneksi ke database MySQL berhasil")
    public void testDatabaseConnection() {
        boolean connected = DatabaseHelper.testConnection();
        Assert.assertTrue(connected,
            "Koneksi database harus berhasil. Pastikan MySQL berjalan dan konfigurasi di DatabaseHelper sudah benar.");
        System.out.println("[PASS] Koneksi database berhasil.");
    }

    // ===================================================
    //  GROUP 2 : PasswordUtil (Unit Test)
    // ===================================================

    /**
     * Test hashing dan verifikasi password.
     */
    @Test(groups = "util",
          description = "Hashing password menghasilkan hash yang dapat diverifikasi")
    public void testPasswordHashAndVerify() {
        String plain  = "SuperSecret999";
        String hashed = PasswordUtil.hashPassword(plain);

        Assert.assertNotNull(hashed, "Hash tidak boleh null");
        Assert.assertTrue(hashed.contains(":"), "Hash harus berformat salt:hash");
        Assert.assertTrue(PasswordUtil.verifyPassword(plain, hashed),
            "Verifikasi password yang benar harus mengembalikan true");
        Assert.assertFalse(PasswordUtil.verifyPassword("salahPassword", hashed),
            "Verifikasi password yang salah harus mengembalikan false");

        System.out.println("[PASS] PasswordUtil: hash = " + hashed.substring(0, 20) + "...");
    }

    /**
     * Test bahwa dua hash dari password yang sama BERBEDA (karena salt acak).
     */
    @Test(groups = "util",
          description = "Setiap hashing menghasilkan hash yang berbeda (salt acak)")
    public void testPasswordHashIsUnique() {
        String h1 = PasswordUtil.hashPassword("same_password");
        String h2 = PasswordUtil.hashPassword("same_password");
        Assert.assertNotEquals(h1, h2,
            "Dua hash dari password yang sama harus berbeda karena salt acak");
        System.out.println("[PASS] Hash bersifat unik karena random salt.");
    }

    // ===================================================
    //  GROUP 3 : RegionDAO
    // ===================================================

    /**
     * Test bahwa region "Jawa Barat" ada di database.
     */
    @Test(groups = "region",
          dependsOnGroups = "db",
          description = "Region '" + REGION_NAME + "' ditemukan di database")
    public void testRegionExists() {
        Optional<Region> regionOpt = regionDAO.findByName(REGION_NAME);
        Assert.assertTrue(regionOpt.isPresent(),
            "Region '" + REGION_NAME + "' harus ada di database. Pastikan data seed sudah dijalankan.");
        System.out.println("[PASS] Region ditemukan: " + regionOpt.get());
    }

    /**
     * Test findAll region mengembalikan list tidak kosong.
     */
    @Test(groups = "region",
          dependsOnGroups = "db",
          description = "findAll region mengembalikan data")
    public void testRegionFindAll() {
        List<Region> regions = regionDAO.findAll();
        Assert.assertNotNull(regions, "List region tidak boleh null");
        Assert.assertFalse(regions.isEmpty(), "Harus ada minimal satu region di database");
        System.out.println("[PASS] Total region di DB: " + regions.size());
    }

    // ===================================================
    //  GROUP 4 : AuthService - Register
    // ===================================================

    /**
     * Registrasi akun Government (langsung approved).
     */
    @Test(groups = "register",
          dependsOnGroups = "region",
          description = "Registrasi akun GOVERNMENT berhasil dan langsung approved")
    public void testRegisterGovernment() {
        authService.register(
            "Gov Test " + SUFFIX, GOV_EMAIL, PASSWORD,
            "Jl. Pemerintahan No.1", "GOVERMENT", REGION_NAME,
            "Sungai Citarum Test", 0
        );

        Optional<User> userOpt = userDAO.findByEmail(GOV_EMAIL);
        Assert.assertTrue(userOpt.isPresent(), "User Government harus tersimpan di database");

        govUser = userOpt.get();
        Assert.assertEquals(govUser.getRole(), ERole.GOVERMENT, "Role harus GOVERMENT");
        Assert.assertTrue(govUser.isApproved(), "Government harus langsung approved");

        System.out.println("[PASS] Government terdaftar: " + govUser.getName() + " (id=" + govUser.getIdUser() + ")");
    }

    /**
     * Registrasi akun Company (belum approved sampai disetujui).
     */
    @Test(groups = "register",
          dependsOnGroups = "region",
          description = "Registrasi akun COMPANY berhasil (belum approved)")
    public void testRegisterCompany() {
        authService.register(
            "Company Test " + SUFFIX, COMP_EMAIL, PASSWORD,
            "Jl. Industri No.10", "COMPANY", REGION_NAME,
            "Manufaktur", 0
        );

        Optional<User> userOpt = userDAO.findByEmail(COMP_EMAIL);
        Assert.assertTrue(userOpt.isPresent(), "User Company harus tersimpan di database");

        compUser = userOpt.get();
        Assert.assertEquals(compUser.getRole(), ERole.COMPANY, "Role harus COMPANY");
        Assert.assertFalse(compUser.isApproved(), "Company belum diapprove");

        // Verifikasi Company profile ada di tabel company
        Optional<Company> compOpt = companyDAO.findByUser(compUser);
        Assert.assertTrue(compOpt.isPresent(), "Profil Company harus ada di tabel company");

        System.out.println("[PASS] Company terdaftar: " + compUser.getName() + " (id=" + compUser.getIdUser() + ")");
    }

    /**
     * Registrasi akun Individual pertama.
     */
    @Test(groups = "register",
          dependsOnGroups = "region",
          description = "Registrasi akun INDIVIDUAL pertama berhasil")
    public void testRegisterIndividual1() {
        authService.register(
            "Individual1 Test " + SUFFIX, IND1_EMAIL, PASSWORD,
            "Jl. Warga No.5", "INDIVIDUAL", REGION_NAME,
            "Petani", 30
        );

        Optional<User> userOpt = userDAO.findByEmail(IND1_EMAIL);
        Assert.assertTrue(userOpt.isPresent(), "User Individual 1 harus tersimpan");

        ind1User = userOpt.get();
        Assert.assertEquals(ind1User.getRole(), ERole.INDIVIDUAL);
        Assert.assertFalse(ind1User.isApproved(), "Individual 1 belum diapprove");

        System.out.println("[PASS] Individual1 terdaftar: " + ind1User.getName());
    }

    /**
     * Registrasi akun Individual kedua.
     */
    @Test(groups = "register",
          dependsOnGroups = "region",
          description = "Registrasi akun INDIVIDUAL kedua berhasil")
    public void testRegisterIndividual2() {
        authService.register(
            "Individual2 Test " + SUFFIX, IND2_EMAIL, PASSWORD,
            "Jl. Warga No.8", "INDIVIDUAL", REGION_NAME,
            "Nelayan", 25
        );

        Optional<User> userOpt = userDAO.findByEmail(IND2_EMAIL);
        Assert.assertTrue(userOpt.isPresent(), "User Individual 2 harus tersimpan");

        ind2User = userOpt.get();
        Assert.assertEquals(ind2User.getRole(), ERole.INDIVIDUAL);

        System.out.println("[PASS] Individual2 terdaftar: " + ind2User.getName());
    }

    /**
     * Registrasi dengan email yang sudah ada harus gagal.
     */
    @Test(groups = "register",
          dependsOnMethods = "testRegisterGovernment",
          description = "Registrasi dengan email duplikat harus melempar exception")
    public void testRegisterDuplicateEmailFails() {
        try {
            authService.register(
                "Duplikat User", GOV_EMAIL, PASSWORD,
                "Jl. Duplikat", "INDIVIDUAL", REGION_NAME, "Buruh", 20
            );
            Assert.fail("Seharusnya melempar RuntimeException karena email duplikat");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Email sudah digunakan"),
                "Pesan error harus menyebutkan email duplikat, tapi mendapat: " + e.getMessage());
            System.out.println("[PASS] Duplikat email ditolak: " + e.getMessage());
        }
    }

    /**
     * Registrasi dengan region tidak valid harus gagal.
     */
    @Test(groups = "register",
          dependsOnGroups = "region",
          description = "Registrasi dengan region tidak valid harus melempar exception")
    public void testRegisterInvalidRegionFails() {
        try {
            authService.register(
                "User Invalid Region", "invalidregion_" + SUFFIX + "@test.com", PASSWORD,
                "Jl. Mana", "INDIVIDUAL", "REGION_TIDAK_ADA_XYZ", "Buruh", 20
            );
            Assert.fail("Seharusnya melempar RuntimeException karena region tidak ada");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Region tidak ditemukan"),
                "Pesan error harus menyebutkan region tidak ditemukan");
            System.out.println("[PASS] Region tidak valid ditolak: " + e.getMessage());
        }
    }

    // ===================================================
    //  GROUP 5 : AuthService - Login
    // ===================================================

    /**
     * Login dengan kredensial benar untuk Government.
     */
    @Test(groups = "login",
          dependsOnMethods = "testRegisterGovernment",
          description = "Login Government dengan kredensial benar berhasil")
    public void testLoginGovernmentSuccess() {
        User logged = authService.login(GOV_EMAIL, PASSWORD);
        Assert.assertNotNull(logged);
        Assert.assertEquals(logged.getEmail(), GOV_EMAIL);
        Assert.assertEquals(logged.getRole(), ERole.GOVERMENT);
        govUser = logged; // Update reference dengan data fresh dari DB
        System.out.println("[PASS] Login Government berhasil: " + logged.getName());
    }

    /**
     * Login Company yang belum diapprove harus gagal.
     */
    @Test(groups = "login",
          dependsOnMethods = "testRegisterCompany",
          description = "Login Company yang belum diapprove harus gagal")
    public void testLoginUnapprovedCompanyFails() {
        try {
            authService.login(COMP_EMAIL, PASSWORD);
            Assert.fail("Login Company yang belum diapprove seharusnya gagal");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("belum disetujui"),
                "Pesan error harus menyebutkan belum disetujui");
            System.out.println("[PASS] Login Company belum approve ditolak: " + e.getMessage());
        }
    }

    /**
     * Login dengan password salah harus gagal.
     */
    @Test(groups = "login",
          dependsOnMethods = "testRegisterGovernment",
          description = "Login dengan password salah harus gagal")
    public void testLoginWrongPasswordFails() {
        try {
            authService.login(GOV_EMAIL, "passwordSalah999!");
            Assert.fail("Login dengan password salah seharusnya gagal");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Email atau password salah"),
                "Pesan error harus menyebutkan email atau password salah");
            System.out.println("[PASS] Password salah ditolak: " + e.getMessage());
        }
    }

    /**
     * Login dengan email yang tidak terdaftar harus gagal.
     */
    @Test(groups = "login",
          dependsOnGroups = "db",
          description = "Login dengan email tidak terdaftar harus gagal")
    public void testLoginUnknownEmailFails() {
        try {
            authService.login("emailtidakada_xyz999@test.com", PASSWORD);
            Assert.fail("Login dengan email tidak terdaftar seharusnya gagal");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Email atau password salah"));
            System.out.println("[PASS] Email tidak terdaftar ditolak.");
        }
    }

    // ===================================================
    //  GROUP 6 : AuthService - Approve User
    // ===================================================

    /**
     * Government menyetujui Company.
     */
    @Test(groups = "approve",
          dependsOnMethods = {"testLoginGovernmentSuccess", "testRegisterCompany"},
          description = "Government berhasil approve Company")
    public void testApproveCompany() {
        // Pastikan compUser ter-inisialisasi (mungkin dijalankan setelah register)
        if (compUser == null) {
            compUser = userDAO.findByEmail(COMP_EMAIL).orElseThrow();
        }
        authService.approveUser(compUser.getIdUser());

        User updated = userDAO.findById(compUser.getIdUser()).orElseThrow();
        Assert.assertTrue(updated.isApproved(), "Company harus sudah diapprove");
        compUser = updated;
        System.out.println("[PASS] Company diapprove: " + compUser.getName());
    }

    /**
     * Government menyetujui Individual 1.
     */
    @Test(groups = "approve",
          dependsOnMethods = {"testLoginGovernmentSuccess", "testRegisterIndividual1"},
          description = "Government berhasil approve Individual 1")
    public void testApproveIndividual1() {
        if (ind1User == null) ind1User = userDAO.findByEmail(IND1_EMAIL).orElseThrow();
        authService.approveUser(ind1User.getIdUser());

        User updated = userDAO.findById(ind1User.getIdUser()).orElseThrow();
        Assert.assertTrue(updated.isApproved(), "Individual 1 harus sudah diapprove");
        ind1User = updated;
        System.out.println("[PASS] Individual1 diapprove: " + ind1User.getName());
    }

    /**
     * Government menyetujui Individual 2.
     */
    @Test(groups = "approve",
          dependsOnMethods = {"testLoginGovernmentSuccess", "testRegisterIndividual2"},
          description = "Government berhasil approve Individual 2")
    public void testApproveIndividual2() {
        if (ind2User == null) ind2User = userDAO.findByEmail(IND2_EMAIL).orElseThrow();
        authService.approveUser(ind2User.getIdUser());

        User updated = userDAO.findById(ind2User.getIdUser()).orElseThrow();
        Assert.assertTrue(updated.isApproved(), "Individual 2 harus sudah diapprove");
        ind2User = updated;
        System.out.println("[PASS] Individual2 diapprove: " + ind2User.getName());
    }

    /**
     * Verifikasi getPendingUsersInRegion mengembalikan daftar sebelum approval.
     */
    @Test(groups = "approve",
          dependsOnMethods = {"testRegisterCompany", "testRegisterIndividual1",
                              "testRegisterIndividual2", "testLoginGovernmentSuccess"},
          description = "getPendingUsersInRegion mengembalikan user yang belum diapprove")
    public void testGetPendingUsers() {
        if (govUser == null) govUser = userDAO.findByEmail(GOV_EMAIL).orElseThrow();
        Optional<Region> regionOpt = regionDAO.findByName(REGION_NAME);
        Assert.assertTrue(regionOpt.isPresent());

        List<User> pending = authService.getPendingUsersInRegion(regionOpt.get());
        Assert.assertNotNull(pending, "List pending tidak boleh null");
        // Tidak assert jumlah pasti karena bisa ada data lain di DB
        System.out.println("[PASS] Jumlah user pending di region: " + pending.size());
    }

    /**
     * Login Company berhasil setelah diapprove.
     */
    @Test(groups = "approve",
          dependsOnMethods = "testApproveCompany",
          description = "Login Company berhasil setelah diapprove")
    public void testLoginApprovedCompanySuccess() {
        User logged = authService.login(COMP_EMAIL, PASSWORD);
        Assert.assertNotNull(logged);
        Assert.assertTrue(logged.isApproved(), "Company seharusnya sudah approved");
        compUser = logged;
        System.out.println("[PASS] Login Company setelah approve berhasil: " + logged.getName());
    }

    // ===================================================
    //  GROUP 7 : FootprintService
    // ===================================================

    /**
     * Government menghitung Blue Water Footprint.
     */
    @Test(groups = "footprint",
          dependsOnGroups = "approve",
          description = "Government berhasil menghitung Blue Water Footprint")
    public void testCalculateGovernmentFootprint() {
        if (govUser == null) govUser = userDAO.findByEmail(GOV_EMAIL).orElseThrow();

        double bwi  = 1000.0; // Blue Water Incorporation
        double lrf  = 0.5;    // Lost Return Flow
        double vol  = 200.0;  // Volume
        double time = 10.0;   // Time

        double expectedBwf = bwi + lrf * (vol / time); // 1000 + 0.5 * 20 = 1010

        double result = fpService.calculateGovernmentFootprint(bwi, lrf, vol, time, govUser);
        Assert.assertEquals(result, expectedBwf, 0.001,
            "Hasil BWF harus sesuai formula: BWI + LRF * (Volume / Time)");

        System.out.println("[PASS] BWF dihitung: " + result);
    }

    /**
     * Setelah BWF dihitung, set eta & sl pada Company agar distribusi bisa berjalan.
     */
    @Test(groups = "footprint",
          dependsOnMethods = "testCalculateGovernmentFootprint",
          description = "Set eta dan sl pada Company untuk keperluan distribusi")
    public void testSetCompanyEtaSl() {
        if (compUser == null) compUser = userDAO.findByEmail(COMP_EMAIL).orElseThrow();
        Company company = companyDAO.findByUser(compUser).orElseThrow();
        company.setEta(0.8f);
        company.setSl(0.2f);
        companyDAO.update(company);

        Company updated = companyDAO.findByUser(compUser).orElseThrow();
        Assert.assertEquals(updated.getEta(), 0.8f, 0.001f, "Eta harus tersimpan 0.8");
        Assert.assertEquals(updated.getSl(),  0.2f, 0.001f, "Sl harus tersimpan 0.2");

        System.out.println("[PASS] Eta=" + updated.getEta() + " Sl=" + updated.getSl() + " tersimpan.");
    }

    /**
     * Government mendistribusikan water credit ke Company dan Individual.
     */
    @Test(groups = "footprint",
          dependsOnMethods = {"testSetCompanyEtaSl", "testApproveIndividual1", "testApproveIndividual2"},
          description = "Distribusi water credit ke Company dan Individual berhasil")
    public void testDistributeCredit() {
        if (govUser == null) govUser = userDAO.findByEmail(GOV_EMAIL).orElseThrow();

        var result = fpService.distributeCredit(1000.0, 0.5, 200.0, 10.0, govUser);

        Assert.assertNotNull(result, "Hasil distribusi tidak boleh null");
        Assert.assertTrue(result.containsKey("bwf"), "Hasil harus mengandung key 'bwf'");
        Assert.assertTrue(result.containsKey("averageCompanyCredit"));
        Assert.assertTrue(result.containsKey("companies"));
        Assert.assertTrue(result.containsKey("individuals"));

        double bwf = (double) result.get("bwf");
        Assert.assertEquals(bwf, 1010.0, 0.001, "BWF distribusi harus 1010.0");

        @SuppressWarnings("unchecked")
        List<?> companies = (List<?>) result.get("companies");
        Assert.assertFalse(companies.isEmpty(), "Harus ada minimal satu company dalam distribusi");

        System.out.println("[PASS] Distribusi berhasil. BWF=" + bwf
            + " | Companies=" + companies.size()
            + " | Region=" + result.get("regionName"));
    }

    /**
     * Company menghitung water credit sendiri setelah BWF tersedia.
     */
    @Test(groups = "footprint",
          dependsOnMethods = "testDistributeCredit",
          description = "Company berhasil menghitung water credit sendiri")
    public void testCalculateCompanyCredit() {
        if (compUser == null) compUser = userDAO.findByEmail(COMP_EMAIL).orElseThrow();

        double credit = fpService.calculateCompanyCredit(0.8, 0.2, compUser);
        Assert.assertTrue(credit >= 0, "Water credit harus >= 0");

        System.out.println("[PASS] Company water credit: " + credit);
    }

    /**
     * Individual mendapatkan total water credit regional dari Company.
     */
    @Test(groups = "footprint",
          dependsOnMethods = "testDistributeCredit",
          description = "Individual mendapatkan total regional company credit")
    public void testGetRegionalCompanyCredit() {
        if (ind1User == null) ind1User = userDAO.findByEmail(IND1_EMAIL).orElseThrow();

        double totalCredit = fpService.getRegionalCompanyCredit(ind1User);
        Assert.assertTrue(totalCredit > 0, "Total company credit di region harus > 0 setelah distribusi");

        System.out.println("[PASS] Regional company credit untuk Individual1: " + totalCredit);
    }

    // ===================================================
    //  GROUP 8 : ProductService
    // ===================================================

    /**
     * Company membuat produk baru.
     */
    @Test(groups = "product",
          dependsOnMethods = "testDistributeCredit",
          description = "Company berhasil membuat produk baru")
    public void testCreateProduct() {
        if (compUser == null) compUser = userDAO.findByEmail(COMP_EMAIL).orElseThrow();

        // Pastikan company punya water credit cukup (dari distribusi)
        Company comp = companyDAO.findByUser(compUser).orElseThrow();
        double currentCredit = comp.getWatercredit() != null ? comp.getWatercredit() : 0.0;

        // Jika credit tidak cukup, beri secara langsung untuk keperluan test
        if (currentCredit < 10.0) {
            comp.setWatercredit(100.0);
            companyDAO.update(comp);
        }

        testProduct = productService.createProduct(compUser, "Produk Tes " + SUFFIX, 5.0f, 10);
        Assert.assertNotNull(testProduct, "Produk harus berhasil dibuat");
        Assert.assertTrue(testProduct.getIdProduct() > 0, "ID produk harus terisi");
        Assert.assertEquals(testProduct.getProductName(), "Produk Tes " + SUFFIX);
        Assert.assertEquals(testProduct.getEntitas().intValue(), 10);
        Assert.assertEquals(testProduct.getWaterCredit(), 5.0f, 0.001f);

        testProductId = testProduct.getIdProduct();
        System.out.println("[PASS] Produk dibuat: " + testProduct.getProductName() + " (id=" + testProductId + ")");
    }

    /**
     * Company gagal membuat produk karena water credit tidak cukup.
     */
    @Test(groups = "product",
          dependsOnMethods = "testCreateProduct",
          description = "Pembuatan produk gagal jika water credit tidak cukup")
    public void testCreateProductInsufficientCreditFails() {
        // Buat user company baru yang tidak punya credit
        String poorCompEmail = "poorcomp_" + SUFFIX + "@test.com";
        try {
            authService.register(
                "Poor Company " + SUFFIX, poorCompEmail, PASSWORD,
                "Jl. Miskin No.1", "COMPANY", REGION_NAME, "Pertanian", 0
            );
            User poorUser = userDAO.findByEmail(poorCompEmail).orElseThrow();
            // Approve dulu
            authService.approveUser(poorUser.getIdUser());
            poorUser = userDAO.findById(poorUser.getIdUser()).orElseThrow();

            // Pastikan water credit = 0
            Company poorComp = companyDAO.findByUser(poorUser).orElseThrow();
            poorComp.setWatercredit(0.0);
            companyDAO.update(poorComp);

            // Coba buat produk - harus gagal
            productService.createProduct(poorUser, "Produk Tidak Mampu", 999999.0f, 1);
            Assert.fail("Seharusnya gagal karena water credit tidak cukup");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Kredit air tidak mencukupi"),
                "Pesan error harus menyebutkan kredit tidak cukup, dapat: " + e.getMessage());
            System.out.println("[PASS] Pembuatan produk ditolak karena kredit tidak cukup: " + e.getMessage());
        } finally {
            // Bersihkan data test tambahan
            cleanupByEmail(poorCompEmail);
        }
    }

    /**
     * Mengambil semua produk (getAllProducts).
     */
    @Test(groups = "product",
          dependsOnMethods = "testCreateProduct",
          description = "getAllProducts mengembalikan daftar produk")
    public void testGetAllProducts() {
        List<Product> products = productService.getAllProducts();
        Assert.assertNotNull(products, "Daftar produk tidak boleh null");
        Assert.assertFalse(products.isEmpty(), "Harus ada minimal satu produk");

        boolean found = products.stream().anyMatch(p -> p.getIdProduct() == testProductId);
        Assert.assertTrue(found, "Produk yang baru dibuat harus muncul di daftar");
        System.out.println("[PASS] getAllProducts: " + products.size() + " produk ditemukan");
    }

    /**
     * Mengambil produk milik Company yang login (getMyProducts).
     */
    @Test(groups = "product",
          dependsOnMethods = "testCreateProduct",
          description = "getMyProducts mengembalikan produk milik Company")
    public void testGetMyProducts() {
        if (compUser == null) compUser = userDAO.findByEmail(COMP_EMAIL).orElseThrow();
        List<Product> myProducts = productService.getMyProducts(compUser);
        Assert.assertNotNull(myProducts);
        Assert.assertFalse(myProducts.isEmpty(), "Company harus punya produk yang dibuat tadi");

        boolean found = myProducts.stream().anyMatch(p -> p.getIdProduct() == testProductId);
        Assert.assertTrue(found, "Produk tes harus ada di daftar produk company");
        System.out.println("[PASS] getMyProducts: " + myProducts.size() + " produk milik company");
    }

    /**
     * Company mengupdate produk.
     */
    @Test(groups = "product",
          dependsOnMethods = "testCreateProduct",
          description = "Company berhasil mengupdate produk")
    public void testUpdateProduct() {
        if (compUser == null) compUser = userDAO.findByEmail(COMP_EMAIL).orElseThrow();
        Product updated = productService.updateProduct(
            compUser, testProductId, "Produk Updated " + SUFFIX, 4.0f, 8
        );

        Assert.assertEquals(updated.getProductName(), "Produk Updated " + SUFFIX);
        Assert.assertEquals(updated.getEntitas().intValue(), 8);
        Assert.assertEquals(updated.getWaterCredit(), 4.0f, 0.001f);

        System.out.println("[PASS] Produk diupdate: " + updated.getProductName());
    }

    // ===================================================
    //  GROUP 9 : TransactionService
    // ===================================================

    /**
     * Individual membeli produk dari Company.
     */
    @Test(groups = "transaction",
          dependsOnMethods = {"testUpdateProduct", "testApproveIndividual1"},
          description = "Individual berhasil membeli produk")
    public void testPurchaseProduct() {
        if (ind1User == null) ind1User = userDAO.findByEmail(IND1_EMAIL).orElseThrow();

        // Pastikan individual punya water credit cukup
        Individual ind = individualDAO.findByUser(ind1User).orElseThrow();
        double creditBefore = ind.getWaterCredit() != null ? ind.getWaterCredit() : 0.0;

        // Pastikan produk punya harga jual
        Product p = productDAO.findById(testProductId).orElseThrow();
        float harga = p.getHargaJual() != null ? p.getHargaJual() : 0;
        int qty = 2;
        float totalHarga = harga * qty;

        if (creditBefore < totalHarga) {
            ind.setWaterCredit((double) totalHarga + 10.0);
            individualDAO.update(ind);
            ind1User = userDAO.findById(ind1User.getIdUser()).orElseThrow(); // Refresh
        }

        double creditBeforePurchase = individualDAO.findByUser(ind1User).orElseThrow().getWaterCredit();
        int stokBefore = p.getEntitas();

        Transaction tx = txService.purchase(ind1User, testProductId, qty);
        Assert.assertNotNull(tx, "Transaksi harus berhasil dibuat");
        Assert.assertTrue(tx.getIdTransaction() > 0, "ID transaksi harus terisi");
        Assert.assertEquals(tx.getQuantity().intValue(), qty);

        // Verifikasi credit berkurang
        double creditAfter = individualDAO.findByUser(ind1User).orElseThrow().getWaterCredit();
        Assert.assertEquals(creditAfter, creditBeforePurchase - totalHarga, 0.01,
            "Water credit Individual harus berkurang sebesar total harga");

        // Verifikasi stok berkurang
        int stokAfter = productDAO.findById(testProductId).orElseThrow().getEntitas();
        Assert.assertEquals(stokAfter, stokBefore - qty, "Stok produk harus berkurang");

        System.out.println("[PASS] Pembelian berhasil. TX_ID=" + tx.getIdTransaction()
            + " | Credit: " + creditBeforePurchase + " -> " + creditAfter
            + " | Stok: " + stokBefore + " -> " + stokAfter);
    }

    /**
     * Pembelian dengan quantity 0 harus gagal.
     */
    @Test(groups = "transaction",
          dependsOnMethods = "testPurchaseProduct",
          description = "Pembelian dengan quantity <= 0 harus gagal")
    public void testPurchaseZeroQuantityFails() {
        if (ind1User == null) ind1User = userDAO.findByEmail(IND1_EMAIL).orElseThrow();
        try {
            txService.purchase(ind1User, testProductId, 0);
            Assert.fail("Pembelian dengan quantity 0 seharusnya gagal");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Jumlah pembelian harus lebih dari 0"),
                "Pesan error harus menyebutkan jumlah pembelian, dapat: " + e.getMessage());
            System.out.println("[PASS] Quantity 0 ditolak: " + e.getMessage());
        }
    }

    /**
     * Pembelian jika credit tidak cukup harus gagal.
     */
    @Test(groups = "transaction",
          dependsOnMethods = "testPurchaseProduct",
          description = "Pembelian gagal jika water credit tidak cukup")
    public void testPurchaseInsufficientCreditFails() {
        if (ind2User == null) ind2User = userDAO.findByEmail(IND2_EMAIL).orElseThrow();
        // Set credit ind2 ke 0 agar tidak bisa beli
        Individual ind2 = individualDAO.findByUser(ind2User).orElseThrow();
        ind2.setWaterCredit(0.0);
        individualDAO.update(ind2);

        try {
            txService.purchase(ind2User, testProductId, 1);
            Assert.fail("Seharusnya gagal karena credit tidak cukup");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Water credit tidak mencukupi"),
                "Pesan error harus menyebutkan water credit tidak mencukupi, dapat: " + e.getMessage());
            System.out.println("[PASS] Pembelian ditolak karena credit 0: " + e.getMessage());
        }
    }

    /**
     * Individual melihat riwayat transaksi miliknya.
     */
    @Test(groups = "transaction",
          dependsOnMethods = "testPurchaseProduct",
          description = "getMyTransactions mengembalikan riwayat transaksi Individual")
    public void testGetMyTransactions() {
        if (ind1User == null) ind1User = userDAO.findByEmail(IND1_EMAIL).orElseThrow();
        List<Transaction> txList = txService.getMyTransactions(ind1User);
        Assert.assertNotNull(txList, "Riwayat transaksi tidak boleh null");
        Assert.assertFalse(txList.isEmpty(), "Individual harus punya minimal satu transaksi");
        System.out.println("[PASS] getMyTransactions: " + txList.size() + " transaksi ditemukan");
    }

    /**
     * getAllTransactions mengembalikan semua transaksi.
     */
    @Test(groups = "transaction",
          dependsOnMethods = "testPurchaseProduct",
          description = "getAllTransactions mengembalikan semua transaksi")
    public void testGetAllTransactions() {
        List<Transaction> allTx = txService.getAllTransactions();
        Assert.assertNotNull(allTx, "Daftar transaksi tidak boleh null");
        Assert.assertFalse(allTx.isEmpty(), "Harus ada minimal satu transaksi");
        System.out.println("[PASS] getAllTransactions: " + allTx.size() + " total transaksi");
    }

    // ===================================================
    //  GROUP 10 : WaterCreditRequestService
    // ===================================================

    /**
     * Individual2 membuat permintaan beli water credit ONSITE ke Individual1.
     */
    @Test(groups = "wc_request",
          dependsOnMethods = {"testApproveIndividual1", "testApproveIndividual2"},
          description = "Individual membuat permintaan beli water credit ONSITE")
    public void testCreateOnsiteRequest() {
        if (ind1User == null) ind1User = userDAO.findByEmail(IND1_EMAIL).orElseThrow();
        if (ind2User == null) ind2User = userDAO.findByEmail(IND2_EMAIL).orElseThrow();

        // Pastikan ind2 approved dan freshly loaded
        ind2User = userDAO.findById(ind2User.getIdUser()).orElseThrow();

        Individual seller = individualDAO.findByUser(ind1User).orElseThrow();
        int sellerId = seller.getIdIndividual();

        WaterCreditRequest req = wcService.createOnsiteRequest(ind2User, sellerId, 0.001);
        Assert.assertNotNull(req, "Request harus berhasil dibuat");
        Assert.assertTrue(req.getIdRequest() > 0, "ID request harus terisi");
        Assert.assertEquals(req.getStatus(), WaterCreditRequest.RequestStatus.PENDING);
        Assert.assertEquals(req.getMode(),   WaterCreditRequest.RequestMode.ONSITE);

        wcRequestId = req.getIdRequest();
        System.out.println("[PASS] ONSITE request dibuat: id=" + wcRequestId);
    }

    /**
     * Individual membuat permintaan beli water credit RANDOM (broadcast).
     */
    @Test(groups = "wc_request",
          dependsOnMethods = "testApproveIndividual2",
          description = "Individual membuat permintaan beli water credit RANDOM/broadcast")
    public void testCreateRandomRequest() {
        if (ind2User == null) ind2User = userDAO.findByEmail(IND2_EMAIL).orElseThrow();
        ind2User = userDAO.findById(ind2User.getIdUser()).orElseThrow();

        WaterCreditRequest req = wcService.createRandomRequest(ind2User, 0.001);
        Assert.assertNotNull(req);
        Assert.assertEquals(req.getMode(), WaterCreditRequest.RequestMode.RANDOM);
        Assert.assertEquals(req.getStatus(), WaterCreditRequest.RequestStatus.PENDING);
        Assert.assertNull(req.getSeller(), "RANDOM request tidak punya seller spesifik");
        System.out.println("[PASS] RANDOM request dibuat: id=" + req.getIdRequest());
    }

    /**
     * Individual1 (seller) menyetujui permintaan ONSITE dari Individual2.
     */
    @Test(groups = "wc_request",
          dependsOnMethods = "testCreateOnsiteRequest",
          description = "Seller berhasil menyetujui permintaan ONSITE")
    public void testApproveOnsiteRequest() {
        if (ind1User == null) ind1User = userDAO.findByEmail(IND1_EMAIL).orElseThrow();
        ind1User = userDAO.findById(ind1User.getIdUser()).orElseThrow();

        // Pastikan ind1 punya credit untuk dijual
        Individual seller = individualDAO.findByUser(ind1User).orElseThrow();
        if (seller.getWaterCredit() == null || seller.getWaterCredit() < 0.01) {
            seller.setWaterCredit(10.0);
            individualDAO.update(seller);
        }

        WaterCreditRequest req = wcDAO.findById(wcRequestId).orElseThrow();
        double amount = req.getAmount();

        double sellerCreditBefore = individualDAO.findByUser(ind1User).orElseThrow().getWaterCredit();
        double buyerCreditBefore  = individualDAO.findByUser(ind2User).orElseThrow().getWaterCredit();

        wcService.approveRequest(ind1User, wcRequestId);

        double sellerCreditAfter = individualDAO.findByUser(ind1User).orElseThrow().getWaterCredit();
        double buyerCreditAfter  = individualDAO.findByUser(ind2User).orElseThrow().getWaterCredit();

        Assert.assertEquals(sellerCreditAfter, sellerCreditBefore - amount, 0.0001,
            "Credit seller harus berkurang sebesar amount");
        Assert.assertEquals(buyerCreditAfter, buyerCreditBefore + amount, 0.0001,
            "Credit buyer harus bertambah sebesar amount");

        WaterCreditRequest updated = wcDAO.findById(wcRequestId).orElseThrow();
        Assert.assertEquals(updated.getStatus(), WaterCreditRequest.RequestStatus.APPROVED);

        System.out.println("[PASS] ONSITE request diapprove. Seller: " + sellerCreditBefore
            + " -> " + sellerCreditAfter + " | Buyer: " + buyerCreditBefore + " -> " + buyerCreditAfter);
    }

    /**
     * Menyetujui request yang sudah diproses harus gagal.
     */
    @Test(groups = "wc_request",
          dependsOnMethods = "testApproveOnsiteRequest",
          description = "Menyetujui request yang sudah diproses harus gagal")
    public void testApproveAlreadyProcessedRequestFails() {
        if (ind1User == null) ind1User = userDAO.findByEmail(IND1_EMAIL).orElseThrow();
        try {
            wcService.approveRequest(ind1User, wcRequestId);
            Assert.fail("Seharusnya gagal karena request sudah diproses");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("sudah diproses"),
                "Pesan harus menyebutkan sudah diproses, dapat: " + e.getMessage());
            System.out.println("[PASS] Double-approve ditolak: " + e.getMessage());
        }
    }

    /**
     * getSentRequests mengembalikan daftar request yang dibuat oleh buyer.
     */
    @Test(groups = "wc_request",
          dependsOnMethods = "testCreateOnsiteRequest",
          description = "getSentRequests mengembalikan request yang dikirim buyer")
    public void testGetSentRequests() {
        if (ind2User == null) ind2User = userDAO.findByEmail(IND2_EMAIL).orElseThrow();
        List<WaterCreditRequest> sent = wcService.getSentRequests(ind2User);
        Assert.assertNotNull(sent);
        Assert.assertFalse(sent.isEmpty(), "Individual2 harus punya request yang dikirim");
        System.out.println("[PASS] getSentRequests: " + sent.size() + " request terkirim");
    }

    /**
     * getAllIndividualsSorted mengembalikan individual terurut berdasarkan water credit.
     */
    @Test(groups = "wc_request",
          dependsOnMethods = {"testApproveIndividual1", "testApproveIndividual2"},
          description = "getAllIndividualsSorted mengembalikan list individual terurut")
    public void testGetAllIndividualsSorted() {
        if (ind1User == null) ind1User = userDAO.findByEmail(IND1_EMAIL).orElseThrow();
        List<Individual> sorted = wcService.getAllIndividualsSorted(ind1User);
        Assert.assertNotNull(sorted, "List tidak boleh null");
        // Verifikasi urutan (descending water credit)
        for (int i = 0; i < sorted.size() - 1; i++) {
            Double c1 = sorted.get(i).getWaterCredit();
            Double c2 = sorted.get(i + 1).getWaterCredit();
            if (c1 != null && c2 != null) {
                Assert.assertTrue(c1 >= c2, "List harus terurut descending berdasarkan water credit");
            }
        }
        System.out.println("[PASS] getAllIndividualsSorted: " + sorted.size() + " individual terurut");
    }

    // ===================================================
    //  GROUP 11 : ComplainDAO
    // ===================================================

    /**
     * Individual mengirim komplain ke Government.
     */
    @Test(groups = "complain",
          dependsOnMethods = {"testLoginGovernmentSuccess", "testApproveIndividual1"},
          description = "Individual berhasil mengirim komplain ke Government")
    public void testInsertComplain() {
        if (ind1User == null) ind1User = userDAO.findByEmail(IND1_EMAIL).orElseThrow();
        if (govUser  == null) govUser  = userDAO.findByEmail(GOV_EMAIL).orElseThrow();

        Complain c = new Complain();
        complainId = UUID.randomUUID().toString();
        c.setIdComplaint(complainId);
        c.setSender(ind1User);
        c.setReceiver(govUser);
        c.setTitle("Komplain Tes " + SUFFIX);
        c.setDescription("Ini adalah komplain pengujian otomatis.");
        c.setStatus(Complain.ComplaintStatus.PENDING);

        complainDAO.insert(c);

        Optional<Complain> found = complainDAO.findById(complainId);
        Assert.assertTrue(found.isPresent(), "Komplain harus tersimpan di database");
        Assert.assertEquals(found.get().getTitle(), "Komplain Tes " + SUFFIX);
        Assert.assertEquals(found.get().getStatus(), Complain.ComplaintStatus.PENDING);

        System.out.println("[PASS] Komplain tersimpan: " + complainId);
    }

    /**
     * Government membalas komplain (update status ke IN_PROGRESS, tambahkan reply).
     */
    @Test(groups = "complain",
          dependsOnMethods = "testInsertComplain",
          description = "Government berhasil membalas dan mengupdate status komplain")
    public void testUpdateComplain() {
        Complain c = complainDAO.findById(complainId).orElseThrow();
        c.setStatus(Complain.ComplaintStatus.IN_PROGRESS);
        c.setReply("Terima kasih, sedang ditindaklanjuti.");
        c.setUpdatedAt(java.time.LocalDateTime.now());
        c.setRepliedAt(java.time.LocalDateTime.now());

        complainDAO.update(c);

        Complain updated = complainDAO.findById(complainId).orElseThrow();
        Assert.assertEquals(updated.getStatus(), Complain.ComplaintStatus.IN_PROGRESS);
        Assert.assertEquals(updated.getReply(), "Terima kasih, sedang ditindaklanjuti.");
        Assert.assertNotNull(updated.getRepliedAt(), "repliedAt harus terisi");

        System.out.println("[PASS] Komplain diupdate status: " + updated.getStatus());
    }

    /**
     * findBySender mengembalikan komplain yang dikirim oleh individual.
     */
    @Test(groups = "complain",
          dependsOnMethods = "testInsertComplain",
          description = "findBySender mengembalikan komplain yang dikirim user")
    public void testFindComplainBySender() {
        if (ind1User == null) ind1User = userDAO.findByEmail(IND1_EMAIL).orElseThrow();
        List<Complain> complains = complainDAO.findBySender(ind1User);
        Assert.assertNotNull(complains);
        Assert.assertFalse(complains.isEmpty(), "Harus ada komplain yang dikirim ind1");

        boolean found = complains.stream().anyMatch(c -> complainId.equals(c.getIdComplaint()));
        Assert.assertTrue(found, "Komplain tes harus ada di daftar sender");
        System.out.println("[PASS] findBySender: " + complains.size() + " komplain ditemukan");
    }

    /**
     * findByReceiver mengembalikan komplain yang diterima Government.
     */
    @Test(groups = "complain",
          dependsOnMethods = "testInsertComplain",
          description = "findByReceiver mengembalikan komplain yang diterima Government")
    public void testFindComplainByReceiver() {
        if (govUser == null) govUser = userDAO.findByEmail(GOV_EMAIL).orElseThrow();
        List<Complain> complains = complainDAO.findByReceiver(govUser);
        Assert.assertNotNull(complains);
        Assert.assertFalse(complains.isEmpty(), "Government harus ada komplain masuk");

        boolean found = complains.stream().anyMatch(c -> complainId.equals(c.getIdComplaint()));
        Assert.assertTrue(found, "Komplain tes harus ada di daftar receiver");
        System.out.println("[PASS] findByReceiver: " + complains.size() + " komplain ditemukan");
    }

    /**
     * Government menyelesaikan komplain (RESOLVED).
     */
    @Test(groups = "complain",
          dependsOnMethods = "testUpdateComplain",
          description = "Government menyelesaikan komplain dengan status RESOLVED")
    public void testResolveComplain() {
        Complain c = complainDAO.findById(complainId).orElseThrow();
        c.setStatus(Complain.ComplaintStatus.RESOLVED);
        c.setReply("Masalah telah diselesaikan.");
        c.setUpdatedAt(java.time.LocalDateTime.now());
        c.setRepliedAt(java.time.LocalDateTime.now());

        complainDAO.update(c);

        Complain resolved = complainDAO.findById(complainId).orElseThrow();
        Assert.assertEquals(resolved.getStatus(), Complain.ComplaintStatus.RESOLVED);

        System.out.println("[PASS] Komplain RESOLVED: " + complainId);
    }

    // ===================================================
    //  GROUP 12 : Cleanup - Hapus Data Uji dari Database
    // ===================================================

    /**
     * Membersihkan semua data uji yang dibuat selama test suite ini.
     * Urutan hapus penting karena foreign key constraint.
     */
    @Test(groups = "cleanup",
          dependsOnGroups = {"complain", "wc_request", "transaction", "product", "footprint"},
          alwaysRun = true,
          description = "Membersihkan semua data uji dari database")
    public void testCleanupTestData() {
        System.out.println("[CLEANUP] Memulai pembersihan data uji dengan SUFFIX: " + SUFFIX);

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Hapus komplain
                if (complainId != null) {
                    executeUpdate(conn, "DELETE FROM complains WHERE id_complain = ?", complainId);
                }

                // 2. Hapus water credit requests yang dibuat oleh ind2
                if (ind2User != null) {
                    Individual ind2 = individualDAO.findByUser(ind2User).orElse(null);
                    if (ind2 != null) {
                        executeUpdate(conn,
                            "DELETE FROM water_credit_request WHERE id_buyer = ?",
                            ind2.getIdIndividual());
                    }
                }

                // 3. Hapus transaksi milik ind1
                if (ind1User != null) {
                    Individual ind1 = individualDAO.findByUser(ind1User).orElse(null);
                    if (ind1 != null) {
                        executeUpdate(conn,
                            "DELETE FROM transaction WHERE id_buyer = ?",
                            ind1.getIdIndividual());
                    }
                }

                // 4. Hapus produk
                if (testProductId > 0) {
                    executeUpdate(conn, "DELETE FROM product WHERE id_product = ?", testProductId);
                }

                // 5. Hapus juga produk random test jika ada (dari testCreateProductInsufficientCreditFails)
                executeUpdate(conn, "DELETE FROM product WHERE product_name LIKE ?",
                    "Produk Tes " + SUFFIX + "%");
                executeUpdate(conn, "DELETE FROM product WHERE product_name LIKE ?",
                    "Produk Updated " + SUFFIX + "%");

                // 6. Hapus individual
                executeUpdate(conn,
                    "DELETE FROM individual WHERE id_user IN " +
                    "(SELECT id_user FROM user WHERE email IN (?, ?))",
                    IND1_EMAIL, IND2_EMAIL);

                // 7. Hapus company
                executeUpdate(conn,
                    "DELETE FROM company WHERE id_user IN " +
                    "(SELECT id_user FROM user WHERE email LIKE ?)",
                    "%_" + SUFFIX + "@test.com");

                // 8. Hapus government
                executeUpdate(conn,
                    "DELETE FROM government WHERE id_user = " +
                    "(SELECT id_user FROM user WHERE email = ?)",
                    GOV_EMAIL);

                // 9. Hapus semua user test
                executeUpdate(conn,
                    "DELETE FROM user WHERE email LIKE ?",
                    "%_" + SUFFIX + "@test.com");

                conn.commit();
                System.out.println("[PASS] Data uji berhasil dibersihkan.");
            } catch (Exception ex) {
                conn.rollback();
                System.err.println("[CLEANUP ERROR] Rollback karena: " + ex.getMessage());
                // Tidak fail test cleanup agar tidak menimbulkan confusion
            }
        } catch (SQLException e) {
            System.err.println("[CLEANUP ERROR] Koneksi gagal: " + e.getMessage());
        }
    }

    // ===================================================
    //  Helper Methods
    // ===================================================

    /**
     * Helper: eksekusi UPDATE/DELETE dengan satu parameter String atau int.
     */
    private void executeUpdate(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof String s) {
                    ps.setString(i + 1, s);
                } else if (params[i] instanceof Integer n) {
                    ps.setInt(i + 1, n);
                } else if (params[i] instanceof Long l) {
                    ps.setLong(i + 1, l);
                } else {
                    ps.setObject(i + 1, params[i]);
                }
            }
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("[CLEANUP]   Deleted " + rows + " row(s) | SQL: "
                    + sql.substring(0, Math.min(sql.length(), 60)) + "...");
            }
        }
    }

    /**
     * Helper: hapus semua data user berdasarkan email (cascade manual).
     */
    private void cleanupByEmail(String email) {
        try {
            Optional<User> u = userDAO.findByEmail(email);
            if (u.isEmpty()) return;
            User user = u.get();

            // Hapus company jika ada
            companyDAO.findByUser(user).ifPresent(c -> {
                try (Connection conn = DatabaseHelper.getConnection()) {
                    executeUpdate(conn, "DELETE FROM company WHERE id_company = ?", c.getIdCompany());
                } catch (Exception ex) { /* ignore */ }
            });

            userDAO.delete(user.getIdUser());
        } catch (Exception ex) {
            System.err.println("[CLEANUP] Gagal hapus email=" + email + ": " + ex.getMessage());
        }
    }
}
