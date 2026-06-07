package gui;

import model.Individual;
import model.User;
import model.WaterCreditRequest;
import service.WaterCreditRequestService;
import utils.SessionManager;
import utils.ValidationUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * WaterCreditPanel - Panel transaksi water credit antar individual.
 * Menyediakan dua metode pembelian: Onsite dan Random (Broadcast).
 */
public class WaterCreditPanel extends JPanel {

    private static final Color BG         = new Color(10, 14, 26);
    private static final Color CARD_BG    = new Color(18, 28, 50);
    private static final Color CYAN       = new Color(0, 212, 255);
    private static final Color GREEN      = new Color(16, 185, 129);
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color INPUT_BG   = new Color(30, 44, 70);
    private static final Color DANGER     = new Color(239, 68, 68);
    private static final Color WARNING    = new Color(245, 158, 11);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final WaterCreditRequestService creditService = new WaterCreditRequestService();

    // Tab 1 Components
    private JTable sellerTable, sentRequestsTable;
    private DefaultTableModel sellerModel, sentRequestsModel;
    private JTextField tfOnsiteAmount, tfRandomAmount;
    private JLabel lblSelectedSeller;
    private int selectedSellerId = -1;

    // Tab 2 Components
    private JTable onsiteIncomingTable, randomIncomingTable;
    private DefaultTableModel onsiteIncomingModel, randomIncomingModel;

    private JTabbedPane tabbedPane;

    public WaterCreditPanel() {
        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        initUI();
    }

