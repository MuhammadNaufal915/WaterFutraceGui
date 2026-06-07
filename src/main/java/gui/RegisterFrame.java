package gui;

import dao.RegionDAO;
import model.Region;
import service.AuthService;
import utils.ValidationUtil;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * RegisterFrame - Form registrasi user baru yang ringkas dengan JScrollPane kustom.
 */
public class RegisterFrame extends JDialog {

    private static final Color BG_DARK     = new Color(238, 242, 247);
    private static final Color BG_CARD     = new Color(255, 255, 255);
    private static final Color ACCENT_CYAN = new Color(37, 99, 235); // Royal Blue
    private static final Color TEXT_DARK   = new Color(15, 23, 42); // Dark slate
    private static final Color TEXT_WHITE  = Color.WHITE;
    private static final Color TEXT_MUTED  = new Color(100, 116, 139);
    private static final Color BTN_PRIMARY = new Color(37, 99, 235);
    private static final Color INPUT_BG    = new Color(248, 250, 252);
    private static final Color INPUT_BORDER = new Color(226, 232, 240);
    private static final Color ERROR_RED   = new Color(239, 68, 68);
    private static final Font  FONT_LABEL  = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font  FONT_LABEL_PL = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 22);

    private final AuthService authService = new AuthService();
    private final RegionDAO   regionDAO   = new RegionDAO();

    // Form fields
    private JTextField  tfName, tfEmail, tfAlamat, tfExtra;
    private JPasswordField pfPassword, pfConfirm;
    private JComboBox<String> cbRole, cbRegion;
    private JSpinner    spUsia;
    private JLabel      lblExtra, lblStatus, lblUsia;
    private JPanel      pnlExtra;
    private Component   usiaStrut; // Menyimpan referensi jarak spacing dinamis

    public RegisterFrame(Frame parent) {
        super(parent, "Registrasi Akun Baru", true);
        // Ukuran vertikal diperkecil (dari 700 menjadi 550) agar ringkas dan muat di layar kecil
        setSize(480, 550); 
        setLocationRelativeTo(parent);
        setResizable(true); // Izinkan resize karena sudah didukung ScrollPane
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        setContentPane(root);

        // Header (Tetap diam di atas, tidak ikut ter-scroll)
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER));
        header.setBackground(BG_DARK);
        header.setBorder(new EmptyBorder(20, 0, 10, 0));
        JLabel title = new JLabel("Buat Akun Baru");
        title.setFont(FONT_TITLE);
        title.setForeground(ACCENT_CYAN);
        header.add(title);
        root.add(header, BorderLayout.NORTH);

        // Container Form (Menggunakan BoxLayout Y_AXIS)
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Pengisian Komponen Form
        card.add(makeLabel("Nama Lengkap"));
        tfName = makeTextField("Masukkan nama lengkap");
        card.add(tfName); card.add(Box.createVerticalStrut(12));

        card.add(makeLabel("Email"));
        tfEmail = makeTextField("email@contoh.com");
        card.add(tfEmail); card.add(Box.createVerticalStrut(12));

        card.add(makeLabel("Password"));
        pfPassword = makePasswordField();
        card.add(pfPassword); card.add(Box.createVerticalStrut(12));

        card.add(makeLabel("Konfirmasi Password"));
        pfConfirm = makePasswordField();
        card.add(pfConfirm); card.add(Box.createVerticalStrut(12));

        card.add(makeLabel("Alamat"));
        tfAlamat = makeTextField("Masukkan alamat");
        card.add(tfAlamat); card.add(Box.createVerticalStrut(12));

        card.add(makeLabel("Daftar Sebagai"));
        cbRole = new JComboBox<>(new String[]{"INDIVIDUAL", "COMPANY", "GOVERMENT"});
        styleComboBox(cbRole);
        card.add(cbRole); card.add(Box.createVerticalStrut(12));

        card.add(makeLabel("Region / Wilayah"));
        cbRegion = new JComboBox<>();
        styleComboBox(cbRegion);
        loadRegions();
        card.add(cbRegion); card.add(Box.createVerticalStrut(12));

        // Bagian Extra Field Dinamis
        pnlExtra = new JPanel();
        pnlExtra.setLayout(new BoxLayout(pnlExtra, BoxLayout.Y_AXIS));
        pnlExtra.setBackground(BG_CARD);
        pnlExtra.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblExtra = makeLabel("Pekerjaan");
        tfExtra = makeTextField("Masukkan pekerjaan");
        pnlExtra.add(lblExtra);
        pnlExtra.add(tfExtra);
        
        usiaStrut = Box.createVerticalStrut(12);
        pnlExtra.add(usiaStrut);

        lblUsia = makeLabel("Usia");
        spUsia = new JSpinner(new SpinnerNumberModel(25, 1, 120, 1));
        styleSpinner(spUsia);
        pnlExtra.add(lblUsia);
        pnlExtra.add(spUsia);

        card.add(pnlExtra); card.add(Box.createVerticalStrut(10));

        // Label Status Error/Validasi
        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(ERROR_RED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblStatus); card.add(Box.createVerticalStrut(12));

        // Tombol Aksi
        JButton btnRegister = makeButton("Daftar Sekarang", BTN_PRIMARY);
        btnRegister.addActionListener(e -> doRegister());
        card.add(btnRegister); card.add(Box.createVerticalStrut(10));

        JButton btnBack = makeButton("← Kembali ke Login", BG_DARK);
        btnBack.setForeground(ACCENT_CYAN);
        btnBack.addActionListener(e -> dispose());
        card.add(btnBack);

        // MEMBUAT SCROLLPANE & MODIFIKASI TAMPILANNYA (Agar tidak kaku bawaan OS)
        JScrollPane scrollPane = new JScrollPane(card);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Scroll terasa lebih mulus
        styleScrollBar(scrollPane.getVerticalScrollBar());
        
        root.add(scrollPane, BorderLayout.CENTER);

        // Pemicu perubahan field dinamis saat pertama kali dibuka
        cbRole.addActionListener(e -> updateExtraField());
        updateExtraField();
    }

    private void updateExtraField() {
        String role = (String) cbRole.getSelectedItem();
        boolean isIndividual = "INDIVIDUAL".equals(role);

        // Kontrol visibilitas berbasis object referensi langsung (Aman dari bug index komponen)
        lblUsia.setVisible(isIndividual);
        spUsia.setVisible(isIndividual);
        usiaStrut.setVisible(isIndividual);

        if (isIndividual) {
            lblExtra.setText("Pekerjaan");
            tfExtra.setToolTipText("Masukkan pekerjaan");
        } else if ("COMPANY".equals(role)) {
            lblExtra.setText("Sektor Perusahaan");
            tfExtra.setToolTipText("Contoh: Manufaktur, Pertanian, dll");
        } else if ("GOVERMENT".equals(role)) {
            lblExtra.setText("Nama Water Basin");
            tfExtra.setToolTipText("Nama sungai / daerah aliran sungai");
        }
        
        pnlExtra.revalidate();
        pnlExtra.repaint();
    }

    private void loadRegions() {
        cbRegion.removeAllItems();
        try {
            List<Region> regions = regionDAO.findAll();
            if (regions != null) {
                for (Region r : regions) cbRegion.addItem(r.getName());
            }
        } catch (Exception e) {
            showError("Gagal memuat wilayah database.");
        }
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

    // ---- Kustomisasi Helper UI & Styling Modern ----

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 2, 4, 0));
        return lbl;
    }

    private JTextField makeTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(FONT_LABEL_PL);
        tf.setForeground(TEXT_DARK);
        tf.setBackground(INPUT_BG);
        tf.setCaretColor(ACCENT_CYAN);
        tf.setBorder(new CompoundBorder(
            new LineBorder(INPUT_BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        tf.setToolTipText(placeholder);
        return tf;
    }

    private JPasswordField makePasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(FONT_LABEL_PL);
        pf.setForeground(TEXT_DARK);
        pf.setBackground(INPUT_BG);
        pf.setCaretColor(ACCENT_CYAN);
        pf.setBorder(new CompoundBorder(
            new LineBorder(INPUT_BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        pf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        pf.setAlignmentX(Component.LEFT_ALIGNMENT);
        return pf;
    }

    private void styleComboBox(JComboBox<?> cb) {
        cb.setFont(FONT_LABEL_PL);
        cb.setForeground(TEXT_DARK);
        cb.setBackground(INPUT_BG);
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb.setBorder(new LineBorder(INPUT_BORDER, 1, true));
    }

    private void styleSpinner(JSpinner sp) {
        sp.setFont(FONT_LABEL_PL);
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setBorder(new LineBorder(INPUT_BORDER, 1, true));
        
        // Menembus warna editor internal spinner agar senada dengan warna input_bg
        JComponent editor = sp.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(INPUT_BG);
            tf.setForeground(TEXT_DARK);
            tf.setCaretColor(ACCENT_CYAN);
            tf.setBorder(new EmptyBorder(0, 4, 0, 4));
        }
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(TEXT_WHITE);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addMouseListener(new MouseAdapter() {
            final Color orig = bg;
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(orig.brighter()); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(orig); }
        });
        return btn;
    }

    /**
     * Kustomisasi Scrollbar UI agar memiliki tampilan modern pipih (flat dark)
     */
    private void styleScrollBar(JScrollBar scrollBar) {
        scrollBar.setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
        scrollBar.setBackground(BG_DARK);
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                g.setColor(BG_DARK);
                g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                g.setColor(INPUT_BG);
                g.fillRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height);
            }
            private JButton createZeroButton() {
                JButton jb = new JButton();
                jb.setPreferredSize(new Dimension(0, 0));
                return jb;
            }
        });
    }
}