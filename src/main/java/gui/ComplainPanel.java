package gui;

import dao.ComplainDAO;
import dao.GovernmentDAO;
import model.*;
import utils.SessionManager;
import utils.ValidationUtil;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ComplainPanel - Kirim pengaduan (Company/Individual) dan balas pengaduan (Government).
 */
public class ComplainPanel extends JPanel {

    private static final Color BG         = new Color(10, 14, 26);
    private static final Color CARD_BG    = new Color(18, 28, 50);
    private static final Color CYAN       = new Color(0, 212, 255);
    private static final Color GREEN      = new Color(16, 185, 129);
    private static final Color AMBER      = new Color(245, 158, 11);
    private static final Color DANGER     = new Color(239, 68, 68);
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color INPUT_BG   = new Color(30, 44, 70);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ComplainDAO   complainDAO   = new ComplainDAO();
    private final GovernmentDAO governmentDAO = new GovernmentDAO();

    private JTable  table;
    private DefaultTableModel tableModel;
    private JLabel  lblStatus;

    // Form fields
    private JTextField tfTitle;
    private JTextArea  taDesc;

    // Detail area
    private JTextArea  taDetail, taReply;
    private JComboBox<String> cbStatus;
    private JButton btnReply, btnSend;

    private boolean isGov;

    public ComplainPanel() {
        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        initUI();
    }

    private void initUI() {
        User user = SessionManager.getInstance().getCurrentUser();
        isGov = user != null && user.getRole() == ERole.GOVERMENT;

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BG);
        hdr.setBorder(new EmptyBorder(24, 32, 12, 32));
        JLabel title = new JLabel(isGov ? "📢  Pengaduan Masuk" : "📢  Pengaduan Saya");
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
        split.setDividerLocation(480);
        split.setDividerSize(8);

        // Left: Table + Form kirim
        JPanel leftPanel = new JPanel(new BorderLayout(0, 12));
        leftPanel.setBackground(BG);

