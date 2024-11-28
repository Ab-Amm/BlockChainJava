package com.example.blockchainjava.Util.Network;

public class ValidationMessage {
    private final int transactionId;
    private final int validatorId;
    private final String status;

    public ValidationMessage(int transactionId, int validatorId, String status) {
        this.transactionId = transactionId;
        this.validatorId = validatorId;
        this.status = status;
    }

    public int getTransactionId() { return transactionId; }
    public int getValidatorId() { return validatorId; }
    public String getStatus() { return status; }
}
