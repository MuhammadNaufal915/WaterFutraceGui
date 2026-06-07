package gui;

import service.AuthService;
import utils.SessionManager;
import utils.ValidationUtil;
import model.User;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * LoginFrame - Halaman utama (Welcome Screen & Login) dengan desain modern.
 * Memiliki top header, left gradient info panel, dan right dynamic card (Home News / Login Form).
 */
public class LoginFrame extends JFrame {

    // ── Palette ───────────────────────────────────────────────────
    private static final Color HEADER_BG     = new Color(11, 26, 48); // Dark Navy
    private static final Color CONTENT_BG    = new Color(238, 242, 247); // Light Grayish Blue
    private static final Color CARD_BG       = new Color(255, 255, 255); // White
    private static final Color ACCENT_BLUE   = new Color(37, 99, 235); // Royal Blue
    private static final Color ACCENT_CYAN   = new Color(0, 212, 255);
    private static final Color TEXT_DARK     = new Color(15, 23, 42); // Slate 900
    private static final Color TEXT_MUTED    = new Color(100, 116, 139); // Slate 500
    private static final Color INPUT_BG      = new Color(248, 250, 252);
    private static final Color INPUT_BORDER  = new Color(226, 232, 240);
    private static final Color BTN_HOVER     = new Color(29, 78, 216);
    private static final Color ERROR_RED     = new Color(239, 68, 68);
    // ─────────────────────────────────────────────────────────────

    private final AuthService authService = new AuthService();

    private CardLayout rightCardLayout;
    private JPanel rightCardPanel;
    private JButton btnTabHome, btnTabLogin;

    // Login Form Fields
    private JTextField tfEmail;
    private JPasswordField pfPassword;
    private JLabel lblStatus;

    public LoginFrame() {
        setTitle("WaterFutrace - Smart Water Monitoring");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);
        setResizable(false);
        initUI();
        setVisible(true);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(CONTENT_BG);
        setContentPane(root);

        // 1. TOP HEADER
        root.add(buildHeader(), BorderLayout.NORTH);

        // 2. MAIN SPLIT AREA (Left Panel + Right Panel CardLayout)
        JPanel mainArea = new JPanel(new GridBagLayout());
        mainArea.setBackground(CONTENT_BG);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Left Panel (Fixed Branding Gradient)
        gbc.gridx = 0;
        gbc.weightx = 0.46; // ~46% width
        mainArea.add(buildLeftPanel(), gbc);

        // Right Panel (Dynamic CardLayout)
        gbc.gridx = 1;
        gbc.weightx = 0.54; // ~54% width
        rightCardLayout = new CardLayout();
        rightCardPanel = new JPanel(rightCardLayout);
        rightCardPanel.setBackground(CONTENT_BG);

        rightCardPanel.add(buildHomeNewsPanel(), "home");
        rightCardPanel.add(buildLoginFormPanel(), "login");

        mainArea.add(rightCardPanel, gbc);

        root.add(mainArea, BorderLayout.CENTER);
        
