package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Util.Network.SocketClient;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class TransactionFormController {
    @FXML
    private TextField receiverField;
    @FXML private TextField amountField;

    private final SocketClient socketClient;
    private final User currentUser;

    public TransactionFormController(SocketClient socketClient, User currentUser) {
        this.socketClient = socketClient;
        this.currentUser = currentUser;
    }

    public void submitTransaction() {
        try {
            Transaction transaction = new Transaction(
                    currentUser.getId(),
                    receiverField.getText(),
                    Double.parseDouble(amountField.getText()),
                    TransactionStatus.PENDING
            );

            // Send to validator through socket
            socketClient.sendTransaction(transaction);

            // Clear form
            receiverField.clear();
            amountField.clear();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Transaction failed");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
