package gui;

import model.*;
import service.ProductService;
import service.TransactionService;
import utils.SessionManager;
import utils.ValidationUtil;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * TransactionPanel - Individual membeli produk dan melihat riwayat transaksi.
 */
public class TransactionPanel extends JPanel {

    private static final Color BG         = new Color(238, 242, 247);
    private static final Color CARD_BG    = new Color(255, 255, 255);
    private static final Color CYAN       = new Color(37, 99, 235); // Royal Blue
    private static final Color GREEN      = new Color(16, 185, 129);
    private static final Color TEXT_WHITE = new Color(15, 23, 42); // Dark text
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color INPUT_BG   = new Color(248, 250, 252);
    private static final Color DANGER     = new Color(239, 68, 68);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TransactionService txService      = new TransactionService();
    private final ProductService     productService = new ProductService();

    private JTable  productTable, historyTable;
    private DefaultTableModel productModel, historyModel;
    private JTextField tfQty;
    private JLabel     lblStatus;

    public TransactionPanel() {
        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        initUI();
    }

    private void initUI() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BG);
        hdr.setBorder(new EmptyBorder(24, 32, 12, 32));
        JLabel title = new JLabel("🛒  Beli Produk & Riwayat Transaksi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);
        hdr.add(title, BorderLayout.WEST);
        JButton btnRefresh = quickBtn("🔄 Refresh", CYAN);
        btnRefresh.addActionListener(e -> refresh());
        hdr.add(btnRefresh, BorderLayout.EAST);
        add(hdr, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBackground(BG);
        split.setBorder(new EmptyBorder(0, 32, 24, 32));
        split.setDividerLocation(560);
        split.setDividerSize(8);

        // Left: Daftar produk + form beli
        JPanel leftPanel = new JPanel(new BorderLayout(0, 12));
        leftPanel.setBackground(BG);

        JPanel prodCard = new JPanel(new BorderLayout());
        prodCard.setBackground(CARD_BG);
        prodCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel prodTitle = new JLabel("Produk Tersedia");
        prodTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        prodTitle.setForeground(CYAN);
        prodCard.add(prodTitle, BorderLayout.NORTH);

        String[] pCols = {"ID", "Nama Produk", "Perusahaan", "Stok", "Harga/Unit"};
        productModel = new DefaultTableModel(pCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        productTable = new JTable(productModel);
        styleTable(productTable);
        prodCard.add(new JScrollPane(productTable), BorderLayout.CENTER);

        // Buy form
        JPanel buyBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        buyBar.setBackground(CARD_BG);
        buyBar.add(qtyLabel("Jumlah:"));
        tfQty = new JTextField("1", 5);
        tfQty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tfQty.setForeground(TEXT_WHITE);
        tfQty.setBackground(INPUT_BG);
        tfQty.setCaretColor(CYAN);
        tfQty.setBorder(new CompoundBorder(new LineBorder(new Color(226,232,240),1,true), new EmptyBorder(6,8,6,8)));
        buyBar.add(tfQty);
        JButton btnBuy = quickBtn("💳 Beli Sekarang", GREEN);
        btnBuy.addActionListener(e -> doPurchase());
        buyBar.add(btnBuy);
        prodCard.add(buyBar, BorderLayout.SOUTH);

        leftPanel.add(prodCard, BorderLayout.CENTER);

        // Right: Riwayat transaksi
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(CARD_BG);
        rightPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel histTitle = new JLabel("Riwayat Transaksi Saya");
        histTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        histTitle.setForeground(CYAN);
        rightPanel.add(histTitle, BorderLayout.NORTH);

        String[] hCols = {"ID Tx", "Produk", "Jumlah", "Total Harga", "Tanggal"};
        historyModel = new DefaultTableModel(hCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        styleTable(historyTable);
        rightPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        // Status
        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(TEXT_MUTED);
        rightPanel.add(lblStatus, BorderLayout.SOUTH);

        split.setLeftComponent(leftPanel);
        split.setRightComponent(rightPanel);
        add(split, BorderLayout.CENTER);
    }

    public void refresh() {
        refreshProducts();
        refreshHistory();
    }

    private void refreshProducts() {
        productModel.setRowCount(0);
        List<Product> products = productService.getAllProducts();
        for (Product p : products) {
            String compName = (p.getCompany() != null && p.getCompany().getUser() != null)
                ? p.getCompany().getUser().getName() : "?";
            productModel.addRow(new Object[]{
                p.getIdProduct(), p.getProductName(), compName,
                p.getEntitas(),
                String.format("%.4f", p.getHargaJual())
            });
        }
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        try {
            List<Transaction> txList = txService.getMyTransactions(user);
            for (Transaction tx : txList) {
                String prodName = tx.getProduct() != null ? tx.getProduct().getProductName() : "?";
                String date     = tx.getCreatedAt() != null ? tx.getCreatedAt().format(FMT) : "-";
                historyModel.addRow(new Object[]{
                    tx.getIdTransaction(), prodName,
                    tx.getQuantity(),
                    String.format("%.4f", tx.getTotalPrice()),
                    date
                });
            }
            lblStatus.setText("Total transaksi: " + txList.size());
            lblStatus.setForeground(TEXT_MUTED);
        } catch (Exception ex) {
            lblStatus.setText("❌ " + ex.getMessage());
            lblStatus.setForeground(DANGER);
        }
    }

    private void doPurchase() {
        int row = productTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String qtyStr = tfQty.getText().trim();
        if (!ValidationUtil.isPositiveInt(qtyStr)) {
            JOptionPane.showMessageDialog(this, "Jumlah harus bilangan bulat positif.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) productModel.getValueAt(row, 0);
        String prodName = (String) productModel.getValueAt(row, 1);
        int qty = Integer.parseInt(qtyStr);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Beli " + qty + "x " + prodName + "?", "Konfirmasi Pembelian", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        User user = SessionManager.getInstance().getCurrentUser();
        try {
            Transaction tx = txService.purchase(user, productId, qty);
            lblStatus.setText("✅ Pembelian berhasil! ID Transaksi: " + tx.getIdTransaction());
            lblStatus.setForeground(GREEN);
            refresh();
        } catch (Exception ex) {
            lblStatus.setText("❌ " + ex.getMessage());
            lblStatus.setForeground(DANGER);
        }
    }

    private void styleTable(JTable t) {
        t.setBackground(CARD_BG); t.setForeground(TEXT_WHITE); t.setGridColor(new Color(226,232,240));
        t.setRowHeight(34); t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setSelectionBackground(new Color(37, 99, 235, 30)); t.setSelectionForeground(TEXT_WHITE);
        t.setShowVerticalLines(false); t.setFillsViewportHeight(true);
        JTableHeader th = t.getTableHeader();
        th.setBackground(new Color(248,250,252)); th.setForeground(CYAN);
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBorder(new MatteBorder(0,0,1,0, new Color(226,232,240)));
    }

    private JLabel qtyLabel(String text) {
        JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(TEXT_WHITE); return l;
    }

    private JButton quickBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12)); btn.setForeground(Color.WHITE);
        btn.setBackground(bg); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 14, 7, 14)); return btn;
    }
}
