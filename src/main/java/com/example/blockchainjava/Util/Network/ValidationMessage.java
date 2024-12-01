package com.example.blockchainjava.Util.Network;


public class ValidationMessage {
//    public String type="validation";
    private int transactionId;
    private int validatorId;
    private String status;


    public ValidationMessage(int transactionId, int validatorId, String status) {
        this.transactionId = transactionId;
        this.validatorId = validatorId;
        this.status = status;
    }
    public ValidationMessage() {}

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getValidatorId() { return validatorId; }
    public void setValidatorId(int validatorId) { this.validatorId = validatorId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }


}