    private void initUI() {
        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BG);
        hdr.setBorder(new EmptyBorder(24, 32, 12, 32));
        JLabel title = new JLabel("💧  Pembelian Water Credit");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);
        hdr.add(title, BorderLayout.WEST);

        JButton btnRefresh = quickBtn("🔄 Refresh Data", CYAN);
        btnRefresh.addActionListener(e -> refresh());
        hdr.add(btnRefresh, BorderLayout.EAST);
        add(hdr, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(BG);
        tabbedPane.setForeground(TEXT_WHITE);

        // Build Tab 1
        tabbedPane.addTab("🛒 Beli Water Credit", buildBuyTab());
        // Build Tab 2
        tabbedPane.addTab("📥 Persetujuan & Broadcast", buildApprovalTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel buildBuyTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(8, 32, 24, 32));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBackground(BG);
        split.setBorder(null);
        split.setDividerLocation(520);
        split.setDividerSize(8);

        // Left Side: Seller list
        JPanel leftPanel = new JPanel(new BorderLayout(0, 12));
        leftPanel.setBackground(BG);

        JPanel sellerCard = new JPanel(new BorderLayout(0, 10));
        sellerCard.setBackground(CARD_BG);
        sellerCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel sellerTitle = new JLabel("Daftar Penjual (Kredit Terbanyak ke Terkecil)");
        sellerTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        sellerTitle.setForeground(CYAN);
        sellerCard.add(sellerTitle, BorderLayout.NORTH);

        String[] sCols = {"ID Individual", "Nama", "Pekerjaan", "Region", "Water Credit"};
        sellerModel = new DefaultTableModel(sCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        sellerTable = new JTable(sellerModel);
        styleTable(sellerTable);
        sellerCard.add(new JScrollPane(sellerTable), BorderLayout.CENTER);

        // Selection Listener
        sellerTable.getSelectionModel().addListSelectionListener(e -> {
            int row = sellerTable.getSelectedRow();
            if (row >= 0) {
                selectedSellerId = (int) sellerModel.getValueAt(row, 0);
                String name = (String) sellerModel.getValueAt(row, 1);
                lblSelectedSeller.setText(name);
            } else {
                selectedSellerId = -1;
                lblSelectedSeller.setText("-");
            }
        });

        // Onsite Buy Form (at bottom of left panel)
        JPanel onsiteBuyForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        onsiteBuyForm.setBackground(CARD_BG);
        onsiteBuyForm.add(qtyLabel("Target Penjual:"));
        lblSelectedSeller = new JLabel("-");
        lblSelectedSeller.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSelectedSeller.setForeground(CYAN);
        onsiteBuyForm.add(lblSelectedSeller);

        onsiteBuyForm.add(qtyLabel("Jumlah:"));
        tfOnsiteAmount = createTextField("10", 6);
        onsiteBuyForm.add(tfOnsiteAmount);

        JButton btnOnsiteBuy = quickBtn("💳 Beli Onsite", GREEN);
        btnOnsiteBuy.addActionListener(e -> doOnsitePurchase());
        onsiteBuyForm.add(btnOnsiteBuy);

        sellerCard.add(onsiteBuyForm, BorderLayout.SOUTH);
        leftPanel.add(sellerCard, BorderLayout.CENTER);

        // Right Side: Random Broadcast Form & Sent Requests list
        JPanel rightPanel = new JPanel(new BorderLayout(0, 16));
        rightPanel.setBackground(BG);

        // Random Broadcast Card
        JPanel randomCard = new JPanel(new BorderLayout(0, 10));
        randomCard.setBackground(CARD_BG);
        randomCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel randomTitle = new JLabel("Broadcast Permintaan Pembelian (Random)");
        randomTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        randomTitle.setForeground(CYAN);
        randomCard.add(randomTitle, BorderLayout.NORTH);

        JPanel randomForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        randomForm.setBackground(CARD_BG);
        randomForm.add(qtyLabel("Jumlah Kredit:"));
        tfRandomAmount = createTextField("10", 8);
        randomForm.add(tfRandomAmount);

        JButton btnRandomBroadcast = quickBtn("📢 Kirim Broadcast", WARNING);
        btnRandomBroadcast.addActionListener(e -> doRandomBroadcast());
        randomForm.add(btnRandomBroadcast);
        randomCard.add(randomForm, BorderLayout.CENTER);

        rightPanel.add(randomCard, BorderLayout.NORTH);

        // My Sent Requests Card
        JPanel sentCard = new JPanel(new BorderLayout(0, 10));
        sentCard.setBackground(CARD_BG);
        sentCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel sentTitle = new JLabel("Permintaan Saya (Sent Requests)");
        sentTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        sentTitle.setForeground(CYAN);
        sentCard.add(sentTitle, BorderLayout.NORTH);

        String[] sentCols = {"ID Request", "Mode", "Tujuan/Penjual", "Jumlah", "Status / Keterangan", "Tanggal"};
        sentRequestsModel = new DefaultTableModel(sentCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        sentRequestsTable = new JTable(sentRequestsModel);
        styleTable(sentRequestsTable);
        sentCard.add(new JScrollPane(sentRequestsTable), BorderLayout.CENTER);

        rightPanel.add(sentCard, BorderLayout.CENTER);

        split.setLeftComponent(leftPanel);
        split.setRightComponent(rightPanel);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildApprovalTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(8, 32, 24, 32));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setBackground(BG);
        split.setBorder(null);
        split.setDividerLocation(300);
        split.setDividerSize(8);

        // Top Part: Onsite Incoming Requests
        JPanel onsitePanel = new JPanel(new BorderLayout(0, 10));
        onsitePanel.setBackground(CARD_BG);
        onsitePanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel onsiteTitle = new JLabel("Permintaan Masuk Khusus (Onsite)");
        onsiteTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        onsiteTitle.setForeground(CYAN);
        onsitePanel.add(onsiteTitle, BorderLayout.NORTH);

        String[] onsiteCols = {"ID Request", "Pembeli", "Pekerjaan Pembeli", "Jumlah Credit", "Tanggal"};
        onsiteIncomingModel = new DefaultTableModel(onsiteCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        onsiteIncomingTable = new JTable(onsiteIncomingModel);
        styleTable(onsiteIncomingTable);
        onsitePanel.add(new JScrollPane(onsiteIncomingTable), BorderLayout.CENTER);

        JPanel onsiteButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        onsiteButtons.setBackground(CARD_BG);
        JButton btnApproveOnsite = quickBtn("✅ Setujui", GREEN);
        btnApproveOnsite.addActionListener(e -> approveOnsiteRequest());
        JButton btnRejectOnsite = quickBtn("❌ Tolak", DANGER);
        btnRejectOnsite.addActionListener(e -> rejectOnsiteRequest());
        onsiteButtons.add(btnRejectOnsite);
        onsiteButtons.add(btnApproveOnsite);
        onsitePanel.add(onsiteButtons, BorderLayout.SOUTH);

        // Bottom Part: Random Broadcast Public Requests
        JPanel randomPanel = new JPanel(new BorderLayout(0, 10));
        randomPanel.setBackground(CARD_BG);
        randomPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel randomTitle = new JLabel("Broadcast Publik Aktif (Random)");
        randomTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        randomTitle.setForeground(CYAN);
        randomPanel.add(randomTitle, BorderLayout.NORTH);

        String[] randomCols = {"ID Request", "Pembeli", "Pekerjaan Pembeli", "Jumlah Credit", "Tanggal"};
        randomIncomingModel = new DefaultTableModel(randomCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        randomIncomingTable = new JTable(randomIncomingModel);
        styleTable(randomIncomingTable);
        randomPanel.add(new JScrollPane(randomIncomingTable), BorderLayout.CENTER);

        JPanel randomButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        randomButtons.setBackground(CARD_BG);
        JButton btnApproveRandom = quickBtn("🤝 Penuhi Request (Approve)", GREEN);
        btnApproveRandom.addActionListener(e -> approveRandomRequest());
        randomButtons.add(btnApproveRandom);
        randomPanel.add(randomButtons, BorderLayout.SOUTH);

        split.setTopComponent(onsitePanel);
        split.setBottomComponent(randomPanel);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    public void refresh() {
        refreshSellers();
        refreshSentRequests();
        refreshIncomingRequests();
    }

    private void refreshSellers() {
        sellerModel.setRowCount(0);
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        try {
            List<Individual> list = creditService.getAllIndividualsSorted(user);
            for (Individual ind : list) {
                sellerModel.addRow(new Object[]{
                    ind.getIdIndividual(),
                    ind.getUser() != null ? ind.getUser().getName() : "-",
                    ind.getPekerjaan() != null ? ind.getPekerjaan() : "-",
                    ind.getRegion() != null ? ind.getRegion().getName() : "-",
                    String.format("%.4f", ind.getWaterCredit() != null ? ind.getWaterCredit() : 0.0)
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal mengambil data penjual: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshSentRequests() {
        sentRequestsModel.setRowCount(0);
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        try {
            List<WaterCreditRequest> list = creditService.getSentRequests(user);
            for (WaterCreditRequest req : list) {
                String modeStr = req.getMode() == WaterCreditRequest.RequestMode.ONSITE ? "Onsite" : "Random";
                String targetStr = req.getSeller() != null && req.getSeller().getUser() != null ?
                    req.getSeller().getUser().getName() : "(Broadcast)";
                
                String statusStr = req.getStatus().name();
                if (req.getStatus() == WaterCreditRequest.RequestStatus.APPROVED) {
                    String approvedBy = req.getSeller() != null && req.getSeller().getUser() != null ?
                        req.getSeller().getUser().getName() : "?";
                    statusStr = "APPROVED (Disetujui oleh " + approvedBy + ")";
                }

                sentRequestsModel.addRow(new Object[]{
                    req.getIdRequest(),
                    modeStr,
                    targetStr,
                    String.format("%.4f", req.getAmount()),
                    statusStr,
                    req.getCreatedAt() != null ? req.getCreatedAt().format(FMT) : "-"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal mengambil data permintaan saya: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshIncomingRequests() {
        onsiteIncomingModel.setRowCount(0);
        randomIncomingModel.setRowCount(0);
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        try {
            // 1. Onsite Incoming
            List<WaterCreditRequest> onsiteList = creditService.getPendingRequestsForSeller(user);
            for (WaterCreditRequest req : onsiteList) {
                String buyerName = req.getBuyer() != null && req.getBuyer().getUser() != null ?
                    req.getBuyer().getUser().getName() : "?";
                String job = req.getBuyer() != null ? req.getBuyer().getPekerjaan() : "-";
                
                onsiteIncomingModel.addRow(new Object[]{
                    req.getIdRequest(),
                    buyerName,
                    job,
                    String.format("%.4f", req.getAmount()),
                    req.getCreatedAt() != null ? req.getCreatedAt().format(FMT) : "-"
                });
            }

            // 2. Random Broadcasts
            List<WaterCreditRequest> randomList = creditService.getPendingBroadcastRequests(user);
            for (WaterCreditRequest req : randomList) {
                String buyerName = req.getBuyer() != null && req.getBuyer().getUser() != null ?
                    req.getBuyer().getUser().getName() : "?";
                String job = req.getBuyer() != null ? req.getBuyer().getPekerjaan() : "-";

                randomIncomingModel.addRow(new Object[]{
                    req.getIdRequest(),
                    buyerName,
                    job,
                    String.format("%.4f", req.getAmount()),
                    req.getCreatedAt() != null ? req.getCreatedAt().format(FMT) : "-"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal mengambil data permintaan masuk: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doOnsitePurchase() {
        if (selectedSellerId == -1) {
            JOptionPane.showMessageDialog(this, "Pilih penjual terlebih dahulu dari tabel.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String amountStr = tfOnsiteAmount.getText().trim();
        if (!ValidationUtil.isPositiveDouble(amountStr)) {
            JOptionPane.showMessageDialog(this, "Jumlah harus berupa angka desimal positif.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String sellerName = lblSelectedSeller.getText();

        int confirm = JOptionPane.showConfirmDialog(this,
            "Kirim permintaan pembelian " + String.format("%.4f", amount) + " Water Credit ke " + sellerName + "?",
            "Konfirmasi Pembelian Onsite", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        User user = SessionManager.getInstance().getCurrentUser();
        try {
            creditService.createOnsiteRequest(user, selectedSellerId, amount);
            JOptionPane.showMessageDialog(this, "Permintaan Onsite berhasil dikirim ke " + sellerName + ".", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            tfOnsiteAmount.setText("10");
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal mengirim permintaan: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doRandomBroadcast() {
        String amountStr = tfRandomAmount.getText().trim();
        if (!ValidationUtil.isPositiveDouble(amountStr)) {
            JOptionPane.showMessageDialog(this, "Jumlah harus berupa angka desimal positif.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double amount = Double.parseDouble(amountStr);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Kirim broadcast permintaan " + String.format("%.4f", amount) + " Water Credit ke semua individual?",
            "Konfirmasi Broadcast", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        User user = SessionManager.getInstance().getCurrentUser();
        try {
            creditService.createRandomRequest(user, amount);
            JOptionPane.showMessageDialog(this, "Permintaan Broadcast acak berhasil dikirim.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            tfRandomAmount.setText("10");
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal mengirim broadcast: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void approveOnsiteRequest() {
        int row = onsiteIncomingTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih permintaan masuk terlebih dahulu.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestId = (int) onsiteIncomingModel.getValueAt(row, 0);
        String buyerName = (String) onsiteIncomingModel.getValueAt(row, 1);
        String amountVal = (String) onsiteIncomingModel.getValueAt(row, 3);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Apakah Anda yakin ingin menyetujui permintaan " + amountVal + " Water Credit dari " + buyerName + "?\n" +
            "Kredit Anda akan terpotong sebesar " + amountVal + ".",
            "Konfirmasi Approval Onsite", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        User user = SessionManager.getInstance().getCurrentUser();
        try {
            creditService.approveRequest(user, requestId);
            JOptionPane.showMessageDialog(this, "Permintaan berhasil disetujui!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal memproses approval: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rejectOnsiteRequest() {
        int row = onsiteIncomingTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih permintaan masuk terlebih dahulu.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestId = (int) onsiteIncomingModel.getValueAt(row, 0);
        String buyerName = (String) onsiteIncomingModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Apakah Anda yakin ingin menolak permintaan dari " + buyerName + "?",
            "Konfirmasi Penolakan Onsite", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        User user = SessionManager.getInstance().getCurrentUser();
        try {
            creditService.rejectRequest(user, requestId);
            JOptionPane.showMessageDialog(this, "Permintaan telah ditolak.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal menolak permintaan: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void approveRandomRequest() {
        int row = randomIncomingTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih permintaan broadcast terlebih dahulu.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestId = (int) randomIncomingModel.getValueAt(row, 0);
        String buyerName = (String) randomIncomingModel.getValueAt(row, 1);
        String amountVal = (String) randomIncomingModel.getValueAt(row, 3);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Apakah Anda yakin ingin memenuhi permintaan broadcast " + amountVal + " Water Credit dari " + buyerName + "?\n" +
            "Kredit Anda akan terpotong sebesar " + amountVal + ".",
            "Konfirmasi Fulfill Broadcast", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        User user = SessionManager.getInstance().getCurrentUser();
        try {
            creditService.approveRequest(user, requestId);
            JOptionPane.showMessageDialog(this, "Permintaan broadcast berhasil dipenuhi!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal memproses fulfillment: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void styleTable(JTable t) {
        t.setBackground(CARD_BG);
        t.setForeground(TEXT_WHITE);
        t.setGridColor(new Color(30, 41, 59));
        t.setRowHeight(34);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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

    private JLabel qtyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(TEXT_WHITE);
        return l;
    }

    private JTextField createTextField(String text, int columns) {
        JTextField tf = new JTextField(text, columns);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setForeground(TEXT_WHITE);
        tf.setBackground(INPUT_BG);
        tf.setCaretColor(CYAN);
        tf.setBorder(new CompoundBorder(new LineBorder(new Color(51, 65, 85), 1, true), new EmptyBorder(6, 8, 6, 8)));
        return tf;
    }

    private JButton quickBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        return btn;
    }
}
