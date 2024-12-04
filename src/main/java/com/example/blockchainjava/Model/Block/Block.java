package com.example.blockchainjava.Model.Block;

import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Util.Security.HashUtil;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class Block {
    private int blockId;
    private String previousHash;
    private String currentHash;
    private Transaction transaction;
    private LocalDateTime timestamp;
    private String validatorSignature;
    public Block() {
        // Constructeur par défaut
    }
    public Block(int blockId , String previousHash, Transaction transaction, String validatorSignature) {
        this.blockId=blockId;
        this.previousHash = previousHash;
        this.transaction = transaction;
        this.timestamp = LocalDateTime.now();
        this.validatorSignature = validatorSignature;
        this.currentHash = calculateHash();
    }
    public Block(int blockId , String previousHash, Transaction transaction, String validatorSignature , LocalDateTime timestamp , String currentHash) {
        this.blockId=blockId;
        this.previousHash = previousHash;
        this.transaction = transaction;
        this.timestamp = timestamp;
        this.validatorSignature = validatorSignature;
        this.currentHash = currentHash;
    }


    public String calculateHash() {
        // Convertir LocalDateTime en Timestamp
        Timestamp timestampAsTimestamp = Timestamp.valueOf(timestamp);

        // Afficher les valeurs pour débogage
        System.out.println("Previous Hash: " + previousHash);
        System.out.println("Transaction ID: " + transaction.getId());
        //System.out.println("Timestamp: " + timestampAsTimestamp.toString());
        System.out.println("Validator Signature: " + validatorSignature);

        // Calculer le hash
        String calculatedHash = HashUtil.sha256(
                previousHash +
                        transaction.getId() +
                        validatorSignature
        );

        System.out.println("Calculated Hash: " + calculatedHash);
        return calculatedHash;
    }

    // Property methods for TableView
    public Integer idProperty() {
        return blockId;
    }

    public String previousHashProperty() {
        return previousHash;
    }

    public String currentHashProperty() {
        return currentHash;
    }

    public Integer transactionIdProperty() {
        return transaction != null ? transaction.getId() : null;
    }

    public Double transactionAmountProperty() {
        return transaction != null ? transaction.getAmount() : null;
    }

    public String validatorSignatureProperty() {
        return validatorSignature;
    }

    public String timestampProperty() {
        return timestamp != null ? timestamp.toString() : "";
    }

    public int getBlockId() {
        return blockId;
    }

    public void setBlockId(int blockId) {
        this.blockId = blockId;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getValidatorSignature() {
        return validatorSignature;
    }

    public void setValidatorSignature(String validatorSignature) {
        this.validatorSignature = validatorSignature;
    }
    @Override
    public String toString() {
        return "Block{" +
                "blockId=" + blockId +
                ", previousHash='" + previousHash + '\'' +
                ", currentHash='" + currentHash + '\'' +
                ", transaction=" + (transaction != null ? transaction.toString() : "null") +
                ", timestamp=" + (timestamp != null ? timestamp.toString() : "null") +
                ", validatorSignature='" + validatorSignature + '\'' +
                '}';
    }

}
