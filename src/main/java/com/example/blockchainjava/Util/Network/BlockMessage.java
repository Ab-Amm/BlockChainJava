package com.example.blockchainjava.Util.Network;

import java.time.LocalDateTime;

public class BlockMessage {
    private final int Id;
    private String previousHash;
    private String currentHash;
    private LocalDateTime timestamp;
    private final String signature;

    public BlockMessage(int Id,String previousHash,String currentHash,LocalDateTime timestamp, String signature) {
        this.Id = Id;
        this.previousHash = previousHash;
        this.currentHash= currentHash;
        this.timestamp = timestamp;
        this.signature = signature;
    }

    // Add getters
    public int getTransactionId() { return Id; }
    public String getSignature() { return signature; }
    @Override
    public String toString() {
        return "BlockMessage{" +
                "Id=" + Id +
                ", previousHash='" + previousHash + '\'' +
                ", currentHash='" + currentHash + '\'' +
                ", timestamp=" + timestamp +
                ", signature='" + signature + '\'' +
                '}';
    }

}
