package gui;

import model.User;
import service.FootprintService;
import utils.SessionManager;
import utils.ValidationUtil;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * GovernmentPanel - Perhitungan BWF dan distribusi water credit.
 */
public class GovernmentPanel extends JPanel {

    private static final Color BG = new Color(10, 14, 26);
    private static final Color CARD_BG = new Color(18, 28, 50);
    private static final Color CYAN = new Color(0, 212, 255);
    private static final Color GREEN = new Color(16, 185, 129);
    private static final Color AMBER = new Color(245, 158, 11);
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color INPUT_BG = new Color(30, 44, 70);
    private static final Color ERROR_RED = new Color(239, 68, 68);

    private final FootprintService footprintService = new FootprintService();

    // BWF Fields
    private JTextField tfBWI, tfLRF, tfVolume, tfTime;
    private JTextArea taResult;
    private JLabel lblStatus;

    public GovernmentPanel() {
        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        initUI();
    }

    private void initUI() {
        // Title
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BG);
        hdr.setBorder(new EmptyBorder(24, 32, 12, 32));
        JLabel title = new JLabel("🌊  Distribusi Water Credit (BWF)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);
        hdr.add(title, BorderLayout.WEST);
        add(hdr, BorderLayout.NORTH);

        // Main content
        JPanel content = new JPanel(new GridLayout(1, 2, 20, 0));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(8, 32, 24, 32));

        // Left: Form input BWF
        JPanel formCard = new JPanel();
        formCard.setBackground(CARD_BG);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel formTitle = new JLabel("Parameter Blue Water Footprint");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        formTitle.setForeground(CYAN);
        formCard.add(formTitle);
        formCard.add(Box.createVerticalStrut(6));

        JLabel desc = new JLabel("<html><small>Formula: BWF = BWI + LRF × (V / T)</small></html>");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        desc.setForeground(TEXT_MUTED);
        formCard.add(desc);
        formCard.add(Box.createVerticalStrut(20));

        formCard.add(fieldLabel("Blue Water Incorporation (BWI) [m³/tahun]"));
        tfBWI = inputField("Contoh: 1000.5");
        formCard.add(tfBWI);
        formCard.add(Box.createVerticalStrut(12));

        formCard.add(fieldLabel("Lost Return Flow (LRF) [fraksi 0-1]"));
        tfLRF = inputField("Contoh: 0.3");
        formCard.add(tfLRF);
        formCard.add(Box.createVerticalStrut(12));

        formCard.add(fieldLabel("Volume Air (V) [m³]"));
        tfVolume = inputField("Contoh: 5000");
        formCard.add(tfVolume);
        formCard.add(Box.createVerticalStrut(12));

        formCard.add(fieldLabel("Waktu (T) [tahun]"));
        tfTime = inputField("Contoh: 1");
        formCard.add(tfTime);
        formCard.add(Box.createVerticalStrut(20));

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(ERROR_RED);
        formCard.add(lblStatus);
        formCard.add(Box.createVerticalStrut(12));

        JButton btnDistribute = actionButton("🚀  Hitung & Distribusikan Credit", GREEN);
        btnDistribute.addActionListener(e -> doDistribute());
        formCard.add(btnDistribute);

        // Right: Hasil
        JPanel resultCard = new JPanel(new BorderLayout());
        resultCard.setBackground(CARD_BG);
        resultCard.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel resultTitle = new JLabel("Hasil Distribusi");
        resultTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        resultTitle.setForeground(CYAN);
        resultCard.add(resultTitle, BorderLayout.NORTH);

        taResult = new JTextArea("Hasil akan muncul di sini setelah distribusi dilakukan.");
        taResult.setFont(new Font("Consolas", Font.PLAIN, 13));
        taResult.setForeground(TEXT_WHITE);
        taResult.setBackground(new Color(10, 20, 40));
        taResult.setEditable(false);
        taResult.setLineWrap(true);
        taResult.setWrapStyleWord(true);
        taResult.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(taResult);
        scroll.setBorder(new LineBorder(new Color(30, 41, 59), 1));
        resultCard.add(scroll, BorderLayout.CENTER);

        content.add(formCard);
        content.add(resultCard);
        add(content, BorderLayout.CENTER);
    }

    public void refresh() {
        /* Data otomatis dimuat saat distribusi */ }

    @SuppressWarnings("unchecked")
    private void doDistribute() {
        String bwiStr = tfBWI.getText().trim();
        String lrfStr = tfLRF.getText().trim();
        String volumeStr = tfVolume.getText().trim();
        String timeStr = tfTime.getText().trim();

        if (!ValidationUtil.isPositiveDouble(bwiStr)) {
            setError("BWI harus angka positif.");
            return;
        }
        if (!ValidationUtil.isPositiveDouble(lrfStr)) {
            setError("LRF harus angka positif.");
            return;
        }
        if (!ValidationUtil.isPositiveDouble(volumeStr)) {
            setError("Volume harus angka positif.");
            return;
        }
        if (!ValidationUtil.isPositiveDouble(timeStr)) {
            setError("Waktu harus angka positif.");
            return;
        }

        double bwi = Double.parseDouble(bwiStr);
        double lrf = Double.parseDouble(lrfStr);
        double volume = Double.parseDouble(volumeStr);
        double time = Double.parseDouble(timeStr);

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            setError("Session tidak ditemukan. Silakan login ulang.");
            return;
        }

        lblStatus.setText("Menghitung...");
        lblStatus.setForeground(CYAN);

        try {
            Map<String, Object> result = footprintService.distributeCredit(bwi, lrf, volume, time, user);

            double bwf = (double) result.get("bwf");
            double avg = (double) result.get("averageCompanyCredit");
            String region = (String) result.get("regionName");

            StringBuilder sb = new StringBuilder();
            sb.append("=== HASIL DISTRIBUSI ===\n");
            sb.append("Region   : ").append(region).append("\n");
            sb.append(String.format("BWF Total: %.6f m³/tahun%n", bwf));
            sb.append(String.format("Rata-rata Credit Company: %.6f%n%n", avg));

            sb.append("--- PERUSAHAAN ---\n");
            List<Map<String, Object>> companies = (List<Map<String, Object>>) result.get("companies");
            for (Map<String, Object> c : companies) {
                sb.append(String.format("%-20s | η=%.2f | Sl=%.2f | Credit=%.4f%n",
                        c.get("companyName"), c.get("eta"), c.get("sl"), c.get("credit")));
            }

            sb.append("\n--- INDIVIDUAL ---\n");
            List<Map<String, Object>> individuals = (List<Map<String, Object>>) result.get("individuals");
            if (individuals.isEmpty())
                sb.append("(Tidak ada individual approved di region ini)\n");
            for (Map<String, Object> i : individuals) {
                sb.append(String.format("%-20s | Credit=%.4f%n", i.get("name"), i.get("credit")));
            }

            taResult.setText(sb.toString());
            lblStatus.setText("✅ Distribusi berhasil!");
            lblStatus.setForeground(GREEN);
        } catch (Exception ex) {
            setError(ex.getMessage());
            taResult.setText("❌ Gagal: " + ex.getMessage());
        }
    }

    private void setError(String msg) {
        lblStatus.setText("❌ " + msg);
        lblStatus.setForeground(new Color(239, 68, 68));
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
        tf.setBorder(new CompoundBorder(new LineBorder(new Color(51, 65, 85), 1, true), new EmptyBorder(8, 12, 8, 12)));
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
}
