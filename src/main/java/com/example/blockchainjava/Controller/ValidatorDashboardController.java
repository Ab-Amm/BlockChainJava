package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.Block.Block;
import com.example.blockchainjava.Model.Block.BlockChain;
import com.example.blockchainjava.Model.DAO.TransactionDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.User.Client;
import com.example.blockchainjava.Model.User.User;
import com.example.blockchainjava.Model.User.Validator;
import com.example.blockchainjava.Util.Network.SocketClient;
import com.example.blockchainjava.Util.Network.SocketServer;
import com.example.blockchainjava.Observer.BlockchainUpdateObserver;
import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.DAO.DatabaseConnection;

import com.example.blockchainjava.Util.Network.ValidationMessage;
import com.example.blockchainjava.Util.Security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Alert;
import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

//import static com.example.blockchainjava.Model.DAO.DatabaseConnection.connection;

public class ValidatorDashboardController implements BlockchainUpdateObserver {
    private UserDAO userDAO;
    private TransactionDAO transactionDAO;
    private Map<Integer, List<Validator>> transactionValidatorVotes = new HashMap<>();
    private final Connection connection;



    @FXML
    private TableView<Block> blockTable;

    @FXML
    private TableView<Transaction> pendingTransactionsTable;

    @FXML
    private TableView<Client> userTable;

    @FXML
    private TextField balanceField;
    @FXML
    private TextField clientIdField;

    @FXML
    private TextField clientBalanceField;
    @FXML
    private TableColumn<Client, Integer> id;
    @FXML
    private TableColumn<Client, String> clientNameColumn;
    @FXML
    private TableColumn<Client, Double> clientBalanceColumn;
    @FXML
    private TableColumn<Transaction, Integer> pendingTxIdColumn;

    @FXML
    private TableColumn<Transaction, Integer> senderColumn;

    @FXML
    private TableColumn<Transaction, String> receiverColumn;

    @FXML
    private TableColumn<Transaction, Double> amountColumn;

    @FXML
    private TableColumn<Transaction, String> timestampTxColumn;

    private ObservableList<Transaction> pendingTransactionsList = FXCollections.observableArrayList();


    private Validator validator;
    private BlockChain blockchain;
    private SocketServer socketServer;

    public ValidatorDashboardController() throws NoSuchAlgorithmException {
        this.userDAO = new UserDAO();
        this.transactionDAO = new TransactionDAO();// Initialisation du DAO
        this.blockchain = new BlockChain();
        this.validator = new Validator();
        this.connection = DatabaseConnection.getConnection();
    }