        // Table
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(CARD_BG);
        tableCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        String[] cols = {"ID", "Judul", "Status", "Dari", "Tanggal"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);
        table.getSelectionModel().addListSelectionListener(e -> loadDetail());
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);
        leftPanel.add(tableCard, BorderLayout.CENTER);

        // Form kirim (non-gov)
        if (!isGov) {
            JPanel sendCard = buildSendForm();
            leftPanel.add(sendCard, BorderLayout.SOUTH);
        }

        // Right: Detail + Reply
        JPanel rightPanel = buildDetailPanel();

        split.setLeftComponent(leftPanel);
        split.setRightComponent(rightPanel);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildSendForm() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel t = new JLabel("Kirim Pengaduan Baru");
        t.setFont(new Font("Segoe UI", Font.BOLD, 15)); t.setForeground(CYAN);
        card.add(t); card.add(Box.createVerticalStrut(12));

        card.add(fldLabel("Judul"));
        tfTitle = inputField();
        card.add(tfTitle); card.add(Box.createVerticalStrut(8));

        card.add(fldLabel("Deskripsi"));
        taDesc = new JTextArea(4, 20);
        taDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        taDesc.setForeground(TEXT_WHITE); taDesc.setBackground(INPUT_BG);
        taDesc.setCaretColor(CYAN); taDesc.setLineWrap(true); taDesc.setWrapStyleWord(true);
        taDesc.setBorder(new CompoundBorder(new LineBorder(new Color(51,65,85),1,true), new EmptyBorder(8,12,8,12)));
        JScrollPane descScroll = new JScrollPane(taDesc);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        descScroll.setAlignmentX(LEFT_ALIGNMENT);
        card.add(descScroll); card.add(Box.createVerticalStrut(10));

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(TEXT_MUTED);
        card.add(lblStatus); card.add(Box.createVerticalStrut(8));

        btnSend = quickBtn("📤 Kirim Pengaduan", GREEN);
        btnSend.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnSend.addActionListener(e -> doSend());
        card.add(btnSend);
        return card;
    }

    private JPanel buildDetailPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(CARD_BG);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel t = new JLabel("Detail Pengaduan");
        t.setFont(new Font("Segoe UI", Font.BOLD, 15)); t.setForeground(CYAN);
        card.add(t, BorderLayout.NORTH);

        taDetail = new JTextArea("Pilih pengaduan dari daftar untuk melihat detail.");
        taDetail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        taDetail.setForeground(TEXT_WHITE); taDetail.setBackground(new Color(10,20,40));
        taDetail.setEditable(false); taDetail.setLineWrap(true); taDetail.setWrapStyleWord(true);
        taDetail.setBorder(new EmptyBorder(10,10,10,10));
        JScrollPane detScroll = new JScrollPane(taDetail);
        card.add(detScroll, BorderLayout.CENTER);

        if (isGov) {
            JPanel replyPanel = new JPanel();
            replyPanel.setBackground(CARD_BG);
            replyPanel.setLayout(new BoxLayout(replyPanel, BoxLayout.Y_AXIS));

            replyPanel.add(fldLabel("Status Pengaduan"));
            cbStatus = new JComboBox<>(new String[]{"PENDING","IN_PROGRESS","RESOLVED","REJECTED"});
            cbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cbStatus.setBackground(INPUT_BG); cbStatus.setForeground(TEXT_WHITE);
            cbStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            cbStatus.setAlignmentX(LEFT_ALIGNMENT);
            replyPanel.add(cbStatus); replyPanel.add(Box.createVerticalStrut(8));

            replyPanel.add(fldLabel("Balasan"));
            taReply = new JTextArea(3, 20);
            taReply.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            taReply.setForeground(TEXT_WHITE); taReply.setBackground(INPUT_BG);
            taReply.setCaretColor(CYAN); taReply.setLineWrap(true); taReply.setWrapStyleWord(true);
            taReply.setBorder(new CompoundBorder(new LineBorder(new Color(51,65,85),1,true), new EmptyBorder(8,8,8,8)));
            JScrollPane rs = new JScrollPane(taReply);
            rs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            rs.setAlignmentX(LEFT_ALIGNMENT);
            replyPanel.add(rs); replyPanel.add(Box.createVerticalStrut(8));

            if (lblStatus == null) {
                lblStatus = new JLabel(" ");
                lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                lblStatus.setForeground(TEXT_MUTED);
            }
            replyPanel.add(lblStatus); replyPanel.add(Box.createVerticalStrut(8));

            btnReply = quickBtn("💬 Kirim Balasan", AMBER);
            btnReply.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            btnReply.addActionListener(e -> doReply());
            replyPanel.add(btnReply);
            card.add(replyPanel, BorderLayout.SOUTH);
        } else {
            lblStatus = new JLabel(" ");
            lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblStatus.setForeground(TEXT_MUTED);
        }
        return card;
    }

    public void refresh() {
        tableModel.setRowCount(0);
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        List<Complain> list = isGov
            ? complainDAO.findByReceiver(user)
            : complainDAO.findBySender(user);

        for (Complain c : list) {
            String from = c.getSender() != null ? c.getSender().getName() : "?";
            String date = c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "-";
            tableModel.addRow(new Object[]{
                c.getIdComplaint(), c.getTitle(), c.getStatus().name(), from, date
            });
        }
        if (lblStatus != null) lblStatus.setText("Total: " + list.size() + " pengaduan");
    }

    private void loadDetail() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String id = (String) tableModel.getValueAt(row, 0);
        complainDAO.findById(id).ifPresent(c -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Judul    : ").append(c.getTitle()).append("\n");
            sb.append("Status   : ").append(c.getStatus().name()).append("\n");
            sb.append("Pengirim : ").append(c.getSender() != null ? c.getSender().getName() : "?").append("\n");
            sb.append("Tanggal  : ").append(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "-").append("\n\n");
            sb.append("DESKRIPSI:\n").append(c.getDescription()).append("\n\n");
            if (c.getReply() != null) {
                sb.append("BALASAN:\n").append(c.getReply()).append("\n");
                sb.append("Dibalas: ").append(c.getRepliedAt() != null ? c.getRepliedAt().format(FMT) : "-");
            }
            taDetail.setText(sb.toString());
            if (isGov && cbStatus != null) cbStatus.setSelectedItem(c.getStatus().name());
        });
    }

    private void doSend() {
        String title = tfTitle.getText().trim();
        String desc  = taDesc.getText().trim();
        if (ValidationUtil.isEmpty(title)) { setErr("Judul wajib diisi."); return; }
        if (ValidationUtil.isEmpty(desc))  { setErr("Deskripsi wajib diisi."); return; }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        // Cari Government di region user
        User receiver = findGovUserForSender(user);
        if (receiver == null) { setErr("Tidak ada Government di region Anda."); return; }

        Complain c = new Complain();
        c.setIdComplaint(UUID.randomUUID().toString());
        c.setSender(user); c.setReceiver(receiver);
        c.setTitle(title); c.setDescription(desc);
        c.setStatus(Complain.ComplaintStatus.PENDING);
        c.setCreatedAt(LocalDateTime.now());

        try {
            complainDAO.insert(c);
            setOk("✅ Pengaduan berhasil dikirim!");
            tfTitle.setText(""); taDesc.setText("");
            refresh();
        } catch (Exception ex) { setErr(ex.getMessage()); }
    }

    private User findGovUserForSender(User sender) {
        dao.CompanyDAO compDAO = new dao.CompanyDAO();
        dao.IndividualDAO indDAO = new dao.IndividualDAO();
        Region region = null;
        if (sender.getRole() == ERole.COMPANY) {
            Optional<model.Company> c = compDAO.findByUser(sender);
            if (c.isPresent()) region = c.get().getRegion();
        } else if (sender.getRole() == ERole.INDIVIDUAL) {
            Optional<model.Individual> i = indDAO.findByUser(sender);
            if (i.isPresent()) region = i.get().getRegion();
        }
        if (region == null) return null;
        Optional<model.Government> govOpt = governmentDAO.findFirstByRegion(region);
        return govOpt.map(model.Government::getUser).orElse(null);
    }

    private void doReply() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Pilih pengaduan terlebih dahulu.","Peringatan",JOptionPane.WARNING_MESSAGE); return; }
        String id    = (String) tableModel.getValueAt(row, 0);
        String reply = taReply.getText().trim();
        String status= (String) cbStatus.getSelectedItem();
        if (ValidationUtil.isEmpty(reply)) { setErr("Balasan wajib diisi."); return; }

        complainDAO.findById(id).ifPresent(c -> {
            c.setReply(reply);
            c.setRepliedAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            c.setStatus(Complain.ComplaintStatus.valueOf(status));
            try {
                complainDAO.update(c);
                setOk("✅ Balasan berhasil dikirim.");
                taReply.setText("");
                refresh();
            } catch (Exception ex) { setErr(ex.getMessage()); }
        });
    }

    private void setErr(String m) { if (lblStatus != null) { lblStatus.setText("❌ " + m); lblStatus.setForeground(DANGER); } }
    private void setOk(String m)  { if (lblStatus != null) { lblStatus.setText(m); lblStatus.setForeground(GREEN); } }

    private void styleTable(JTable t) {
        t.setBackground(CARD_BG); t.setForeground(TEXT_WHITE); t.setGridColor(new Color(30,41,59));
        t.setRowHeight(34); t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setSelectionBackground(new Color(0,212,255,50)); t.setSelectionForeground(TEXT_WHITE);
        t.setShowVerticalLines(false); t.setFillsViewportHeight(true);
        JTableHeader th = t.getTableHeader();
        th.setBackground(new Color(15,23,50)); th.setForeground(CYAN);
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBorder(new MatteBorder(0,0,1,0, new Color(30,41,59)));
    }

    private JLabel fldLabel(String text) {
        JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXT_MUTED); l.setAlignmentX(LEFT_ALIGNMENT); return l;
    }

    private JTextField inputField() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13)); tf.setForeground(TEXT_WHITE);
        tf.setBackground(INPUT_BG); tf.setCaretColor(CYAN);
        tf.setBorder(new CompoundBorder(new LineBorder(new Color(51,65,85),1,true), new EmptyBorder(7,10,7,10)));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38)); tf.setAlignmentX(LEFT_ALIGNMENT);
        return tf;
    }

    private JButton quickBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12)); btn.setForeground(Color.WHITE);
        btn.setBackground(bg); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7,14,7,14)); return btn;
    }
}
