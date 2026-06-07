package gui;

import dao.CompanyDAO;
import model.Company;
import model.User;
import utils.SessionManager;
import utils.ValidationUtil;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.Optional;

/**
 * CompanyPanel - Company mengatur parameter eta dan sl, serta melihat info kredit.
 */
public class CompanyPanel extends JPanel {

    private static final Color BG         = new Color(238, 242, 247);
    private static final Color CARD_BG    = new Color(255, 255, 255);
    private static final Color CYAN       = new Color(37, 99, 235); // Royal Blue
    private static final Color GREEN      = new Color(16, 185, 129);
    private static final Color TEXT_WHITE = new Color(15, 23, 42); // Dark text
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color INPUT_BG   = new Color(248, 250, 252);

    private final CompanyDAO companyDAO = new CompanyDAO();

    private JTextField tfEta, tfSl, tfSector;
    private JLabel     lblCredit, lblRegion, lblStatus;

    public CompanyPanel() {
        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        initUI();
    }

    private void initUI() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BG);
        hdr.setBorder(new EmptyBorder(24, 32, 12, 32));
        JLabel title = new JLabel("⚙️  Parameter Perusahaan");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);
        hdr.add(title, BorderLayout.WEST);
        add(hdr, BorderLayout.NORTH);

        // 2-column layout
        JPanel content = new JPanel(new GridLayout(1, 2, 20, 0));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(8, 32, 32, 32));

        // Left: Info card
        JPanel infoCard = new JPanel();
        infoCard.setBackground(CARD_BG);
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel infoTitle = new JLabel("Informasi Perusahaan");
        infoTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        infoTitle.setForeground(CYAN);
        infoCard.add(infoTitle);
        infoCard.add(Box.createVerticalStrut(20));

        infoCard.add(infoRow("💧 Water Credit :", null));
        lblCredit = infoValue("Memuat...");
        infoCard.add(lblCredit); infoCard.add(Box.createVerticalStrut(10));

        infoCard.add(infoRow("🗺️ Region :", null));
        lblRegion = infoValue("Memuat...");
        infoCard.add(lblRegion); infoCard.add(Box.createVerticalStrut(10));

        JButton btnRefresh = smallButton("🔄 Refresh Info", CYAN);
        btnRefresh.addActionListener(e -> refresh());
        infoCard.add(btnRefresh);

        // Right: Form parameter
        JPanel formCard = new JPanel();
        formCard.setBackground(CARD_BG);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel formTitle = new JLabel("Set Parameter Efisiensi");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        formTitle.setForeground(CYAN);
        formCard.add(formTitle);
        formCard.add(Box.createVerticalStrut(6));
        JLabel desc = new JLabel("<html><small>Parameter ini digunakan saat Government mendistribusikan water credit.</small></html>");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        desc.setForeground(TEXT_MUTED);
        formCard.add(desc);
        formCard.add(Box.createVerticalStrut(20));

        formCard.add(fieldLabel("Sektor Perusahaan"));
        tfSector = inputField("Contoh: Manufaktur");
        formCard.add(tfSector); formCard.add(Box.createVerticalStrut(12));

        formCard.add(fieldLabel("Efisiensi Penggunaan Air (η) [0 - 1]"));
        tfEta = inputField("Contoh: 0.75");
        formCard.add(tfEta); formCard.add(Box.createVerticalStrut(12));

        formCard.add(fieldLabel("Indeks Kelangkaan Air (Sl) [0 - 1]"));
        tfSl = inputField("Contoh: 0.4");
        formCard.add(tfSl); formCard.add(Box.createVerticalStrut(20));

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(239, 68, 68));
        formCard.add(lblStatus);
        formCard.add(Box.createVerticalStrut(12));

        JButton btnSave = actionButton("💾  Simpan Parameter", GREEN);
        btnSave.addActionListener(e -> saveParameters());
        formCard.add(btnSave);

        content.add(infoCard);
        content.add(formCard);
        add(content, BorderLayout.CENTER);
    }

    public void refresh() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        companyDAO.findByUser(user).ifPresentOrElse(c -> {
            lblCredit.setText(String.format("%.4f", c.getWatercredit() != null ? c.getWatercredit() : 0.0));
            lblRegion.setText(c.getRegion() != null ? c.getRegion().getName() : "Belum diset");
            if (c.getSector() != null) tfSector.setText(c.getSector());
            if (c.getEta() != null)    tfEta.setText(String.valueOf(c.getEta()));
            if (c.getSl()  != null)    tfSl.setText(String.valueOf(c.getSl()));
        }, () -> {
            lblCredit.setText("0.0000");
            lblRegion.setText("Belum diset");
        });
    }

    private void saveParameters() {
        String sectorStr = tfSector.getText().trim();
        String etaStr    = tfEta.getText().trim();
        String slStr     = tfSl.getText().trim();

        if (ValidationUtil.isEmpty(sectorStr)) { setErr("Sektor wajib diisi."); return; }
        if (!ValidationUtil.isRatio(etaStr))   { setErr("η (eta) harus angka antara 0 dan 1."); return; }
        if (!ValidationUtil.isRatio(slStr))    { setErr("Sl harus angka antara 0 dan 1."); return; }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        try {
            Company c = companyDAO.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Profil Company tidak ditemukan."));
            c.setSector(sectorStr);
            c.setEta(Float.parseFloat(etaStr));
            c.setSl(Float.parseFloat(slStr));
            companyDAO.update(c);
            lblStatus.setText("✅ Parameter berhasil disimpan.");
            lblStatus.setForeground(GREEN);
        } catch (Exception ex) {
            setErr(ex.getMessage());
        }
    }

    private void setErr(String msg) {
        lblStatus.setText("❌ " + msg);
        lblStatus.setForeground(new Color(239, 68, 68));
    }

    private JLabel infoRow(String text, Object dummy) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JLabel infoValue(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 18));
        l.setForeground(CYAN);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JTextField inputField(String tooltip) {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setForeground(TEXT_WHITE);
        tf.setBackground(INPUT_BG);
        tf.setCaretColor(CYAN);
        tf.setBorder(new CompoundBorder(new LineBorder(new Color(226, 232, 240), 1, true), new EmptyBorder(8, 12, 8, 12)));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tf.setAlignmentX(LEFT_ALIGNMENT);
        tf.setToolTipText(tooltip);
        return tf;
    }

    private JButton actionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        return btn;
    }

    private JButton smallButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        return btn;
    }
}
