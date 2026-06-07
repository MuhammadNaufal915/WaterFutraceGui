package model;

import java.time.LocalDateTime;

/**
 * WaterCreditRequest - Model data untuk permintaan pembelian water credit antar individual.
 */
public class WaterCreditRequest {

    public enum RequestMode {
        ONSITE, RANDOM
    }

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }

    private int           idRequest;
    private Individual    buyer;
    private Individual    seller;
    private double        amount;
    private RequestMode   mode;
    private RequestStatus status;
    private LocalDateTime createdAt;

    public WaterCreditRequest() {
        this.status = RequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // Getter & Setter
    public int getIdRequest() { return idRequest; }
    public void setIdRequest(int idRequest) { this.idRequest = idRequest; }

    public Individual getBuyer() { return buyer; }
    public void setBuyer(Individual buyer) { this.buyer = buyer; }

    public Individual getSeller() { return seller; }
    public void setSeller(Individual seller) { this.seller = seller; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public RequestMode getMode() { return mode; }
    public void setMode(RequestMode mode) { this.mode = mode; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
