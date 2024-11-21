package com.example.blockchainjava.Model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private int id;
    private int senderId;
    private String receiverKey; // Clé publique du destinataire
    private Double amount;
    private TransactionStatus status;
    private Integer blockId; // Nullable
    private LocalDateTime createdAt;

    // Constructeur
    public Transaction(int id, int senderId, String receiverKey, Double amount, TransactionStatus status, Integer blockId, LocalDateTime createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.receiverKey = receiverKey;
        this.amount = amount;
        this.status = status;
        this.blockId = blockId;
        this.createdAt = createdAt;
    }
    public Transaction(int senderId, String receiverKey, Double amount, TransactionStatus status) {
        this.senderId = senderId;
        this.receiverKey = receiverKey;
        this.amount = amount;
        this.status = status;
    }

    // Getters et setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getReceiverKey() {
        return receiverKey;
    }

    public void setReceiverKey(String receiverKey) {
        this.receiverKey = receiverKey;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Integer getBlockId() {
        return blockId;
    }

    public void setBlockId(Integer blockId) {
        this.blockId = blockId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", senderId=" + senderId +
                ", receiverKey='" + receiverKey + '\'' +
                ", amount=" + amount +
                ", status=" + status +
                ", blockId=" + blockId +
                ", createdAt=" + createdAt +
                '}';
    }
}
