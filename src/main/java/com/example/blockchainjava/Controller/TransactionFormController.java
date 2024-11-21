package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.DAO.TransactionDAO;
import com.example.blockchainjava.Model.User.User;
import com.example.blockchainjava.Model.DAO.DatabaseConnection;
import com.example.blockchainjava.Util.Network.SocketClient;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TransactionFormController {
    @FXML
    private TextField receiverField;
    @FXML
    private TextField amountField;
    private TransactionDAO transactionDAO;

    private final Connection connection;

    // Default constructor for FXML
    public TransactionFormController() {
        // Initialize the database connection
        this.connection = DatabaseConnection.getConnection();
        transactionDAO=new TransactionDAO();
    }

    // Method to handle form submission
    public void submitTransaction() {
        try {
            // Retrieve the current user from the session
            User currentUser = Session.getCurrentUser();

            // Ensure the user is logged in
            if (currentUser == null) {
                throw new IllegalStateException("No user is currently logged in. Cannot submit transaction.");
            }

            String receiverPublicKey = receiverField.getText();
            double amount = Double.parseDouble(amountField.getText());

            // Debugging: Print input values
            System.out.println("Receiver Public Key: " + receiverPublicKey);
            System.out.println("Amount: " + amount);

            // Step 1: Validate receiver public key
            if (!isValidReceiverPublicKey(receiverPublicKey)) {
                // Debugging: Public key validation failed
                System.out.println("Invalid receiver public key.");
                throw new IllegalArgumentException("The receiver's public key is invalid.");
            }
            if (amount > currentUser.getBalance()) {
                System.out.println("Insufficient funds.");
                throw new IllegalArgumentException("You do not have enough balance to complete this transaction.");
            }


            // Step 2: Create transaction
            Transaction transaction = new Transaction(
                    currentUser.getId(),
                    receiverPublicKey,
                    amount,
                    TransactionStatus.PENDING
            );

            // Debugging: Print transaction details
            System.out.println("Transaction created: " + transaction);
            transactionDAO.saveTransaction(transaction);
            // TODO: Save transaction to database or send it to the blockchain network
            // Example: SocketClient.sendTransaction(transaction);

            // Clear form
            receiverField.clear();
            amountField.clear();

            // Debugging: Transaction saved successfully
            System.out.println("Transaction successfully submitted.");
        } catch (Exception e) {
            e.printStackTrace();

            // Handle exception and show error alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Transaction failed");
            alert.setContentText("Error details: " + e.getMessage());
            alert.showAndWait();
        }
    }

    // Method to validate receiver public key in the database
    private boolean isValidReceiverPublicKey(String publicKey) {
        try {
            // Debugging: Querying the database
            System.out.println("Validating public key: " + publicKey);

            String sql = "SELECT public_key FROM users WHERE public_key = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, publicKey);
            ResultSet rs = stmt.executeQuery();

            // Debugging: Check if a user with the given public key exists
            if (rs.next()) {
                System.out.println("Public key is valid.");
                return true; // Found a match
            } else {
                System.out.println("Public key not found.");
                return false; // No match found
            }
        } catch (Exception e) {
            // Log error if database query fails
            System.out.println("Database error while validating public key: " + e.getMessage());
            return false;
        }
    }
}
