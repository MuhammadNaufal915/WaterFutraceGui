package gui;

import model.*;
import service.ProductService;
import utils.SessionManager;
import utils.ValidationUtil;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * ProductPanel - CRUD produk untuk Company, dan beli produk untuk Individual.
 */
public class ProductPanel extends JPanel {

    private static final Color BG         = new Color(238, 242, 247);
    private static final Color CARD_BG    = new Color(255, 255, 255);
    private static final Color CYAN       = new Color(37, 99, 235); // Royal Blue
    private static final Color GREEN      = new Color(16, 185, 129);
    private static final Color AMBER      = new Color(245, 158, 11);
    private static final Color DANGER     = new Color(239, 68, 68);
    private static final Color TEXT_WHITE = new Color(15, 23, 42); // Dark text
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color INPUT_BG   = new Color(248, 250, 252);

    private final ProductService productService = new ProductService();

    private JTable          table;
    private DefaultTableModel tableModel;
    private JLabel          lblStatus;

    // Form fields
    private JTextField      tfProductName, tfWaterCredit, tfEntitas;
    private JPanel          formPanel;
    private JLabel          lblFormTitle; // Dijadikan variabel agar judul form bisa berubah dinamis
    private JButton         btnSave;      // Dijadikan variabel agar teks tombol bisa berubah
    private boolean         isCompany;
    
    // --- PENYELAMAT BUG ---
    private Integer         selectedProductId = null; // Menyimpan ID produk yang sedang diedit

    public ProductPanel() {
        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        initUI();
    }

