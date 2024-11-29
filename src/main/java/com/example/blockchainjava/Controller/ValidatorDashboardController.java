package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.Block.Block;
import com.example.blockchainjava.Model.Block.BlockChain;
import com.example.blockchainjava.Model.DAO.TransactionDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.User.Client;
import com.example.blockchainjava.Model.User.Session;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


//import static com.example.blockchainjava.Model.DAO.DatabaseConnection.connection;

public class ValidatorDashboardController implements BlockchainUpdateObserver {
    private UserDAO userDAO;
    private TransactionDAO transactionDAO;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final Map<Integer, List<Validator>> transactionValidatorVotes = new ConcurrentHashMap<>();

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

    public ValidatorDashboardController() throws NoSuchAlgorithmException {
        this.userDAO = new UserDAO();
        this.transactionDAO = new TransactionDAO();// Initialisation du DAO
        this.blockchain = new BlockChain();
        User currentUser = Session.getCurrentUser();
        if (currentUser != null) {
            int Id = currentUser.getId(); // Get the username from the current user;
            this.validator = new Validator(Id , currentUser.getUsername() , currentUser.getBalance());
        }else {
        System.err.println("No user is currently logged in.");
        }
        this.connection = DatabaseConnection.getConnection();
        System.out.println("this is the validator connected to this dashboard: " + validator);
        this.validator.loadValidatorData(currentUser.getId());
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
            User currentUser = Session.getCurrentUser();
            this.validator.loadValidatorData(currentUser.getId());
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
        if (selectedTransaction == null || blockchain == null) {
            showError("Validation Error", "No transaction selected or components not initialized.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String senderPublicKeyStr = getSenderPublicKey(String.valueOf(selectedTransaction.getSenderId()));
                if (senderPublicKeyStr == null) {
                    throw new IllegalArgumentException("Sender's public key not found.");
                }

                PublicKey senderPublicKey = SecurityUtils.decodePublicKey(senderPublicKeyStr);
                boolean isSignatureValid = SecurityUtils.verifySignature(
                        selectedTransaction.getDataToSign(),
                        selectedTransaction.getSignature(),
                        senderPublicKey
                );

                if (!isSignatureValid) {
                    throw new SecurityException("Invalid signature.");
                }

                User sender = getUserById(selectedTransaction.senderIdProperty());
                if (sender == null || sender.getBalance() < selectedTransaction.getAmount()) {
                    throw new IllegalArgumentException("Invalid sender or insufficient balance.");
                }

                notifyValidatorsOfValidation(selectedTransaction);
                addValidatorVoteForTransaction(selectedTransaction);

            } catch (Exception e) {
                Platform.runLater(() -> showError("Validation Error", e.getMessage()));
            }
        }, executorService);
    }

    private boolean isTransactionValidatedByAllValidators(Transaction transaction) {
        System.out.println("Checking if transaction " + transaction.getId() + " has been validated by all validators.");
        // Get the list of validators who have validated this transaction
        List<Validator> validatorsVoted = transactionValidatorVotes.get(transaction.getId());
        System.out.println("Validators who have validated this transaction: " + validatorsVoted);

        // Compare against required number of validators (2 in this case)
        return validatorsVoted != null && validatorsVoted.size() >= getRequiredValidatorCount();
    }
    private int getRequiredValidatorCount() {
        return 2; // Could be made configurable in the future
    }

    private void addValidatorVoteForTransaction(Transaction transaction) {
        int transactionId = transaction.getId();
        User currentUser = Session.getCurrentUser();
        this.validator.loadValidatorData(currentUser.getId());

        // Initialize the list of validators for this transaction if not already present
        transactionValidatorVotes.putIfAbsent(transactionId, new ArrayList<>());

        // Add the current validator's vote if not already in the list
        List<Validator> validators = transactionValidatorVotes.get(transactionId);
        if (!validators.contains(this.validator)) {
            validators.add(this.validator);
            System.out.println("Validator " + this.validator.getId() +" (" + this.validator.getIpAddress() + ") " + " has validated the transaction.");

            // Check if we have enough validations
            if (isTransactionValidatedByAllValidators(transaction)) {
                System.out.println("Transaction " + transactionId + " has been validated by all validators.");
                System.out.println("Adding transaction to blockchain...");
                addTransactionToBlockchain(transactionId);
            }
        } else {
            System.out.println("Validator " + this.validator.getId() + " has already validated this transaction.");
        }
    }
    private void addTransactionToBlockchain(int transactionId) {
        try {
            System.out.println("Adding transaction " + transactionId + " to the blockchain...");
            Transaction transaction = transactionDAO.getTransactionById(transactionId);
            System.out.println("Transaction found: " + transaction);
            if (transaction == null) {
                throw new IllegalStateException("Transaction not found: " + transactionId);
            }


            String signature = validator.sign(transaction);
            System.out.println("Signature: " + signature);
            blockchain.addBlock(transaction, signature);
            updateBlockchainView();
            System.out.println("Transaction " + transactionId + " successfully added to the blockchain.");

            // Clean up the validation votes for this transaction
            transactionValidatorVotes.remove(transactionId);
        } catch (Exception e) {
            System.err.println("Failed to add transaction to blockchain: " + e.getMessage());
            e.printStackTrace();
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

    private List<Validator> getOtherValidators() {
        List<Validator> validators = new ArrayList<>();
        String sql = """
        SELECT v.id, u.username, v.ip_address, v.port, u.balance 
        FROM validators v
        JOIN users u ON v.id = u.id
        WHERE u.role = 'VALIDATOR'
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            // Obtenir les informations du validateur actuel
            String currentValidatorIp = getCurrentValidatorIp();
            int currentValidatorPort = getCurrentValidatorPort();

            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String ip = rs.getString("ip_address");
                int port = rs.getInt("port");
                double balance = rs.getDouble("balance");

                // Exclure le validateur actuel
                if (ip.equals(currentValidatorIp) && port == currentValidatorPort) {
                    continue;
                }

                // Vérifier que le username n'est pas vide
                if (username == null || username.isBlank()) {
                    System.err.println("Validator with empty username found, skipping...");
                    continue;
                }

                // Créer l'objet Validator
                Validator validator = new Validator(id, username, ip, port, balance);
                validators.add(validator);
                System.out.println("Validator added to the list of other validators: " + validator);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching validators from database: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return validators;
    }


    private String getCurrentValidatorIp() {
        User currentUser = Session.getCurrentUser();
        this.validator.loadValidatorData(currentUser.getId());
        System.out.println("l address de valid actuelle qu'il faut ignorer lors de send "+validator.getIpAddress());
        return validator.getIpAddress();
    }

    private int getCurrentValidatorPort() {
        User currentUser = Session.getCurrentUser();
        this.validator.loadValidatorData(currentUser.getId());
        System.out.println("le port de valid actuelle qu'il faut ignorer lors de send "+validator.getPort());
        return validator.getPort();
    }

    private void notifyValidatorsOfValidation(Transaction transaction) throws JsonProcessingException {
        String message = createValidationMessage(transaction);
        List<Validator> validators = getOtherValidators();

        for (Validator v : validators) {
            CompletableFuture.runAsync(() -> {
                try {
                    sendMessageToValidator(v, message);
                } catch (Exception e) {
                    System.err.println("Failed to notify validator " + v.getId() + ": " + e.getMessage());
                }
            }, executorService);
        }
    }

    private void sendMessageToValidator(Validator validator, String message) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://" + validator.getIpAddress() + ":" + validator.getPort() + "/validate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(message))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Invalid response: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message to validator", e);
        }
    }
    private void listenForValidationMessages() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
            server.setExecutor(executorService);
            server.createContext("/validate", exchange -> {
                String response = "Validation received";
                try {
                    if (!exchange.getRequestMethod().equals("POST")) {
                        exchange.sendResponseHeaders(405, 0);
                        return;
                    }

                    String message = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    System.out.println("Received validation message: " + message);

                    handleReceivedValidationMessage(message);
                    exchange.sendResponseHeaders(200, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } catch (Exception e) {
                    System.err.println("Error handling validation message: " + e.getMessage());
                    exchange.sendResponseHeaders(500, e.getMessage().length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(e.getMessage().getBytes());
                    }
                } finally {
                    exchange.close();
                }
            });

            server.start();
            System.out.println("Validation server started on port " + 8081);
        } catch (IOException e) {
            System.err.println("Failed to start validation server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleReceivedValidationMessage(String message) {
        try {
            ValidationMessage validationMessage = new ObjectMapper().readValue(message, ValidationMessage.class);
            System.out.println("Processing validation message for transaction: " + validationMessage.getTransactionId());

            if (!"validated".equals(validationMessage.getStatus())) {
                System.err.println("Invalid validation status: " + validationMessage.getStatus());
                return;
            }

            Transaction transaction = transactionDAO.getTransactionById(validationMessage.getTransactionId());
            if (transaction == null) {
                System.err.println("Transaction not found: " + validationMessage.getTransactionId());
                return;
            }

            Platform.runLater(() -> {
                addValidatorVoteForTransaction(transaction);
                System.out.println("Validation vote added for transaction: " + validationMessage.getTransactionId());
            });
        } catch (IOException e) {
            System.err.println("Failed to process validation message: " + e.getMessage());
            e.printStackTrace();
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