        // Show Home page initially
        switchTab("home");
    }

    // ─── Header Builder ──────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(0, 60));
        header.setBorder(new EmptyBorder(0, 32, 0, 32));

        // Brand Logo
        JLabel logo = new JLabel("💧 WaterFutrace");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logo.setForeground(Color.WHITE);
        header.add(logo, BorderLayout.WEST);

        // Navigation Tabs
        JPanel navTabs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        navTabs.setOpaque(false);

        btnTabHome = navTabButton("Home");
        btnTabHome.addActionListener(e -> switchTab("home"));
        navTabs.add(btnTabHome);

        btnTabLogin = navTabButton("Login");
        btnTabLogin.addActionListener(e -> switchTab("login"));
        navTabs.add(btnTabLogin);

        header.add(navTabs, BorderLayout.EAST);
        return header;
    }

    private JButton navTabButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(new Color(200, 210, 230));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void switchTab(String name) {
        rightCardLayout.show(rightCardPanel, name);
        if ("home".equals(name)) {
            btnTabHome.setForeground(Color.WHITE);
            btnTabLogin.setForeground(new Color(148, 163, 184));
        } else {
            btnTabHome.setForeground(new Color(148, 163, 184));
            btnTabLogin.setForeground(Color.WHITE);
        }
    }

    // ─── Left Gradient Panel ─────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(11, 26, 48), getWidth(), getHeight(),
                        new Color(26, 72, 148));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(48, 48, 48, 48));

        // 1. Badge Platform
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setMaximumSize(new Dimension(200, 24));
        badge.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lblBadge = new JLabel("💧 Water Monitoring Platform");
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblBadge.setForeground(ACCENT_CYAN);
        badge.add(lblBadge);
        panel.add(badge);
        panel.add(Box.createVerticalStrut(28));

        // 2. Title
        JLabel title1 = new JLabel("WaterFutrace");
        title1.setFont(new Font("Segoe UI", Font.BOLD, 42));
        title1.setForeground(Color.WHITE);
        title1.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(title1);

        JLabel title2 = new JLabel("for Future");
        title2.setFont(new Font("Segoe UI", Font.BOLD, 42));
        title2.setForeground(ACCENT_CYAN);
        title2.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(title2);
        panel.add(Box.createVerticalStrut(20));

        // 3. Subtitle
        JLabel subtitle = new JLabel("<html>Platform pemantauan jejak air cerdas<br>untuk masa depan yang berkelanjutan.</html>");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(new Color(210, 220, 240));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(32));

        // 4. Buttons Row
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        pnlBtns.setOpaque(false);
        pnlBtns.setAlignmentX(LEFT_ALIGNMENT);

        JButton btnStart = roundedButton("Mulai Sekarang →", ACCENT_BLUE, Color.WHITE);
        btnStart.addActionListener(e -> switchTab("login"));
        pnlBtns.add(btnStart);

        JButton btnDoc = outlineButton("Documentation");
        btnDoc.addActionListener(e -> JOptionPane.showMessageDialog(this, 
                "Documentation link: https://github.com/MuhammadNaufal915/WaterFutraceGui", 
                "Dokumentasi", JOptionPane.INFORMATION_MESSAGE));
        pnlBtns.add(btnDoc);
        panel.add(pnlBtns);

        panel.add(Box.createVerticalGlue());

        // 5. Stats Row
        JPanel pnlStats = new JPanel(new GridLayout(1, 3, 20, 0));
        pnlStats.setOpaque(false);
        pnlStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        pnlStats.setAlignmentX(LEFT_ALIGNMENT);

        pnlStats.add(statItem("3", "Tipe Pengguna"));
        pnlStats.add(statItem("0", "Smart Monitoring"));
        pnlStats.add(statItem("0", "Eco Friendly"));

        panel.add(pnlStats);

        return panel;
    }

    private JPanel statItem(String value, String label) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                // Drawing thin vertical divider line on the left except for first item
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(LEFT_ALIGNMENT);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valLbl.setForeground(Color.WHITE);
        p.add(valLbl);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(170, 190, 220));
        p.add(lbl);

        return p;
    }

    // ─── Right Panel - Card 1: Home News ──────────────────────────
    private JPanel buildHomeNewsPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CONTENT_BG);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(36, 40, 36, 40));

        JLabel lblNewsTitle = new JLabel("Berita Terkini");
        lblNewsTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblNewsTitle.setForeground(TEXT_DARK);
        lblNewsTitle.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(lblNewsTitle);
        panel.add(Box.createVerticalStrut(20));

        // News 1
        panel.add(buildNewsCard("Lingkungan", new Color(16, 185, 129), 
                "Menuju Indonesia Sejahtera", "Menjaga ekosistem air nasional", 
                "10 Maret 2026", "Konservasi",
                "Pemerintah meluncurkan inisiatif nasional konservasi air daerah aliran sungai secara terpadu."));

        panel.add(Box.createVerticalStrut(16));

        // News 2
        panel.add(buildNewsCard("Global", new Color(249, 115, 22), 
                "Krisis Air Global Akibat AI", "Pendingin server menghabiskan jutaan liter", 
                "5 April 2026", "Teknologi",
                "Riset terbaru menunjukkan data center kecerdasan buatan menyedot jutaan liter air untuk menjaga suhu server."));

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildNewsCard(String badgeText, Color badgeColor, String title, String desc, 
                                 String date, String cat, String fullContent) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 20, 16, 20));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 165));
        card.setAlignmentX(LEFT_ALIGNMENT);

        // Top Content
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        // Badge
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(badgeColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setAlignmentX(LEFT_ALIGNMENT);
        JLabel badgeLbl = new JLabel(badgeText);
        badgeLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badgeLbl.setForeground(Color.WHITE);
        badge.add(badgeLbl);
        top.add(badge);
        top.add(Box.createVerticalStrut(8));

        // Title
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(TEXT_DARK);
        top.add(titleLbl);
        top.add(Box.createVerticalStrut(4));

        // Description
        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLbl.setForeground(TEXT_MUTED);
        top.add(descLbl);

        card.add(top, BorderLayout.CENTER);

        // Bottom Info Row
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(12, 0, 0, 0));

        JLabel infoLbl = new JLabel("📅 " + date + "  •  🏷️ Kategori: " + cat);
        infoLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoLbl.setForeground(TEXT_MUTED);
        bottom.add(infoLbl, BorderLayout.WEST);

        JButton btnDetail = roundedButton("Lihat Detail →", ACCENT_BLUE, Color.WHITE);
        btnDetail.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDetail.setMargin(new Insets(4, 12, 4, 12));
        btnDetail.addActionListener(e -> JOptionPane.showMessageDialog(this, fullContent, title, JOptionPane.INFORMATION_MESSAGE));
        bottom.add(btnDetail, BorderLayout.EAST);

        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    // ─── Right Panel - Card 2: Login Form Panel ────────────────────
    private JPanel buildLoginFormPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(CONTENT_BG);
        wrapper.setBorder(new EmptyBorder(36, 40, 36, 40));

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(32, 36, 32, 36));
        card.setPreferredSize(new Dimension(420, 500));

        // ── Form Header ──
        JLabel lblFormTitle = new JLabel("Masuk Ke Akun");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblFormTitle.setForeground(TEXT_DARK);
        lblFormTitle.setAlignmentX(CENTER_ALIGNMENT);
        card.add(lblFormTitle);

        JLabel lblSub = new JLabel("Silakan masukkan email & password Anda");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_MUTED);
        lblSub.setAlignmentX(CENTER_ALIGNMENT);
        card.add(Box.createVerticalStrut(4));
        card.add(lblSub);
        card.add(Box.createVerticalStrut(28));

        // ── Email ──
        card.add(fieldLabel("Email"));
        card.add(Box.createVerticalStrut(6));
        tfEmail = styledTextField("email@contoh.com");
        card.add(tfEmail);
        card.add(Box.createVerticalStrut(16));

        // ── Password ──
        card.add(fieldLabel("Password"));
        card.add(Box.createVerticalStrut(6));
        pfPassword = styledPasswordField();
        card.add(pfPassword);
        card.add(Box.createVerticalStrut(6));

        // ── Status / Error ──
        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(ERROR_RED);
        lblStatus.setAlignmentX(CENTER_ALIGNMENT);
        card.add(lblStatus);
        card.add(Box.createVerticalStrut(14));

        // ── Login Button ──
        JButton btnLogin = roundedButton("Masuk", ACCENT_BLUE, Color.WHITE);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnLogin.addActionListener(e -> doLogin());
        card.add(btnLogin);
        card.add(Box.createVerticalStrut(16));

        // ── Separator ──
        JSeparator sep = new JSeparator();
        sep.setForeground(INPUT_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep);
        card.add(Box.createVerticalStrut(16));

        // ── Register Button ──
        JButton btnReg = outlineButtonDark("Belum punya akun? Daftar");
        btnReg.addActionListener(e -> {
            RegisterFrame rf = new RegisterFrame(this);
            rf.setVisible(true);
        });
        card.add(btnReg);

        // Enter key action
        pfPassword.addActionListener(e -> doLogin());
        tfEmail.addActionListener(e -> pfPassword.requestFocus());

        wrapper.add(card);
        return wrapper;
    }

    private void doLogin() {
        String email    = tfEmail.getText().trim();
        String password = new String(pfPassword.getPassword());

        if (!ValidationUtil.isValidEmail(email)) {
            lblStatus.setText("Format email tidak valid.");
            return;
        }
        if (ValidationUtil.isEmpty(password)) {
            lblStatus.setText("Password wajib diisi.");
            return;
        }

        lblStatus.setText("Sedang masuk...");
        lblStatus.setForeground(ACCENT_BLUE);

        SwingUtilities.invokeLater(() -> {
            try {
                User user = authService.login(email, password);
                SessionManager.getInstance().setCurrentUser(user);
                dispose();
                new MainFrame().setVisible(true);
            } catch (Exception ex) {
                lblStatus.setText("<html><center>" + ex.getMessage() + "</center></html>");
                lblStatus.setForeground(ERROR_RED);
                pfPassword.setText("");
            }
        });
    }

    // ── UI Helpers ──────────────────────────────────────────────

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JTextField styledTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setForeground(TEXT_DARK);
        tf.setBackground(INPUT_BG);
        tf.setCaretColor(ACCENT_BLUE);
        tf.setBorder(new CompoundBorder(
            new LineBorder(INPUT_BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tf.setAlignmentX(LEFT_ALIGNMENT);
        tf.setToolTipText(placeholder);
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                tf.setBorder(new CompoundBorder(new LineBorder(ACCENT_BLUE, 1, true), new EmptyBorder(8,12,8,12)));
            }
            @Override public void focusLost(FocusEvent e) {
                tf.setBorder(new CompoundBorder(new LineBorder(INPUT_BORDER, 1, true), new EmptyBorder(8,12,8,12)));
            }
        });
        return tf;
    }

    private JPasswordField styledPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        pf.setForeground(TEXT_DARK);
        pf.setBackground(INPUT_BG);
        pf.setCaretColor(ACCENT_BLUE);
        pf.setBorder(new CompoundBorder(
            new LineBorder(INPUT_BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        pf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        pf.setAlignmentX(LEFT_ALIGNMENT);
        pf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                pf.setBorder(new CompoundBorder(new LineBorder(ACCENT_BLUE, 1, true), new EmptyBorder(8,12,8,12)));
            }
            @Override public void focusLost(FocusEvent e) {
                pf.setBorder(new CompoundBorder(new LineBorder(INPUT_BORDER, 1, true), new EmptyBorder(8,12,8,12)));
            }
        });
        return pf;
    }

    private JButton roundedButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        return btn;
    }

    private JButton outlineButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(Color.WHITE);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        return btn;
    }

    private JButton outlineButtonDark(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(37, 99, 235, 10));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(ACCENT_BLUE);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(ACCENT_BLUE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        return btn;
    }
}
