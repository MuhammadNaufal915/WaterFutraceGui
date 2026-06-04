package gui;

import model.ERole;
import model.User;
import utils.SessionManager;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * MainFrame - Jendela utama aplikasi setelah login.
 * Memiliki sidebar navigasi di kiri dan panel konten (CardLayout) di kanan.
 */
public class MainFrame extends JFrame {

    private static final Color BG_DARK     = new Color(10, 14, 26);
    private static final Color SIDEBAR_BG  = new Color(15, 23, 50);
    private static final Color ACCENT_CYAN = new Color(0, 212, 255);
    private static final Color TEXT_WHITE  = new Color(248, 250, 252);
    private static final Color TEXT_MUTED  = new Color(100, 116, 139);
    private static final Color SELECTED_BG = new Color(0, 212, 255, 40);
    private static final Color HOVER_BG    = new Color(255, 255, 255, 12);
    private static final Font  FONT_NAV    = new Font("Segoe UI", Font.BOLD, 13);

    private JPanel        contentPanel;
    private CardLayout    cardLayout;
    private JButton       selectedBtn;

    // Panels (loaded lazily)
    private DashboardPanel      dashboardPanel;
    private UserManagementPanel userMgmtPanel;
    private GovernmentPanel     govPanel;
    private CompanyPanel        companyPanel;
    private ProductPanel        productPanel;
    private TransactionPanel    transactionPanel;
    private ComplainPanel       complainPanel;

    public MainFrame() {
        User user = SessionManager.getInstance().getCurrentUser();
        setTitle("WaterFutrace – " + (user != null ? user.getName() + " [" + user.getRole() + "]" : ""));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 720);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);
        setContentPane(root);

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContentArea(), BorderLayout.CENTER);

        // Default panel
        showPanel("dashboard");
    }

    // ─── Sidebar ─────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, new Color(30, 41, 59)));

        // Logo
        sidebar.add(Box.createVerticalStrut(24));
        JLabel logo = new JLabel("💧 WaterFutrace", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logo.setForeground(ACCENT_CYAN);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        logo.setMaximumSize(new Dimension(220, 36));
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(8));

        // User info
        User user = SessionManager.getInstance().getCurrentUser();
        JLabel lblUser = new JLabel(user != null ? user.getName() : "Guest", SwingConstants.CENTER);
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblUser.setForeground(TEXT_MUTED);
        lblUser.setAlignmentX(CENTER_ALIGNMENT);
        lblUser.setMaximumSize(new Dimension(220, 24));
        sidebar.add(lblUser);
        sidebar.add(Box.createVerticalStrut(20));

        // Divider
        sidebar.add(divider());
        sidebar.add(Box.createVerticalStrut(12));

        // Navigation buttons berdasarkan role
        ERole role = user != null ? user.getRole() : null;

        sidebar.add(navBtn("🏠  Dashboard", "dashboard"));

        if (role == ERole.GOVERMENT) {
            sidebar.add(navBtn("📋  Kelola Pengguna", "usermgmt"));
            sidebar.add(navBtn("🌊  Distribusi BWF", "government"));
            sidebar.add(navBtn("📢  Pengaduan Masuk", "complain"));
        }
        if (role == ERole.COMPANY) {
            sidebar.add(navBtn("⚙️  Parameter Perusahaan", "company"));
            sidebar.add(navBtn("📦  Kelola Produk", "product"));
            sidebar.add(navBtn("📢  Kirim Pengaduan", "complain"));
        }
        if (role == ERole.INDIVIDUAL) {
            sidebar.add(navBtn("🛒  Beli Produk", "product"));
            sidebar.add(navBtn("📜  Riwayat Transaksi", "transaction"));
            sidebar.add(navBtn("📢  Kirim Pengaduan", "complain"));
        }

        sidebar.add(Box.createVerticalGlue());

        // Logout button
        sidebar.add(divider());
        sidebar.add(Box.createVerticalStrut(8));
        JButton btnLogout = navBtn("🚪  Keluar", "logout");
        btnLogout.setForeground(new Color(239, 68, 68));
        sidebar.add(btnLogout);
        sidebar.add(Box.createVerticalStrut(16));

        return sidebar;
    }

    private JButton navBtn(String text, String card) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                if (this == selectedBtn) {
                    g.setColor(SELECTED_BG);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_NAV);
        btn.setForeground(TEXT_WHITE);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(220, 42));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.setBorder(new EmptyBorder(0, 20, 0, 12));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn != selectedBtn) btn.setBackground(HOVER_BG);
            }
            @Override public void mouseExited(MouseEvent e) {
                if (btn != selectedBtn) btn.setBackground(new Color(0, 0, 0, 0));
            }
        });

        btn.addActionListener(e -> {
            if ("logout".equals(card)) {
                doLogout();
            } else {
                selectNav(btn);
                showPanel(card);
            }
        });
        return btn;
    }

    private JSeparator divider() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(new Color(30, 41, 59));
        sep.setBackground(new Color(30, 41, 59));
        sep.setMaximumSize(new Dimension(200, 1));
        sep.setAlignmentX(CENTER_ALIGNMENT);
        return sep;
    }

    private void selectNav(JButton btn) {
        selectedBtn = btn;
        repaint();
    }

    // ─── Content Area ────────────────────────────────────────────

    private JPanel buildContentArea() {
        User user = SessionManager.getInstance().getCurrentUser();

        // Header bar
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 50));
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, new Color(30, 41, 59)),
            new EmptyBorder(12, 24, 12, 24)
        ));
        header.setPreferredSize(new Dimension(0, 56));

        JLabel lblTitle = new JLabel("WaterFutrace Desktop");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(TEXT_WHITE);
        header.add(lblTitle, BorderLayout.WEST);

        JLabel lblRole = new JLabel(user != null ?
            user.getRole().name() + " · " + (user.isApproved() ? "✅ Aktif" : "⏳ Pending")
            : "");
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblRole.setForeground(TEXT_MUTED);
        header.add(lblRole, BorderLayout.EAST);

        // Content panels
        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG_DARK);

        dashboardPanel   = new DashboardPanel();
        userMgmtPanel    = new UserManagementPanel();
        govPanel         = new GovernmentPanel();
        companyPanel     = new CompanyPanel();
        productPanel     = new ProductPanel();
        transactionPanel = new TransactionPanel();
        complainPanel    = new ComplainPanel();

        contentPanel.add(dashboardPanel,   "dashboard");
        contentPanel.add(userMgmtPanel,    "usermgmt");
        contentPanel.add(govPanel,         "government");
        contentPanel.add(companyPanel,     "company");
        contentPanel.add(productPanel,     "product");
        contentPanel.add(transactionPanel, "transaction");
        contentPanel.add(complainPanel,    "complain");

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(contentPanel, BorderLayout.CENTER);
        return wrapper;
    }

    public void showPanel(String name) {
        // Refresh data saat panel dibuka
        switch (name) {
            case "dashboard"   -> dashboardPanel.refresh();
            case "usermgmt"    -> userMgmtPanel.refresh();
            case "government"  -> govPanel.refresh();
            case "company"     -> companyPanel.refresh();
            case "product"     -> productPanel.refresh();
            case "transaction" -> transactionPanel.refresh();
            case "complain"    -> complainPanel.refresh();
        }
        cardLayout.show(contentPanel, name);
    }

    private void doLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Apakah Anda yakin ingin keluar?", "Konfirmasi Logout",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            SessionManager.getInstance().logout();
            dispose();
            new LoginFrame();
        }
    }
}
