package model;

import java.time.LocalDateTime;

/**
 * Complain - Pengaduan yang dikirim pengguna ke Government
 */
public class Complain {

    public enum ComplaintStatus {
        PENDING, IN_PROGRESS, RESOLVED, REJECTED
    }

    private String          idComplaint;
    private User            sender;
    private User            receiver;
    private String          title;
    private String          description;
    private ComplaintStatus status;
    private LocalDateTime   createdAt;
    private LocalDateTime   updatedAt;
    private String          reply;
    private LocalDateTime   repliedAt;

    public Complain() {
        this.status    = ComplaintStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // Getter & Setter
    public String getIdComplaint() { return idComplaint; }
    public void setIdComplaint(String idComplaint) { this.idComplaint = idComplaint; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public LocalDateTime getRepliedAt() { return repliedAt; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }
}
