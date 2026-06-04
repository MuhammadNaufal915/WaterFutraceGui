package gui;

import dao.CompanyDAO;
import dao.GovernmentDAO;
import dao.IndividualDAO;
import model.*;
import utils.SessionManager;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.Optional;

/**
 * DashboardPanel - Panel statistik utama sesuai role user yang login.
 */
public class DashboardPanel extends JPanel {

    private static final Color BG         = new Color(10, 14, 26);
    private static final Color CARD_BG    = new Color(18, 28, 50);
    private static final Color CYAN       = new Color(0, 212, 255);
    private static final Color GREEN      = new Color(16, 185, 129);
    private static final Color AMBER      = new Color(245, 158, 11);
    private static final Color PURPLE     = new Color(139, 92, 246);
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    private JPanel statsRow;
    private JLabel lblWelcome, lblSub;

    private final CompanyDAO    companyDAO    = new CompanyDAO();
    private final IndividualDAO individualDAO = new IndividualDAO();
    private final GovernmentDAO governmentDAO = new GovernmentDAO();

    public DashboardPanel() {
        setBackground(BG);
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // Top section
        JPanel top = new JPanel();
        top.setBackground(BG);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(32, 32, 16, 32));

        lblWelcome = new JLabel("Selamat Datang 👋");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblWelcome.setForeground(TEXT_WHITE);
        top.add(lblWelcome);

        lblSub = new JLabel("Memuat data...");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSub.setForeground(TEXT_MUTED);
        top.add(Box.createVerticalStrut(6));
        top.add(lblSub);

        add(top, BorderLayout.NORTH);

        // Stats row
        statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        statsRow.setBackground(BG);
        statsRow.setBorder(new EmptyBorder(0, 20, 0, 20));

        JScrollPane scroll = new JScrollPane(statsRow);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(BG);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
    }

    public void refresh() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        lblWelcome.setText("Selamat Datang, " + user.getName() + " 👋");
        lblSub.setText("Role: " + user.getRole().name() + "  ·  Status: "
            + (user.isApproved() ? "✅ Aktif" : "⏳ Menunggu Persetujuan"));
        lblSub.setForeground(user.isApproved() ? GREEN : AMBER);

        statsRow.removeAll();

        switch (user.getRole()) {
            case GOVERMENT -> buildGovStats(user);
            case COMPANY   -> buildCompanyStats(user);
            case INDIVIDUAL -> buildIndividualStats(user);
        }

        statsRow.revalidate();
        statsRow.repaint();
    }

    private void buildGovStats(User user) {
        Optional<model.Government> govOpt = governmentDAO.findByUser(user);
        govOpt.ifPresentOrElse(gov -> {
            Region region = gov.getRegion();
            String regionName = region != null ? region.getName() : "Belum diset";

            long totalCompanies = region != null ?
                companyDAO.findByRegion(region).stream().filter(c -> c.getUser() != null && c.getUser().isApproved()).count() : 0;
            long totalIndividuals = region != null ?
                individualDAO.findByRegion(region).stream().filter(i -> i.getUser() != null && i.getUser().isApproved()).count() : 0;
            long totalPending = region != null ? (
                companyDAO.findByRegion(region).stream().filter(c -> c.getUser() != null && !c.getUser().isApproved()).count()
                + individualDAO.findByRegion(region).stream().filter(i -> i.getUser() != null && !i.getUser().isApproved()).count()
            ) : 0;
            float bwf = (gov.getWaterFootprint() != null) ? gov.getWaterFootprint().getCalculateFootprint() : 0f;

            statsRow.add(statCard("🗺️ Region",        regionName,                    CYAN));
            statsRow.add(statCard("🏭 Perusahaan Aktif", String.valueOf(totalCompanies), GREEN));
            statsRow.add(statCard("👤 Individual Aktif", String.valueOf(totalIndividuals), PURPLE));
            statsRow.add(statCard("⏳ Menunggu Approval", String.valueOf(totalPending),   AMBER));
            statsRow.add(statCard("🌊 BWF Terkini",    String.format("%.4f m³/tahun", bwf), CYAN));
        }, () -> statsRow.add(noDataCard("Data Government belum tersedia.")));
    }

    private void buildCompanyStats(User user) {
        companyDAO.findByUser(user).ifPresentOrElse(c -> {
            double credit = c.getWatercredit() != null ? c.getWatercredit() : 0.0;
            String region = c.getRegion() != null ? c.getRegion().getName() : "Belum diset";
            String sector = c.getSector() != null ? c.getSector() : "-";
            String eta    = c.getEta() != null ? String.format("%.2f", c.getEta()) : "Belum diset";
            String sl     = c.getSl()  != null ? String.format("%.2f", c.getSl())  : "Belum diset";

            statsRow.add(statCard("🌊 Water Credit",   String.format("%.4f", credit), CYAN));
            statsRow.add(statCard("🗺️ Region",          region,                       GREEN));
            statsRow.add(statCard("🏭 Sektor",          sector,                       PURPLE));
            statsRow.add(statCard("η Efisiensi (Eta)",  eta,                          AMBER));
            statsRow.add(statCard("Sl Kelangkaan Air",  sl,                           new Color(239,68,68)));
        }, () -> statsRow.add(noDataCard("Profil perusahaan belum tersedia.")));
    }

    private void buildIndividualStats(User user) {
        individualDAO.findByUser(user).ifPresentOrElse(ind -> {
            double credit = ind.getWaterCredit() != null ? ind.getWaterCredit() : 0.0;
            String region = ind.getRegion() != null ? ind.getRegion().getName() : "Belum diset";
            String job    = ind.getPekerjaan() != null ? ind.getPekerjaan() : "-";

            statsRow.add(statCard("💧 Water Credit",    String.format("%.4f", credit), CYAN));
            statsRow.add(statCard("🗺️ Region",           region,                       GREEN));
            statsRow.add(statCard("💼 Pekerjaan",        job,                          PURPLE));
            statsRow.add(statCard("🎂 Usia",             ind.getAge() + " tahun",      AMBER));
        }, () -> statsRow.add(noDataCard("Profil individual belum tersedia.")));
    }

    private JPanel statCard(String title, String value, Color accent) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Left accent bar
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
            }
        };
        card.setBackground(CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 20, 20, 24));
        card.setPreferredSize(new Dimension(210, 110));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTitle.setForeground(TEXT_MUTED);
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(10));

        JLabel lblVal = new JLabel("<html>" + value + "</html>");
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblVal.setForeground(accent);
        card.add(lblVal);

        return card;
    }

    private JLabel noDataCard(String msg) {
        JLabel l = new JLabel(msg);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setForeground(TEXT_MUTED);
        return l;
    }
}