    @FXML
    public void initialize() {
        try {

            id.setCellValueFactory(cellData -> cellData.getValue().getIdProperty().asObject());
            clientNameColumn.setCellValueFactory(cellData -> cellData.getValue().getNameProperty());
            clientBalanceColumn.setCellValueFactory(cellData -> cellData.getValue().getBalanceProperty().asObject());

            pendingTxIdColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().idProperty()));
            senderColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().senderIdProperty()));
            receiverColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().receiverKeyProperty()));
            amountColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().amountProperty()));
            timestampTxColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().createdAtProperty()));

            pendingTransactionsTable.setItems(pendingTransactionsList);
            if (blockchain == null || validator == null) {
                System.out.println("Warning: Blockchain and Validator are not initialized.");
                return;
            }

            blockchain.addObserver(this);


            new Thread(() -> startSocketServer(8080)).start();
            listenForValidationMessages();
            updateBlockchainView();
            updateUserTableView();
        } catch (Exception e) {
            showError("Initialization Error", "An error occurred during initialization: " + e.getMessage());
        }
    }

    private void startSocketServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Validator is listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"))) {
                        String transactionJson = reader.readLine();
                        if (transactionJson != null && !transactionJson.trim().isEmpty()) {
                            if (transactionJson.startsWith("Sending transaction JSON:")) {
                                transactionJson = transactionJson.substring("Sending transaction JSON:".length()).trim();
                            }

                            ObjectMapper objectMapper = new ObjectMapper();
                            objectMapper.registerModule(new JavaTimeModule());  // Register the module

                            try {
                                JsonNode jsonNode = objectMapper.readTree(transactionJson); // Validate JSON
                                Transaction transaction = objectMapper.treeToValue(jsonNode, Transaction.class);
                                System.out.println("Received transaction: " + transaction);
                                Platform.runLater(() -> addTransactionToPendingTable(transaction));
                            } catch (JsonProcessingException e) {
                                Platform.runLater(() -> showError("Transaction Error", "Failed to parse transaction: " + e.getMessage()));
                            }
                        } else {
                            Platform.runLater(() -> showError("Empty Data", "Received empty transaction data."));
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing client request: " + e.getMessage());
                        Platform.runLater(() -> showError("Socket Error", "Error reading data: " + e.getMessage()));
                    } finally {
                        try {
                            clientSocket.close();
                            System.out.println("Client socket closed.");
                        } catch (IOException e) {
                            System.err.println("Error closing client socket: " + e.getMessage());
                        }
                    }
                }).start();
            }
        } catch (BindException e) {
            Platform.runLater(() -> showError("Socket Error", "Port " + port + " is already in use. Please try a different port."));
        } catch (IOException e) {
            Platform.runLater(() -> showError("Socket Error", "An error occurred while receiving a socket: " + e.getMessage()));
        }
    }


    private void updatePendingTransactionsTable() {
        pendingTransactionsTable.setItems(pendingTransactionsList); // Associe les données actualisées à la table
    }

    private void addTransactionToPendingTable(Transaction transaction) {
        if (transaction != null) {
            pendingTransactionsList.add(transaction); // Ajoute la transaction à la liste Observable
            System.out.println("Transaction added to pending list: " + transaction);
            updatePendingTransactionsTable(); // Met à jour la table après l'ajout
        }
    }


    @Override
    public void onBlockchainUpdate(BlockChain updatedBlockchain) {
        this.blockchain = updatedBlockchain;

        // Met à jour la vue sur le thread JavaFX
        Platform.runLater(() -> updateBlockchainView());
    }


    private void updateBlockchainView() {
        Platform.runLater(() -> {
            if (blockchain != null) {
                List<Block> blocks = blockchain.getBlocks();
                ObservableList<Block> blockList = FXCollections.observableArrayList(blocks);
                blockTable.setItems(blockList);

                List<Transaction> pendingTransactions = blockchain.getPendingTransactions();
                ObservableList<Transaction> pendingTransactionsList = FXCollections.observableArrayList(pendingTransactions);
                pendingTransactionsTable.setItems(pendingTransactionsList);

                updateUserTableView();
            }
        });
    }


    private void updateUserTableView() {
        try {
            // Récupérer les clients via le DAO
            List<Client> clients = userDAO.getAllClients(); // Récupérer les clients depuis le DAO
            System.out.println("Clients retrieved: " + clients.size());  // Vérifier la taille de la liste

            // Convertir la liste de clients en une ObservableList
            ObservableList<Client> clientList = FXCollections.observableArrayList(clients);

            // Mise à jour de la table des clients
            userTable.setItems(clientList);
        } catch (Exception e) {
            showError("User Update Error", "Failed to load clients: " + e.getMessage());
        }
    }

    @FXML
    private void validateTransaction() {
        Transaction selectedTransaction = pendingTransactionsTable.getSelectionModel().getSelectedItem();

        if (selectedTransaction != null && blockchain != null) {
            try {
                System.out.println("=== Start of validateTransaction ===");
                System.out.println("Validating transaction: " + selectedTransaction);

                // Fetch the sender's public key
                String senderPublicKeyStr = getSenderPublicKey(String.valueOf(selectedTransaction.getSenderId()));
                if (senderPublicKeyStr == null) {
                    throw new IllegalArgumentException("Sender's public key not found.");
                }
                PublicKey senderPublicKey = SecurityUtils.decodePublicKey(senderPublicKeyStr);

                System.out.println("Sender's public key decoded successfully.");

                // Verify the transaction signature
                boolean isSignatureValid = SecurityUtils.verifySignature(
                        selectedTransaction.getDataToSign(),
                        selectedTransaction.getSignature(),
                        senderPublicKey
                );

                if (!isSignatureValid) {
                    throw new SecurityException("Invalid signature for the transaction.");
                }

                System.out.println("Signature is valid.");

                // Verify the sender's balance
                User sender = getUserById(selectedTransaction.senderIdProperty());
                if (sender == null) {
                    throw new IllegalArgumentException("Sender not found.");
                }

                if (sender.getBalance() < selectedTransaction.getAmount()) {
                    throw new IllegalArgumentException("Insufficient balance for the transaction.");
                }

                System.out.println("Balance check passed.");

                // Notify other validators and record the validation
                notifyValidatorsOfValidation(selectedTransaction);
                addValidatorVoteForTransaction(selectedTransaction);

            } catch (Exception e) {
                showError("Validation Error", "Failed to validate transaction: " + e.getMessage());
            }
        } else {
            showError("Validation Error", "No transaction selected or components not initialized.");
        }
    }

    private boolean isTransactionValidatedByAllValidators(Transaction transaction) {
        // Get the list of validators who have validated this transaction
        List<Validator> validatorsVoted = transactionValidatorVotes.get(transaction.getId());

        // Compare against required number of validators (2 in this case)
        return validatorsVoted != null && validatorsVoted.size() >= getRequiredValidatorCount();
    }

    private int getRequiredValidatorCount() {
        return 2; // Could be made configurable in the future
    }

    private List<Validator> getOtherValidators() {
        List<Validator> validators = new ArrayList<>();
        String sql = "SELECT ip_address, port FROM validators";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            String currentValidatorIp = getCurrentValidatorIp();
            int currentValidatorPort = getCurrentValidatorPort();

            while (rs.next()) {
                String ip = rs.getString("ip_address");
                int port = rs.getInt("port");

                if (ip.equals(currentValidatorIp) && port == currentValidatorPort) {
                    continue;
                }

                Validator validator = new Validator(ip, port);
                validators.add(validator);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching validators from database: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return validators;
    }

    private String getCurrentValidatorIp() {
        // This should be configured properly in a production environment
        return "25.50.53.183";
    }

    private int getCurrentValidatorPort() {
        // This should be configured properly in a production environment
        return 8080;
    }

    private void addValidatorVoteForTransaction(Transaction transaction) {
        int transactionId = transaction.getId();

        // Initialize the list of validators for this transaction if not already present
        transactionValidatorVotes.putIfAbsent(transactionId, new ArrayList<>());

        // Add the current validator's vote if not already in the list
        List<Validator> validators = transactionValidatorVotes.get(transactionId);
        if (!validators.contains(validator)) {
            validators.add(validator);
            System.out.println("Validator " + validator.getId() + " has validated the transaction.");

            // Check if we have enough validations
            if (isTransactionValidatedByAllValidators(transaction)) {
                addTransactionToBlockchain(transactionId);
            }
        } else {
            System.out.println("Validator " + validator.getId() + " has already validated this transaction.");
        }
    }

    private void notifyValidatorsOfValidation(Transaction transaction) throws JsonProcessingException {
        String message = createValidationMessage(transaction);
        for (Validator v : getOtherValidators()) {
            sendMessageToValidator(v, message);
        }
    }

    private String createValidationMessage(Transaction transaction) throws JsonProcessingException {
        ValidationMessage message = new ValidationMessage(
                transaction.getId(),
                validator.getId(),
                "validated"
        );
        return new ObjectMapper().writeValueAsString(message);
    }

    private void sendMessageToValidator(Validator validator, String message) {
        try {
            URL url = new URL("http://" + validator.getIpAddress() + ":" + validator.getPort() + "/validate");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(message.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed to send validation message to validator " + validator.getId());
            }

            connection.disconnect();
        } catch (IOException e) {
            System.err.println("Error sending message to validator " + validator.getId() + ": " + e.getMessage());
        }
    }

    private void listenForValidationMessages() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(validator.getPort()), 0);
            server.createContext("/validate", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String message = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    handleReceivedValidationMessage(message);

                    String response = "Validation message received.";
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            });

            server.start();
            System.out.println("Validator is now listening for validation messages...");
        } catch (IOException e) {
            System.err.println("Error starting validation server: " + e.getMessage());
        }
    }

    private void handleReceivedValidationMessage(String message) {
        try {
            ValidationMessage validationMessage = new ObjectMapper().readValue(message, ValidationMessage.class);

            if ("validated".equals(validationMessage.getStatus())) {
                Transaction transaction = transactionDAO.getTransactionById(validationMessage.getTransactionId());
                if (transaction != null) {
                    addValidatorVoteForTransaction(transaction);
                } else {
                    System.err.println("Transaction not found: " + validationMessage.getTransactionId());
                }
            }
        } catch (IOException e) {
            System.err.println("Invalid message format received: " + e.getMessage());
        }
    }

    private void addTransactionToBlockchain(int transactionId) {
        try {
            Transaction transaction = transactionDAO.getTransactionById(transactionId);
            if (transaction == null) {
                throw new IllegalStateException("Transaction not found: " + transactionId);
            }

            String signature = validator.sign(transaction);
            blockchain.addBlock(transaction, signature);
            updateBlockchainView();
            System.out.println("Transaction " + transactionId + " successfully added to the blockchain.");

            // Clean up the validation votes for this transaction
            transactionValidatorVotes.remove(transactionId);
        } catch (Exception e) {
            System.err.println("Failed to add transaction to blockchain: " + e.getMessage());
        }
    }






    private String getSenderPublicKey(String senderId) {
        // Retrieve the sender's public key from the user database
        User sender = getUserById(Integer.valueOf(senderId));
        return sender != null ? sender.getPublicKey() : null;
    }
    private User getUserById(Integer userId) {
        return userDAO.getUserById(userId); // Adjust this based on your actual DAO implementation
    }




    @FXML
    private void updateClientBalance() {
        // Récupération des données de l'interface utilisateur
        String clientIdText = clientIdField.getText();
        String balanceText = clientBalanceField.getText();

        if (clientIdText.isEmpty() || balanceText.isEmpty()) {
            showError("Input Error", "Both fields must be filled.");
            return;
        }

        int clientId = Integer.parseInt(clientIdText);
        double newBalance = Double.parseDouble(balanceText);

        // Validation du solde
        if (newBalance < 0) {
            showError("Update Error", "Balance cannot be negative.");
            return;
        }

        // Trouver l'utilisateur avec clientId
        User user = userDAO.findUserById(clientId); // Utiliser le DAO pour trouver l'utilisateur
        if (user == null) {
            showError("Update Error", "User not found.");
            return;
        }

        // Appel au DAO pour mettre à jour la balance
        boolean success = userDAO.updateUserBalance(user, newBalance); // Mettre à jour le solde
        if (success) {
            showSuccess("Balance updated successfully.", "Bravo! The balance has been updated.");
            updateUserTableView(); // Actualisation de la table des utilisateurs
        } else {
            showError("Update Error", "Failed to update balance. Please try again.");
        }
    }




    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // Pas d'en-tête
        alert.setContentText(message);
        alert.showAndWait();
    }



    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    public void setBlockchain(BlockChain blockchain) {
        this.blockchain = blockchain;
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }


}