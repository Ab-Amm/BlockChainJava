package com.example.blockchainjava.Util.Network;

public class BlockMessage {
    private final int transactionId;
    private final String signature;
    private final int validatorId;

    public BlockMessage(int transactionId, String signature, int validatorId) {
        this.transactionId = transactionId;
        this.signature = signature;
        this.validatorId = validatorId;
    }

    // Add getters
    public int getTransactionId() { return transactionId; }
    public String getSignature() { return signature; }
    public int getValidatorId() { return validatorId; }
}
