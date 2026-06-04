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
 * LoginFrame - Halaman login utama dengan desain modern gelap.
 * Entry point pertama yang dilihat user.
 */
public class LoginFrame extends JFrame {

    // ── Palette ───────────────────────────────────────────────────
    private static final Color BG_DARK      = new Color(10, 14, 26);
    private static final Color BG_CARD      = new Color(18, 28, 50);
    private static final Color ACCENT_CYAN  = new Color(0, 212, 255);
    private static final Color ACCENT_BLUE  = new Color(37, 99, 235);
    private static final Color TEXT_WHITE   = new Color(248, 250, 252);
    private static final Color TEXT_MUTED   = new Color(148, 163, 184);
    private static final Color INPUT_BG     = new Color(30, 44, 70);
    private static final Color INPUT_BORDER = new Color(51, 71, 110);
    private static final Color BTN_HOVER    = new Color(14, 165, 233);
    private static final Color ERROR_RED    = new Color(239, 68, 68);
    // ─────────────────────────────────────────────────────────────

    private final AuthService authService = new AuthService();

    private JTextField    tfEmail;
    private JPasswordField pfPassword;
    private JLabel        lblStatus;

    public LoginFrame() {
        setTitle("WaterFutrace – Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(440, 580);
        setLocationRelativeTo(null);
        setResizable(false);
        initUI();
        setVisible(true);
    }

    private void initUI() {
        // Root gradient panel
        JPanel root = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, BG_DARK, getWidth(), getHeight(),
                    new Color(5, 10, 35));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        setContentPane(root);

        // Card panel
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(36, 40, 36, 40));
        card.setPreferredSize(new Dimension(360, 500));

        // ── Logo / Brand ──
        JLabel lblDrop = new JLabel("💧");
        lblDrop.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblDrop.setAlignmentX(CENTER_ALIGNMENT);
        card.add(lblDrop);
        card.add(Box.createVerticalStrut(8));

        JLabel lblBrand = new JLabel("WaterFutrace");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblBrand.setForeground(ACCENT_CYAN);
        lblBrand.setAlignmentX(CENTER_ALIGNMENT);
        card.add(lblBrand);

        JLabel lblSub = new JLabel("Water Credit Management System");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_MUTED);
        lblSub.setAlignmentX(CENTER_ALIGNMENT);
        card.add(lblSub);
        card.add(Box.createVerticalStrut(28));

        // ── Email ──
        card.add(fieldLabel("Email"));
        card.add(Box.createVerticalStrut(4));
        tfEmail = styledTextField("email@contoh.com");
        card.add(tfEmail);
        card.add(Box.createVerticalStrut(14));

        // ── Password ──
        card.add(fieldLabel("Password"));
        card.add(Box.createVerticalStrut(4));
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
        JButton btnLogin = gradientButton("Masuk", ACCENT_BLUE, BTN_HOVER);
        btnLogin.addActionListener(e -> doLogin());
        card.add(btnLogin);
        card.add(Box.createVerticalStrut(12));

        // ── Separator ──
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(51, 71, 110));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep);
        card.add(Box.createVerticalStrut(12));

        // ── Register Button ──
        JButton btnReg = outlineButton("Belum punya akun? Daftar");
        btnReg.addActionListener(e -> {
            RegisterFrame rf = new RegisterFrame(this);
            rf.setVisible(true);
        });
        card.add(btnReg);

        // Enter key on password -> login
        pfPassword.addActionListener(e -> doLogin());
        tfEmail.addActionListener(e -> pfPassword.requestFocus());

        root.add(card);
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
        lblStatus.setForeground(new Color(100, 200, 255));

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
        tf.setForeground(TEXT_WHITE);
        tf.setBackground(INPUT_BG);
        tf.setCaretColor(ACCENT_CYAN);
        tf.setBorder(new CompoundBorder(
            new LineBorder(INPUT_BORDER, 1, true),
            new EmptyBorder(10, 14, 10, 14)
        ));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        tf.setAlignmentX(LEFT_ALIGNMENT);
        tf.setToolTipText(placeholder);
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                tf.setBorder(new CompoundBorder(new LineBorder(ACCENT_CYAN, 1, true), new EmptyBorder(10,14,10,14)));
            }
            @Override public void focusLost(FocusEvent e) {
                tf.setBorder(new CompoundBorder(new LineBorder(INPUT_BORDER, 1, true), new EmptyBorder(10,14,10,14)));
            }
        });
        return tf;
    }

    private JPasswordField styledPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        pf.setForeground(TEXT_WHITE);
        pf.setBackground(INPUT_BG);
        pf.setCaretColor(ACCENT_CYAN);
        pf.setBorder(new CompoundBorder(
            new LineBorder(INPUT_BORDER, 1, true),
            new EmptyBorder(10, 14, 10, 14)
        ));
        pf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        pf.setAlignmentX(LEFT_ALIGNMENT);
        pf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                pf.setBorder(new CompoundBorder(new LineBorder(ACCENT_CYAN, 1, true), new EmptyBorder(10,14,10,14)));
            }
            @Override public void focusLost(FocusEvent e) {
                pf.setBorder(new CompoundBorder(new LineBorder(INPUT_BORDER, 1, true), new EmptyBorder(10,14,10,14)));
            }
        });
        return pf;
    }

    private JButton gradientButton(String text, Color c1, Color c2) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), 0, c2);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        return btn;
    }

    private JButton outlineButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(ACCENT_CYAN);
        btn.setBackground(new Color(0, 212, 255, 20));
        btn.setBorder(new LineBorder(ACCENT_CYAN, 1, true));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0, 212, 255, 40)); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(new Color(0, 212, 255, 20)); }
        });
        return btn;
    }
}
