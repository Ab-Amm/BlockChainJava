package com.example.blockchainjava.Model.Block;

import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Util.Security.HashUtil;

import java.time.LocalDateTime;

public class Block {
    private Long blockId;
    private String previousHash;
    private String currentHash;
    private Transaction transaction;
    private LocalDateTime timestamp;
    private String validatorSignature;
    public Block() {
        // Constructeur par défaut
    }
    public Block(String previousHash, Transaction transaction, String validatorSignature) {
        this.previousHash = previousHash;
        this.transaction = transaction;
        this.timestamp = LocalDateTime.now();
        this.validatorSignature = validatorSignature;
        this.currentHash = calculateHash();
    }

    private String calculateHash() {
        return HashUtil.sha256(
                previousHash +
                        transaction.getId() +
                        timestamp.toString() +
                        validatorSignature
        );
    }


    public Long getBlockId() {
        return blockId;
    }

    public void setBlockId(Long blockId) {
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
}
