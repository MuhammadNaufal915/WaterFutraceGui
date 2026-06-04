package gui;

import dao.RegionDAO;
import model.Region;
import service.AuthService;
import utils.ValidationUtil;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * RegisterFrame - Form registrasi user baru (INDIVIDUAL / COMPANY / GOVERMENT)
 */
public class RegisterFrame extends JDialog {

    private static final Color BG_DARK     = new Color(15, 23, 42);
    private static final Color BG_CARD     = new Color(30, 41, 59);
    private static final Color ACCENT_CYAN = new Color(0, 212, 255);
    private static final Color TEXT_WHITE  = new Color(248, 250, 252);
    private static final Color TEXT_MUTED  = new Color(148, 163, 184);
    private static final Color BTN_PRIMARY = new Color(6, 182, 212);
    private static final Color INPUT_BG    = new Color(51, 65, 85);
    private static final Color ERROR_RED   = new Color(239, 68, 68);
    private static final Font  FONT_LABEL  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 22);

    private final AuthService authService = new AuthService();
    private final RegionDAO   regionDAO   = new RegionDAO();

    // Form fields
    private JTextField  tfName, tfEmail, tfAlamat, tfExtra;
    private JPasswordField pfPassword, pfConfirm;
    private JComboBox<String> cbRole, cbRegion;
    private JSpinner    spUsia;
    private JLabel      lblExtra, lblStatus;
    private JPanel      pnlExtra;

    public RegisterFrame(Frame parent) {
        super(parent, "Registrasi Akun Baru", true);
        setSize(520, 700);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER));
        header.setBackground(BG_DARK);
        header.setBorder(new EmptyBorder(24, 0, 8, 0));
        JLabel title = new JLabel("Buat Akun Baru");
        title.setFont(FONT_TITLE);
        title.setForeground(ACCENT_CYAN);
        header.add(title);
        root.add(header, BorderLayout.NORTH);

        // Card center
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
            new EmptyBorder(0, 24, 16, 24),
            new EmptyBorder(20, 20, 20, 20)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Nama
        card.add(makeLabel("Nama Lengkap"));
        tfName = makeTextField("Masukkan nama lengkap");
        card.add(tfName); card.add(Box.createVerticalStrut(10));

        // Email
        card.add(makeLabel("Email"));
        tfEmail = makeTextField("email@contoh.com");
        card.add(tfEmail); card.add(Box.createVerticalStrut(10));

        // Password
        card.add(makeLabel("Password"));
        pfPassword = makePasswordField();
        card.add(pfPassword); card.add(Box.createVerticalStrut(10));

        // Konfirmasi Password
        card.add(makeLabel("Konfirmasi Password"));
        pfConfirm = makePasswordField();
        card.add(pfConfirm); card.add(Box.createVerticalStrut(10));

        // Alamat
        card.add(makeLabel("Alamat"));
        tfAlamat = makeTextField("Masukkan alamat");
        card.add(tfAlamat); card.add(Box.createVerticalStrut(10));

        // Role
        card.add(makeLabel("Daftar Sebagai"));
        cbRole = new JComboBox<>(new String[]{"INDIVIDUAL", "COMPANY", "GOVERMENT"});
        styleComboBox(cbRole);
        card.add(cbRole); card.add(Box.createVerticalStrut(10));

        // Region
        card.add(makeLabel("Region / Wilayah"));
        cbRegion = new JComboBox<>();
        styleComboBox(cbRegion);
        loadRegions();
        card.add(cbRegion); card.add(Box.createVerticalStrut(10));

        // Extra field (dinamis berdasarkan role)
        pnlExtra = new JPanel();
        pnlExtra.setLayout(new BoxLayout(pnlExtra, BoxLayout.Y_AXIS));
        pnlExtra.setBackground(BG_CARD);
        lblExtra = makeLabel("Pekerjaan");
        tfExtra = makeTextField("Masukkan pekerjaan");

        pnlExtra.add(lblExtra);
        pnlExtra.add(tfExtra);
        pnlExtra.add(Box.createVerticalStrut(10));

        // Usia (INDIVIDUAL only)
        JLabel lblUsia = makeLabel("Usia");
        pnlExtra.add(lblUsia);
        spUsia = new JSpinner(new SpinnerNumberModel(25, 1, 120, 1));
        styleSpinner(spUsia);
        pnlExtra.add(spUsia);
        card.add(pnlExtra); card.add(Box.createVerticalStrut(10));

        // Status label
        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(ERROR_RED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblStatus); card.add(Box.createVerticalStrut(12));

        // Tombol Register
        JButton btnRegister = makeButton("Daftar Sekarang", BTN_PRIMARY);
        btnRegister.addActionListener(e -> doRegister());
        card.add(btnRegister); card.add(Box.createVerticalStrut(8));

        // Link kembali
        JButton btnBack = makeButton("← Kembali ke Login", BG_DARK);
        btnBack.setForeground(ACCENT_CYAN);
        btnBack.addActionListener(e -> dispose());
        card.add(btnBack);

        root.add(card, BorderLayout.CENTER);

        // Update extra field saat role berubah
        cbRole.addActionListener(e -> updateExtraField());
        updateExtraField();
    }

    private void updateExtraField() {
        String role = (String) cbRole.getSelectedItem();
        lblExtra.setVisible(true);
        tfExtra.setVisible(true);
        if ("INDIVIDUAL".equals(role)) {
            lblExtra.setText("Pekerjaan");
            tfExtra.setToolTipText("Masukkan pekerjaan");
            spUsia.setVisible(true);
            pnlExtra.getComponent(2).setVisible(true); // usia label
        } else if ("COMPANY".equals(role)) {
            lblExtra.setText("Sektor Perusahaan");
            tfExtra.setToolTipText("Contoh: Manufaktur, Pertanian, dll");
            spUsia.setVisible(false);
            pnlExtra.getComponent(2).setVisible(false);
        } else if ("GOVERMENT".equals(role)) {
            lblExtra.setText("Nama Water Basin");
            tfExtra.setToolTipText("Nama sungai / daerah aliran sungai");
            spUsia.setVisible(false);
            pnlExtra.getComponent(2).setVisible(false);
        }
        pnlExtra.revalidate();
        pnlExtra.repaint();
    }

    private void loadRegions() {
        cbRegion.removeAllItems();
        List<Region> regions = regionDAO.findAll();
        for (Region r : regions) cbRegion.addItem(r.getName());
    }

    private void doRegister() {
        String name     = tfName.getText().trim();
        String email    = tfEmail.getText().trim();
        String password = new String(pfPassword.getPassword());
        String confirm  = new String(pfConfirm.getPassword());
        String alamat   = tfAlamat.getText().trim();
        String role     = (String) cbRole.getSelectedItem();
        String region   = (String) cbRegion.getSelectedItem();
        String extra    = tfExtra.getText().trim();
        int    usia     = (int) spUsia.getValue();

        if (ValidationUtil.isEmpty(name)) { showError("Nama wajib diisi."); return; }
        if (!ValidationUtil.isValidEmail(email)) { showError("Format email tidak valid."); return; }
        if (!ValidationUtil.hasMinLength(password, 6)) { showError("Password minimal 6 karakter."); return; }
        if (!password.equals(confirm)) { showError("Konfirmasi password tidak cocok."); return; }
        if (ValidationUtil.isEmpty(alamat)) { showError("Alamat wajib diisi."); return; }
        if (region == null) { showError("Pilih region terlebih dahulu."); return; }
        if (ValidationUtil.isEmpty(extra)) { showError(lblExtra.getText() + " wajib diisi."); return; }

        try {
            authService.register(name, email, password, alamat, role, region, extra, usia);
            JOptionPane.showMessageDialog(this,
                "<html><b>Registrasi Berhasil!</b><br>" +
                ("GOVERMENT".equals(role) ? "Akun Anda langsung aktif." : "Akun Anda menunggu persetujuan Government.") +
                "</html>",
                "Sukses", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String msg) {
        lblStatus.setText("<html>" + msg + "</html>");
        lblStatus.setForeground(ERROR_RED);
    }

    // ---- Builder Helpers ----

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField makeTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(FONT_LABEL);
        tf.setForeground(TEXT_WHITE);
        tf.setBackground(INPUT_BG);
        tf.setCaretColor(ACCENT_CYAN);
        tf.setBorder(new CompoundBorder(
            new LineBorder(new Color(71, 85, 105), 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        tf.setPreferredSize(new Dimension(Integer.MAX_VALUE, 38));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        tf.setToolTipText(placeholder);
        return tf;
    }

    private JPasswordField makePasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(FONT_LABEL);
        pf.setForeground(TEXT_WHITE);
        pf.setBackground(INPUT_BG);
        pf.setCaretColor(ACCENT_CYAN);
        pf.setBorder(new CompoundBorder(
            new LineBorder(new Color(71, 85, 105), 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        pf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        pf.setAlignmentX(Component.LEFT_ALIGNMENT);
        return pf;
    }

    private void styleComboBox(JComboBox<?> cb) {
        cb.setFont(FONT_LABEL);
        cb.setForeground(TEXT_WHITE);
        cb.setBackground(INPUT_BG);
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void styleSpinner(JSpinner sp) {
        sp.setFont(FONT_LABEL);
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(TEXT_WHITE);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addMouseListener(new MouseAdapter() {
            final Color orig = bg;
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(orig.brighter()); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(orig); }
        });
        return btn;
    }
}
