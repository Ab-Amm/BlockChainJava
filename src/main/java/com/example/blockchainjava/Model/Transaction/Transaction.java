package com.example.blockchainjava.Model.Transaction;

import com.mysql.cj.conf.StringProperty;
import eu.hansolo.toolbox.properties.DoubleProperty;
import eu.hansolo.toolbox.properties.IntegerProperty;
import eu.hansolo.toolbox.properties.ObjectProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;


public class Transaction implements Serializable {
    private int id;
    private int senderId;
    private String receiverKey; // Clé publique du destinataire
    private Double amount;
    private TransactionStatus status;
    private Integer blockId; // Nullable
    private LocalDateTime createdAt;
    private String signature; // Ajoute l'attribut signature

    public Integer idProperty() {
        return id;
    }

    public Integer senderIdProperty() {
        return senderId;
    }

    public String receiverKeyProperty() {
        return receiverKey;
    }

    public Double amountProperty() {
        return amount;
    }

    public String createdAtProperty() {
        return createdAt != null ? createdAt.toString() : "";
    }


    public TransactionStatus statusProperty() {
        return status;
    }

    // Autres attributs, constructeurs, getters et setters
    public Transaction() {}

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

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
    public Transaction(int id ,int senderId, String receiverKey, Double amount, TransactionStatus status , String signature) {
        this.id=id;
        this.senderId = senderId;
        this.receiverKey = receiverKey;
        this.amount = amount;
        this.status = status;
        this.signature=signature;
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

    public String getDataToSign() {
        return senderId + receiverKey + amount;
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
