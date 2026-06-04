package gui;

import dao.GovernmentDAO;
import model.*;
import service.AuthService;
import utils.SessionManager;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * UserManagementPanel - Government melihat dan menyetujui pending users.
 */
public class UserManagementPanel extends JPanel {

    private static final Color BG         = new Color(10, 14, 26);
    private static final Color CARD_BG    = new Color(18, 28, 50);
    private static final Color CYAN       = new Color(0, 212, 255);
    private static final Color GREEN      = new Color(16, 185, 129);
    private static final Color AMBER      = new Color(245, 158, 11);
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    private final AuthService   authService = new AuthService();
    private final GovernmentDAO govDAO      = new GovernmentDAO();

    private JTable   table;
    private DefaultTableModel model;
    private JLabel   lblStatus;

    public UserManagementPanel() {
        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        initUI();
    }

    private void initUI() {
        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BG);
        hdr.setBorder(new EmptyBorder(24, 32, 12, 32));
        JLabel title = new JLabel("📋  Manajemen Pengguna");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);
        hdr.add(title, BorderLayout.WEST);
        JButton btnRefresh = actionButton("🔄 Refresh", CYAN);
        btnRefresh.addActionListener(e -> refresh());
        hdr.add(btnRefresh, BorderLayout.EAST);
        add(hdr, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID User", "Nama", "Email", "Role", "Region/Sektor", "Status"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(CARD_BG);
        scroll.getViewport().setBackground(CARD_BG);
        scroll.setBorder(new EmptyBorder(0, 32, 0, 32));
        add(scroll, BorderLayout.CENTER);

        // Bottom bar
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        bottom.setBackground(BG);
        bottom.setBorder(new EmptyBorder(0, 24, 16, 24));

        JButton btnApprove = actionButton("✅  Setujui User", GREEN);
        btnApprove.addActionListener(e -> approveSelected());
        bottom.add(btnApprove);

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblStatus.setForeground(TEXT_MUTED);
        bottom.add(lblStatus);
        add(bottom, BorderLayout.SOUTH);
    }

    public void refresh() {
        model.setRowCount(0);
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Optional<model.Government> govOpt = govDAO.findByUser(currentUser);
        if (govOpt.isEmpty()) {
            lblStatus.setText("Profil Government tidak ditemukan.");
            return;
        }

        Region region = govOpt.get().getRegion();
        if (region == null) {
            lblStatus.setText("Region Government belum diset.");
            return;
        }

        List<User> pending = authService.getPendingUsersInRegion(region);
        lblStatus.setText("Ditemukan " + pending.size() + " user menunggu persetujuan di region " + region.getName());

        for (User u : pending) {
            model.addRow(new Object[]{
                u.getIdUser(),
                u.getName(),
                u.getEmail(),
                u.getRole().name(),
                "-",
                u.isApproved() ? "✅ Aktif" : "⏳ Pending"
            });
        }
    }

    private void approveSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih user terlebih dahulu.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        long userId = (long) model.getValueAt(row, 0);
        String name = (String) model.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Setujui akun " + name + " (" + userId + ")?",
            "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            authService.approveUser(userId);
            lblStatus.setText("✅ Akun " + name + " berhasil disetujui.");
            lblStatus.setForeground(GREEN);
            refresh();
        } catch (Exception ex) {
            lblStatus.setText("❌ " + ex.getMessage());
            lblStatus.setForeground(new Color(239, 68, 68));
        }
    }

    private void styleTable(JTable t) {
        t.setBackground(CARD_BG);
        t.setForeground(TEXT_WHITE);
        t.setGridColor(new Color(30, 41, 59));
        t.setRowHeight(36);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setSelectionBackground(new Color(0, 212, 255, 50));
        t.setSelectionForeground(TEXT_WHITE);
        t.setShowVerticalLines(false);
        t.setFillsViewportHeight(true);
        JTableHeader th = t.getTableHeader();
        th.setBackground(new Color(15, 23, 50));
        th.setForeground(CYAN);
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBorder(new MatteBorder(0, 0, 1, 0, new Color(30, 41, 59)));
    }

    private JButton actionButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }
}
