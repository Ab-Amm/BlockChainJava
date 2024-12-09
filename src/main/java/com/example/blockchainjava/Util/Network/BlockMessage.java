package com.example.blockchainjava.Util.Network;

import com.example.blockchainjava.Model.Transaction.Transaction;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class BlockMessage {
    private int blockId;
    private String previousHash;
    private String currentHash;
    private Transaction transaction;
    private String validatorSignature;
    public BlockMessage() {
        // Constructeur par défaut
    }
    public BlockMessage(int blockId , String previousHash, Transaction transaction, String validatorSignature , String currentHash) {
        this.blockId=blockId;
        this.previousHash = previousHash;
        this.transaction = transaction;
        this.validatorSignature = validatorSignature;
        this.currentHash = currentHash;
    }
    // Add getters

    public int getBlockId() {
        return blockId;
    }

    public String getValidatorSignature() {
        return validatorSignature;
    }

    public Transaction getTransaction() {
        return transaction;
    }


    public String getCurrentHash() {
        return currentHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    @Override
    public String toString() {
        return "BlockMessage{" +
                "blockId=" + blockId +
                ", previousHash='" + previousHash + '\'' +
                ", currentHash='" + currentHash + '\'' +
                ", transaction='" + transaction + '\'' +
                ", ValidatorSignature='" + validatorSignature + '\'' +
                '}';
    }

}
