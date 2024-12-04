package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.DAO.TransactionDAO;
import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.User.Client;
import com.example.blockchainjava.Model.User.User;
import com.example.blockchainjava.Model.DAO.DatabaseConnection;
import com.example.blockchainjava.Util.Network.SocketClient;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.Session;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import com.example.blockchainjava.Util.Security.SecurityUtils;
import com.example.blockchainjava.Model.DAO.UserDAO;

import java.security.PrivateKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TransactionFormController {
    @FXML
    private TextField receiverField;
    @FXML
    private TextField amountField;
    private TransactionDAO transactionDAO;
    private final Connection connection;
    private UserDAO userDAO;
    private Client client;
    // Default constructor for FXML
    public TransactionFormController() {
        // Initialize the database connection
        this.connection = DatabaseConnection.getConnection();
        transactionDAO = new TransactionDAO();
        User currentUser = Session.getCurrentUser();
        this.userDAO = new UserDAO();
        this.client = userDAO.getClientFromDatabase(currentUser.getId());
    }

    // Méthode pour générer la signature avec la clé privée décodée
    public String generateSignature(Transaction transaction, PrivateKey privateKey) {
        try {
            // Créer les données à signer (par exemple, l'ID de l'expéditeur et le montant)
            String dataToSign = Transaction.generateDataToSign(
                    transaction.getSenderId(), transaction.getReceiverKey(), transaction.getAmount()
            );
            // Signer les données avec la clé privée
            return SecurityUtils.signData(dataToSign, privateKey); // Utiliser la méthode signData avec la clé privée décodée
        } catch (Exception e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }

    // Method to handle form submission
    public void submitTransaction() {
        try {
            System.out.println("=== Start of submitTransaction ===");


            User currentUser = Session.getCurrentUser();
            this.client = userDAO.getClientFromDatabase(currentUser.getId());
            if (currentUser == null) {
                throw new IllegalStateException("No user is currently logged in. Cannot submit transaction.");
            }

            System.out.println("Current user: " + client.getUsername() + ", Balance: " + client.getBalance());

            String receiverPublicKey = receiverField.getText();
            double amount = Double.parseDouble(amountField.getText());

            System.out.println("Receiver public key: " + receiverPublicKey + ", Amount: " + amount);

            if (!isValidReceiverPublicKey(receiverPublicKey)) {
                throw new IllegalArgumentException("The receiver's public key is invalid.");
            }

            if (amount > currentUser.getBalance()) {
                throw new IllegalArgumentException("You do not have enough balance to complete this transaction.");
            }

            Transaction transaction = new Transaction(
                    client.getId(),
                    receiverPublicKey,
                    amount,
                    TransactionStatus.PENDING
            );

            System.out.println("Transaction created: " + transaction);

            String signature = client.sign(transaction , this.client);
            transaction.setSignature(signature);
            System.out.println("Transaction signed: " + transaction);
            System.out.println("Data used for signing: " + transaction.getDataToSign());
            // Save the transaction in the database with status PENDING
            transactionDAO.saveTransaction(transaction);
            System.out.println("Transaction saved in database.");

            // Create a background task for sending the transaction to validators
            javafx.concurrent.Task<Void> sendTransactionTask = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() {
                    List<SocketClient> validators = getValidators();
                    System.out.println("Validators list: " + validators);

                    for (SocketClient validator : validators) {
                        try {
                            System.out.println("Connecting to validator: " + validator);
                            validator.connect();

                            System.out.println("Sending transaction to validator...");
                            validator.sendTransaction(transaction);

                            String response = validator.receiveResponse();
                            System.out.println("Validator response: " + response);

                            validator.close();
                            System.out.println("Connection to validator closed.");
                        } catch (Exception e) {
                            System.out.println("Error sending transaction to validator: " + validator);
                            e.printStackTrace();
                        } finally {
                            try {
                                validator.close();
                            } catch (Exception ignored) {}
                        }
                    }
                    return null;
                }

                @Override
                protected void succeeded() {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText("Transaction Submitted");
                    alert.setContentText("Your transaction has been submitted and sent to the validators.");
                    alert.showAndWait();
                }

                @Override
                protected void failed() {
                    Throwable e = getException();
                    e.printStackTrace();

                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Transaction failed");
                    alert.setContentText("Error details: " + e.getMessage());
                    alert.showAndWait();
                }
            };

            // Run the task in a background thread
            new Thread(sendTransactionTask).start();

            // Clear the form
            receiverField.clear();
            amountField.clear();

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Transaction failed");
            alert.setContentText("Error details: " + e.getMessage());
            alert.showAndWait();
        } finally {
            System.out.println("=== End of submitTransaction ===");
        }
    }




    // Méthode fictive pour récupérer la liste des validateurs (adresses IP et ports)
    // Méthode pour récupérer les validateurs depuis la base de données
    private List<SocketClient> getValidators() {
        List<SocketClient> validators = new ArrayList<>();
        String sql = "SELECT ip_address, port FROM validators"; // Table 'validators' avec colonnes 'ip_address' et 'port'

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String ip = rs.getString("ip_address");
                int port = rs.getInt("port");
                validators.add(new SocketClient(ip, port));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error fetching validators from database: " + e.getMessage());
        }

        return validators;
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