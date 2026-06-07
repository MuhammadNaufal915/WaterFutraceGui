package service;

import dao.IndividualDAO;
import dao.WaterCreditRequestDAO;
import model.Individual;
import model.User;
import model.WaterCreditRequest;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WaterCreditRequestService - Logika bisnis untuk transaksi water credit antar individual.
 */
public class WaterCreditRequestService {

    private final IndividualDAO       individualDAO = new IndividualDAO();
    private final WaterCreditRequestDAO requestDAO    = new WaterCreditRequestDAO();

    public WaterCreditRequest createOnsiteRequest(User buyerUser, int sellerId, double amount) {
        Individual buyer = individualDAO.findByUser(buyerUser)
            .orElseThrow(() -> new RuntimeException("Profil individual pembeli tidak ditemukan."));

        if (!buyerUser.isApproved()) {
            throw new RuntimeException("Akun Anda belum disetujui oleh Government.");
        }

        if (amount <= 0) {
            throw new RuntimeException("Jumlah pembelian harus lebih besar dari 0.");
        }

        Individual seller = individualDAO.findById(sellerId)
            .orElseThrow(() -> new RuntimeException("Profil individual penjual tidak ditemukan."));

        if (buyer.getIdIndividual() == seller.getIdIndividual()) {
            throw new RuntimeException("Anda tidak dapat membeli dari diri Anda sendiri.");
        }

        WaterCreditRequest req = new WaterCreditRequest();
        req.setBuyer(buyer);
        req.setSeller(seller);
        req.setAmount(amount);
        req.setMode(WaterCreditRequest.RequestMode.ONSITE);
        req.setStatus(WaterCreditRequest.RequestStatus.PENDING);
        req.setCreatedAt(LocalDateTime.now());

        return requestDAO.insert(req);
    }

    public WaterCreditRequest createRandomRequest(User buyerUser, double amount) {
        Individual buyer = individualDAO.findByUser(buyerUser)
            .orElseThrow(() -> new RuntimeException("Profil individual pembeli tidak ditemukan."));

        if (!buyerUser.isApproved()) {
            throw new RuntimeException("Akun Anda belum disetujui oleh Government.");
        }

        if (amount <= 0) {
            throw new RuntimeException("Jumlah pembelian harus lebih besar dari 0.");
        }

        WaterCreditRequest req = new WaterCreditRequest();
        req.setBuyer(buyer);
        req.setSeller(null); // NULL for random broadcast
        req.setAmount(amount);
        req.setMode(WaterCreditRequest.RequestMode.RANDOM);
        req.setStatus(WaterCreditRequest.RequestStatus.PENDING);
        req.setCreatedAt(LocalDateTime.now());

        return requestDAO.insert(req);
    }

    public void approveRequest(User sellerUser, int requestId) {
        Individual seller = individualDAO.findByUser(sellerUser)
            .orElseThrow(() -> new RuntimeException("Profil individual penjual tidak ditemukan."));

        if (!sellerUser.isApproved()) {
            throw new RuntimeException("Akun Anda belum disetujui oleh Government.");
        }

        WaterCreditRequest req = requestDAO.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Permintaan pembelian tidak ditemukan."));

        if (req.getStatus() != WaterCreditRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Permintaan sudah diproses sebelumnya.");
        }

        if (req.getMode() == WaterCreditRequest.RequestMode.ONSITE) {
            if (req.getSeller() == null || req.getSeller().getIdIndividual() != seller.getIdIndividual()) {
                throw new RuntimeException("Permintaan ini bukan ditujukan untuk Anda.");
            }
        } else {
            // RANDOM mode
            if (req.getBuyer().getIdIndividual() == seller.getIdIndividual()) {
                throw new RuntimeException("Anda tidak dapat menyetujui permintaan Anda sendiri.");
            }
            req.setSeller(seller);
        }

        double sellerCredit = seller.getWaterCredit() != null ? seller.getWaterCredit() : 0.0;
        if (sellerCredit < req.getAmount()) {
            throw new RuntimeException(String.format(
                "Water credit Anda tidak mencukupi. Saldo Anda: %.4f, Permintaan: %.4f",
                sellerCredit, req.getAmount()
            ));
        }

        Individual buyer = req.getBuyer();
        double buyerCredit = buyer.getWaterCredit() != null ? buyer.getWaterCredit() : 0.0;

        // Transfer credit
        seller.setWaterCredit(sellerCredit - req.getAmount());
        buyer.setWaterCredit(buyerCredit + req.getAmount());

        // Update database
        individualDAO.update(seller);
        individualDAO.update(buyer);

        req.setStatus(WaterCreditRequest.RequestStatus.APPROVED);
        requestDAO.update(req);
    }

    public void rejectRequest(User sellerUser, int requestId) {
        Individual seller = individualDAO.findByUser(sellerUser)
            .orElseThrow(() -> new RuntimeException("Profil individual penjual tidak ditemukan."));

        WaterCreditRequest req = requestDAO.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Permintaan pembelian tidak ditemukan."));

        if (req.getStatus() != WaterCreditRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Permintaan sudah diproses sebelumnya.");
        }

        if (req.getMode() == WaterCreditRequest.RequestMode.ONSITE) {
            if (req.getSeller() == null || req.getSeller().getIdIndividual() != seller.getIdIndividual()) {
                throw new RuntimeException("Permintaan ini bukan ditujukan untuk Anda.");
            }
        } else {
            throw new RuntimeException("Permintaan broadcast acak tidak dapat ditolak oleh satu pihak.");
        }

        req.setStatus(WaterCreditRequest.RequestStatus.REJECTED);
        requestDAO.update(req);
    }

    public List<WaterCreditRequest> getPendingRequestsForSeller(User sellerUser) {
        Individual seller = individualDAO.findByUser(sellerUser)
            .orElseThrow(() -> new RuntimeException("Profil individual tidak ditemukan."));
        return requestDAO.findBySeller(seller.getIdIndividual()).stream()
            .filter(r -> r.getStatus() == WaterCreditRequest.RequestStatus.PENDING && r.getMode() == WaterCreditRequest.RequestMode.ONSITE)
            .collect(Collectors.toList());
    }

    public List<WaterCreditRequest> getPendingBroadcastRequests(User sellerUser) {
        Individual seller = individualDAO.findByUser(sellerUser)
            .orElseThrow(() -> new RuntimeException("Profil individual tidak ditemukan."));
        return requestDAO.findPendingBroadcasts(seller.getIdIndividual());
    }

    public List<WaterCreditRequest> getSentRequests(User buyerUser) {
        Individual buyer = individualDAO.findByUser(buyerUser)
            .orElseThrow(() -> new RuntimeException("Profil individual tidak ditemukan."));
        return requestDAO.findByBuyer(buyer.getIdIndividual());
    }

    public List<Individual> getAllIndividualsSorted(User currentUser) {
        Individual currentInd = individualDAO.findByUser(currentUser).orElse(null);
        int currentIndId = currentInd != null ? currentInd.getIdIndividual() : -1;

        return individualDAO.findAll().stream()
            .filter(i -> i.getUser() != null && i.getUser().isApproved())
            .filter(i -> i.getIdIndividual() != currentIndId)
            .sorted(Comparator.comparing(Individual::getWaterCredit, Comparator.nullsFirst(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }
}
