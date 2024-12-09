package com.example.blockchainjava.Util.Network;

import java.time.LocalDateTime;

public class BlockMessage {
    private int Id;
    private String previousHash;
    private String currentHash;
   // private LocalDateTime timestamp;
    private String signature;

    public BlockMessage(){}
    public BlockMessage(int Id,String previousHash,String currentHash,String signature) {
        this.Id = Id;
        this.previousHash = previousHash;
        this.currentHash= currentHash;
        this.signature = signature;
    }

    // Add getters
    public int getId() { return Id; }
    public String getSignature() { return signature; }


    public String getCurrentHash() {
        return currentHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    @Override
    public String toString() {
        return "BlockMessage{" +
                "Id=" + Id +
                ", previousHash='" + previousHash + '\'' +
                ", currentHash='" + currentHash + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }

}