    private void initUI() {
        User user = SessionManager.getInstance().getCurrentUser();
        isCompany = user != null && user.getRole() == ERole.COMPANY;

        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BG);
        hdr.setBorder(new EmptyBorder(24, 32, 12, 32));
        JLabel title = new JLabel(isCompany ? "📦  Kelola Produk Saya" : "🛒  Daftar Produk Tersedia");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);
        hdr.add(title, BorderLayout.WEST);
        JButton btnRefresh = actionBtn("🔄 Refresh", CYAN);
        btnRefresh.addActionListener(e -> refresh());
        hdr.add(btnRefresh, BorderLayout.EAST);
        add(hdr, BorderLayout.NORTH);

        // Main layout
        JPanel mainPanel;
        if (isCompany) {
            mainPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        } else {
            mainPanel = new JPanel(new BorderLayout());
        }
        mainPanel.setBackground(BG);
        mainPanel.setBorder(new EmptyBorder(0, 32, 24, 32));

        // Table panel
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(CARD_BG);
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] cols = isCompany
            ? new String[]{"ID", "Nama Produk", "Water Credit", "Stok", "Harga/Unit"}
            : new String[]{"ID", "Nama Produk", "Perusahaan", "Stok", "Harga/Unit"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(CARD_BG);
        scroll.getViewport().setBackground(CARD_BG);
        scroll.setBorder(null);
        tableCard.add(scroll, BorderLayout.CENTER);

        // Bottom buttons for company
        if (isCompany) {
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            btnRow.setBackground(CARD_BG);
            JButton btnDelete = actionBtn("🗑️ Hapus", DANGER);
            btnDelete.addActionListener(e -> deleteSelected());
            JButton btnLoadEdit = actionBtn("✏️ Edit di Form", AMBER);
            btnLoadEdit.addActionListener(e -> loadToForm());
            btnRow.add(btnLoadEdit);
            btnRow.add(btnDelete);
            tableCard.add(btnRow, BorderLayout.SOUTH);
        }

        // Status
        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(TEXT_MUTED);
        lblStatus.setBorder(new EmptyBorder(4, 0, 0, 0));
        tableCard.add(lblStatus, BorderLayout.NORTH);

        mainPanel.add(tableCard, isCompany ? null : BorderLayout.CENTER);

        // Form panel (company only)
        if (isCompany) {
            formPanel = buildFormPanel();
            mainPanel.add(formPanel);
        }

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel buildFormPanel() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(24, 24, 24, 24));

        lblFormTitle = new JLabel("Tambah Produk Baru");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblFormTitle.setForeground(CYAN);
        card.add(lblFormTitle);
        card.add(Box.createVerticalStrut(20));

        card.add(fieldLabel("Nama Produk"));
        tfProductName = inputField("Nama produk");
        card.add(tfProductName); card.add(Box.createVerticalStrut(10));

        card.add(fieldLabel("Water Credit yang Digunakan"));
        tfWaterCredit = inputField("Contoh: 100.0");
        card.add(tfWaterCredit); card.add(Box.createVerticalStrut(10));

        card.add(fieldLabel("Jumlah Stok (Entitas)"));
        tfEntitas = inputField("Contoh: 10");
        card.add(tfEntitas); card.add(Box.createVerticalStrut(20));

        JLabel note = new JLabel("<html><small>Harga/Unit = Water Credit ÷ Stok</small></html>");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        note.setForeground(TEXT_MUTED);
        card.add(note); card.add(Box.createVerticalStrut(12));

        btnSave = actionBtn("💾 Simpan Produk", GREEN);
        btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnSave.addActionListener(e -> saveProduct());
        card.add(btnSave); card.add(Box.createVerticalStrut(8));

        JButton btnClear = actionBtn("✖ Reset Form", new Color(148, 163, 184));
        btnClear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnClear.addActionListener(e -> clearForm());
        card.add(btnClear);

        return card;
    }

    public void refresh() {
        tableModel.setRowCount(0);
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        List<Product> products;
        try {
            products = isCompany ? productService.getMyProducts(user) : productService.getAllProducts();
        } catch (Exception ex) {
            lblStatus.setText("❌ " + ex.getMessage());
            return;
        }

        for (Product p : products) {
            if (isCompany) {
                tableModel.addRow(new Object[]{
                    p.getIdProduct(), p.getProductName(),
                    String.format("%.4f", p.getWaterCredit()),
                    p.getEntitas(),
                    String.format("%.4f", p.getHargaJual())
                });
            } else {
                String compName = (p.getCompany() != null && p.getCompany().getUser() != null)
                    ? p.getCompany().getUser().getName() : "?";
                tableModel.addRow(new Object[]{
                    p.getIdProduct(), p.getProductName(), compName,
                    p.getEntitas(),
                    String.format("%.4f", p.getHargaJual())
                });
            }
        }
        lblStatus.setText("Total: " + products.size() + " produk");
    }

    private void saveProduct() {
        String name   = tfProductName.getText().trim();
        String wcStr  = tfWaterCredit.getText().trim();
        String entStr = tfEntitas.getText().trim();

        if (ValidationUtil.isEmpty(name))           { setErr("Nama produk wajib diisi."); return; }
        if (!ValidationUtil.isPositiveDouble(wcStr)) { setErr("Water Credit harus angka positif."); return; }
        if (!ValidationUtil.isPositiveInt(entStr))   { setErr("Stok harus bilangan bulat positif."); return; }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        float wc  = Float.parseFloat(wcStr);
        int   ent = Integer.parseInt(entStr);

        try {
            if (selectedProductId == null) {
                // JIKA MODE TAMBAH BARU (ID NULL)
                productService.createProduct(user, name, wc, ent);
                setOk("✅ Produk berhasil ditambahkan.");
            } else {
                // JIKA MODE EDIT/UPDATE (ID TERSEDIA)
                // Catatan: Pastikan method updateProduct ini sudah ada di ProductService Anda!
                productService.updateProduct(user,selectedProductId, name, wc, ent);
                setOk("✅ Produk berhasil diperbarui.");
            }
            clearForm();
            refresh();
        } catch (Exception ex) {
            setErr(ex.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu.", "Peringatan", JOptionPane.WARNING_MESSAGE); return; }
        int id   = (int) tableModel.getValueAt(row, 0);
        String nm = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this, "Hapus produk '" + nm + "'?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            productService.deleteProduct(SessionManager.getInstance().getCurrentUser(), id);
            setOk("✅ Produk dihapus.");
            if (selectedProductId != null && selectedProductId == id) {
                clearForm(); // Bersihkan form jika produk yang sedang diedit justru dihapus
            }
            refresh();
        } catch (Exception ex) {
            setErr(ex.getMessage());
        }
    }

    private void loadToForm() {
        int row = table.getSelectedRow();
        if (row < 0) { 
            JOptionPane.showMessageDialog(this, "Pilih produk dari tabel terlebih dahulu.", "Peringatan", JOptionPane.WARNING_MESSAGE); 
            return; 
        }
        
        // 1. Ambil ID unik dari kolom index 0 dan simpan ke state global panel
        selectedProductId = (int) tableModel.getValueAt(row, 0);
        
        // 2. Set field teks
        tfProductName.setText((String) tableModel.getValueAt(row, 1));
        tfWaterCredit.setText(tableModel.getValueAt(row, 2).toString().replace(",", "."));
        tfEntitas.setText(tableModel.getValueAt(row, 3).toString());
        
        // 3. Ubah kosmetik judul form & tombol agar user tahu mereka sedang mengedit data
        lblFormTitle.setText("✏️ Edit Produk (ID: " + selectedProductId + ")");
        btnSave.setText("💾 Perbarui Produk");
        btnSave.setBackground(AMBER);
    }

    private void clearForm() {
        tfProductName.setText(""); 
        tfWaterCredit.setText(""); 
        tfEntitas.setText("");
        
        // Kembalikan status form ke mode Insert awal
        selectedProductId = null; 
        if (lblFormTitle != null) lblFormTitle.setText("Tambah Produk Baru");
        if (btnSave != null) {
            btnSave.setText("💾 Simpan Produk");
            btnSave.setBackground(GREEN);
        }
    }

    private void setErr(String m) { lblStatus.setText("❌ " + m); lblStatus.setForeground(DANGER); }
    private void setOk(String m)  { lblStatus.setText(m); lblStatus.setForeground(GREEN); }

    private void styleTable(JTable t) {
        t.setBackground(CARD_BG); t.setForeground(TEXT_WHITE); t.setGridColor(new Color(226,232,240));
        t.setRowHeight(36); t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setSelectionBackground(new Color(37, 99, 235, 30)); t.setSelectionForeground(TEXT_WHITE);
        t.setShowVerticalLines(false); t.setFillsViewportHeight(true);
        JTableHeader th = t.getTableHeader();
        th.setBackground(new Color(248,250,252)); th.setForeground(CYAN);
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBorder(new MatteBorder(0,0,1,0, new Color(226,232,240)));
    }

    private JLabel fieldLabel(String t) {
        JLabel l = new JLabel(t); l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXT_MUTED); l.setAlignmentX(LEFT_ALIGNMENT); return l;
    }

    private JTextField inputField(String tip) {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14)); tf.setForeground(TEXT_WHITE);
        tf.setBackground(INPUT_BG); tf.setCaretColor(CYAN);
        tf.setBorder(new CompoundBorder(new LineBorder(new Color(226,232,240),1,true), new EmptyBorder(8,12,8,12)));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); tf.setAlignmentX(LEFT_ALIGNMENT);
        tf.setToolTipText(tip); return tf;
    }

    private JButton actionBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13)); btn.setForeground(Color.WHITE);
        btn.setBackground(bg); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 14, 8, 14)); return btn;
    }
}