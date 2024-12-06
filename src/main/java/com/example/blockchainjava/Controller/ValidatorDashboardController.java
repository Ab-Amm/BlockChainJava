package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.Block.Block;
import com.example.blockchainjava.Model.Block.BlockChain;
import com.example.blockchainjava.Model.DAO.TransactionDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.Client;
import com.example.blockchainjava.Model.User.Session;
import com.example.blockchainjava.Model.User.User;
import com.example.blockchainjava.Model.User.Validator;
import com.example.blockchainjava.Util.Network.BlockMessage;
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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

//import static com.example.blockchainjava.Model.DAO.DatabaseConnection.connection;

public class ValidatorDashboardController implements BlockchainUpdateObserver {
    private UserDAO userDAO;
    private TransactionDAO transactionDAO;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final Map<Integer, List<Validator>> transactionValidatorVotes = new ConcurrentHashMap<>();
    private final Map<Integer, LocalDateTime> validationTimestamps = new HashMap<>();

    private final Connection connection;

    @FXML
    private TableView<Block> blockTable;

    @FXML
    private TableColumn<Block, Integer> blockIdColumn;
    @FXML
    private TableColumn<Block, String> previousHashColumn;
    @FXML
    private TableColumn<Block, String> currentHashColumn;
    @FXML
    private TableColumn<Block, Integer> transactionIdColumn;
    @FXML
    private TableColumn<Block, Double> transactionAmountColumn;
    @FXML
    private TableColumn<Block, String> validatorSignatureColumn;
    @FXML
    private TableColumn<Block, String> timestampColumn;

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

    @FXML
    private Label connectedValidatorsLabel;

    @FXML
    private Label connectedClientsLabel;

    private final ScheduledExecutorService connectionUpdateExecutor = Executors.newSingleThreadScheduledExecutor();

    private Validator validator;
    private BlockChain blockchain;
    private static final int VALIDATION_PORT = 8081;
    private static final int SERVER_PORT = 8080;

    public ValidatorDashboardController() throws NoSuchAlgorithmException {
        this.userDAO = new UserDAO();
        this.transactionDAO = new TransactionDAO();// Initialisation du DAO
        this.blockchain = new BlockChain();
        User currentUser = Session.getCurrentUser();
        if (currentUser != null) {
            int Id = currentUser.getId(); // Get the username from the current user;
            this.validator = userDAO.getValidatorFromDatabase(Id);
            System.out.println("voici cle prive de validator");
            System.out.println(validator.getPrivateKey());
        }else {
            System.err.println("No user is currently logged in.");
        }
        this.connection = DatabaseConnection.getConnection();
        System.out.println("this is the validator connected to this dashboard: " + validator);
        this.validator = userDAO.getValidatorFromDatabase(currentUser.getId());

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (validator != null) {
                userDAO.updateUserConnection(validator.getId(), false);
            }
        }));
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

            blockIdColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().idProperty()));
            previousHashColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().previousHashProperty()));
            currentHashColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().currentHashProperty()));
            transactionIdColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().transactionIdProperty()));
            transactionAmountColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().transactionAmountProperty()));
            validatorSignatureColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().validatorSignatureProperty()));
            timestampColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().timestampProperty()));

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

            // Mark this validator as connected
            userDAO.updateUserConnection(validator.getId(), true);

            // Add window closing event handler
            Platform.runLater(() -> {
                Stage stage = (Stage) pendingTransactionsTable.getScene().getWindow();
                stage.setOnCloseRequest(event -> {
                    stop();
                });
            });

            // Schedule periodic updates of connection counts using milliseconds
            connectionUpdateExecutor.scheduleAtFixedRate(
                this::updateConnectionCounts,
                0, // initial delay
                5000, // period (5 seconds in milliseconds)
                java.util.concurrent.TimeUnit.MILLISECONDS
            );

            // Schedule cleanup of inactive connections
            connectionUpdateExecutor.scheduleAtFixedRate(
                () -> userDAO.cleanupInactiveConnections(Duration.ofMinutes(5)),
                1, // initial delay
                5, // period
                java.util.concurrent.TimeUnit.MINUTES
            );
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
                System.out.println("Data used for validation: " + selectedTransaction.getDataToSign());
                if (!isSignatureValid) {
                    throw new SecurityException("Invalid signature.");
                }

                Client sender = getUserById(selectedTransaction.senderIdProperty());
                System.out.println("this is the sender : ");
                System.out.println(sender);
                if (sender == null || sender.getBalance() < selectedTransaction.getAmount()) {
                    throw new IllegalArgumentException("Invalid sender or insufficient balance.");
                }

                addValidatorVoteForTransaction(selectedTransaction , this.validator);
                notifyValidatorsOfValidation(selectedTransaction);

                Platform.runLater(() -> {
                    pendingTransactionsList.remove(selectedTransaction);
                    pendingTransactionsTable.refresh(); // Facultatif, mais utile pour forcer une mise à jour visuelle
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Validation Error", e.getMessage()));
            }
        }, executorService);
    }

    private boolean isTransactionValidatedByAllValidators(Transaction transaction) {
        List<Validator> validatorsVoted = transactionValidatorVotes.get(transaction.getId());

        if (validatorsVoted == null) {
            System.out.println("No validators have voted for transaction " + transaction.getId());
            return false;
        }

        boolean hasEnoughVotes = validatorsVoted.size() >= getRequiredValidatorCount();
        System.out.println("\n=== VALIDATION CHECK ===");
        System.out.println("Transaction ID: " + transaction.getId());
        System.out.println("Current votes: " + validatorsVoted.size());
        System.out.println("Required votes: " + getRequiredValidatorCount());
        System.out.println("Has enough votes: " + hasEnoughVotes);
        if (hasEnoughVotes) {
            System.out.println("\nValidating validators:");
            validatorsVoted.forEach(v -> {
                System.out.println("- Validator " + v.getId() + " (" + v.getIpAddress() + ")");
            });
        }
        System.out.println("=====================\n");

        return hasEnoughVotes;
    }
    private int getRequiredValidatorCount() {
        return 2; // Could be made configurable in the future
    }

    private void addValidatorVoteForTransaction(Transaction transaction, Validator sourceValidator) {
        int transactionId = transaction.getId();

        transactionValidatorVotes.putIfAbsent(transactionId, new ArrayList<>());
        List<Validator> validators = transactionValidatorVotes.get(transactionId);

        boolean hasAlreadyVoted = validators.stream()
                .anyMatch(v -> v.getId() == sourceValidator.getId());

        if (!hasAlreadyVoted) {
            validators.add(sourceValidator);
            logValidatorVote(transactionId, sourceValidator, validators.size());
            logCurrentValidationStatus(transactionId);

            // Record validation timestamp for this validator
            validationTimestamps.put(sourceValidator.getId(), LocalDateTime.now());

            if (isTransactionValidatedByAllValidators(transaction)) {
                System.out.println("\n=== VALIDATION COMPLETE ===");
                System.out.println("Transaction " + transactionId + " has reached required validator count ("
                        + getRequiredValidatorCount() + ")");
                System.out.println("Adding transaction to blockchain...");
                System.out.println("==========================\n");

                // Only the validator with the latest timestamp creates the block
                if (shouldCreateBlock(validators)) {
                    addTransactionToBlockchain(transaction);
                } else {
                    System.out.println("Block will be created by validator with latest timestamp");
                    updateBlockchainView();
                }
            } else {
                int remainingValidators = getRequiredValidatorCount() - validators.size();
                System.out.println("\n=== VALIDATION IN PROGRESS ===");
                System.out.println("Waiting for " + remainingValidators + " more validator(s) for transaction "
                        + transactionId);
                System.out.println("============================\n");
            }
        } else {
            System.out.println("\n=== DUPLICATE VALIDATION ATTEMPT ===");
            System.out.println("Validator " + sourceValidator.getId() + " (" + sourceValidator.getIpAddress() +
                    ") has already validated transaction " + transactionId);
            System.out.println("================================\n");
        }
    }

    private boolean shouldCreateBlock(List<Validator> validators) {
        // Get current validator's ID
        int currentValidatorId = this.validator.getId();

        // Get the latest validation timestamp
        LocalDateTime latestTimestamp = validationTimestamps.values().stream()
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // Check if current validator was the last to validate
        boolean isLastValidator = latestTimestamp != null &&
                validationTimestamps.get(currentValidatorId) != null &&
                validationTimestamps.get(currentValidatorId).equals(latestTimestamp);

        System.out.println("Current validator ID: " + currentValidatorId);
        System.out.println("Is last validator to validate: " + isLastValidator);

        return isLastValidator;
    }

    private void logValidatorVote(int transactionId, Validator validator, int currentVoteCount) {
        System.out.println("\n=== NEW VALIDATOR VOTE (#" + currentVoteCount + ") ===");
        System.out.println("Transaction ID: " + transactionId);
        System.out.println("Validator ID: " + validator.getId());
        System.out.println("Validator IP: " + validator.getIpAddress());
        System.out.println("Vote Time: " + LocalDateTime.now());
        System.out.println("===============================\n");
    }

    private void logCurrentValidationStatus(int transactionId) {
        List<Validator> validators = transactionValidatorVotes.get(transactionId);
        System.out.println("\n=== CURRENT VALIDATION STATUS ===");
        System.out.println("Transaction ID: " + transactionId);
        System.out.println("Current validator count: " + validators.size());
        System.out.println("Required validator count: " + getRequiredValidatorCount());
        System.out.println("\nValidators who have voted:");
        validators.forEach(v -> {
            System.out.println("- Validator " + v.getId() + " (" + v.getIpAddress() + ")");
        });
        System.out.println("================================\n");
    }

    private void addTransactionToBlockchain(Transaction transaction) {
        try {
            System.out.println("Adding transaction " + transaction.getId() + " to the blockchain...");
            User currentUser = Session.getCurrentUser();
            this.validator = userDAO.getValidatorFromDatabase(currentUser.getId());
            String signature = validator.sign(transaction , this.validator);
            System.out.println("Signature: " + signature);
            blockchain.addBlock(transaction, signature);
            updateBlockchainView();
            System.out.println("Transaction " + transaction.getId() + " successfully added to the blockchain.");

            // Broadcast the new block to other validators
//            broadcastNewBlock(transaction, signature);
            updateBlockchainView();

            // Clean up the validation votes for this transaction
            transactionValidatorVotes.remove(transaction.getId());
        } catch (Exception e) {
            System.err.println("Failed to add transaction to blockchain: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void broadcastNewBlock(Transaction transaction, String signature) {
        // Create a message containing block information
        BlockMessage blockMessage = new BlockMessage(
                transaction.getId(),
                signature,
                validator.getId()
        );

        try {
            String message = new ObjectMapper().writeValueAsString(blockMessage);
            List<Validator> validators = getOtherValidators();

            for (Validator v : validators) {
                if (!isValidatorAvailable(v)) {
                    System.out.println("Skipping offline validator for block broadcast: " + v.getId());
                    continue;
                }

                CompletableFuture.runAsync(() -> {
                    try {
                        sendBlockToValidator(v, message);
                    } catch (Exception e) {
                        System.err.println("Failed to send block to validator " + v.getId() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }, executorService);
            }
        } catch (JsonProcessingException e) {
            System.err.println("Failed to create block message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendBlockToValidator(Validator validator, String message) {
        if (!testValidatorConnectivity(validator)) {
            throw new RuntimeException("Cannot establish basic network connection to validator");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String validatorUrl = "http://" + validator.getIpAddress() + ":" + VALIDATION_PORT + "/block";
        System.out.println("Sending block to validator at: " + validatorUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(validatorUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(message))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Block broadcast response from validator " + validator.getId() + ": " +
                    "Status=" + response.statusCode() + ", Body=" + response.body());

            if (response.statusCode() != 200) {
                throw new IOException("Invalid response: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Failed to send block to validator at " + validatorUrl + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send block to validator", e);
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
            if (!isValidatorAvailable(v)) {
                System.out.println("Skipping offline validator: " + v.getId());
                continue;
            }

            CompletableFuture.runAsync(() -> {
                try {
                    sendMessageToValidator(v, message);
                } catch (Exception e) {
                    System.err.println("Failed to notify validator " + v.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }, executorService);
        }
    }

    private boolean isValidatorAvailable(Validator validator) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + validator.getIpAddress() + ":" + VALIDATION_PORT + "/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Validator " + validator.getId() + " is not available: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean testValidatorConnectivity(Validator validator) {
        try {
            // Try a basic Socket connection first to test network reachability
            try (Socket socket = new Socket()) {
                // Set a shorter timeout for the connection test
                socket.connect(new InetSocketAddress(validator.getIpAddress(), VALIDATION_PORT), 5000);
                System.out.println("Basic connectivity test successful for validator " + validator.getId() +
                        " at " + validator.getIpAddress() + ":" + VALIDATION_PORT);
                return true;
            }
        } catch (Exception e) {
            System.out.println("Connection test failed for validator " + validator.getId() +
                    " at " + validator.getIpAddress() + ":" + VALIDATION_PORT +
                    ". Error: " + e.getMessage());

            // Print local network information for debugging
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                System.out.println("Local host: " + localHost.getHostAddress());

                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    if (ni.isUp() && !ni.isLoopback()) {
                        Enumeration<InetAddress> addresses = ni.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress addr = addresses.nextElement();
                            if (addr instanceof Inet4Address) {
                                System.out.println("Available network interface: " + addr.getHostAddress());
                            }
                        }
                    }
                }
            } catch (Exception ne) {
                System.out.println("Could not print network interfaces: " + ne.getMessage());
            }

            return false;
        }
    }

    private void sendMessageToValidator(Validator validator, String message) {
        if (!testValidatorConnectivity(validator)) {
            throw new RuntimeException("Cannot establish basic network connection to validator");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String validatorUrl = "http://" + validator.getIpAddress() + ":" + VALIDATION_PORT + "/validate";
        System.out.println("Attempting to send message to validator at: " + validatorUrl);
        System.out.println("Sending validation message: " + message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(validatorUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(message))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response from validator " + validator.getId() + ": " +
                    "Status=" + response.statusCode() + ", Body=" + response.body());

            if (response.statusCode() != 200) {
                throw new IOException("Invalid response: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Failed to send message to validator at " + validatorUrl + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send message to validator", e);
        }
    }

    private void listenForValidationMessages() {
        try {
            // Bind to 0.0.0.0 instead of localhost to accept connections from any interface
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", VALIDATION_PORT), 0);
            server.setExecutor(executorService);

            // Add health check endpoint
            server.createContext("/health", exchange -> {
                if (!exchange.getRequestMethod().equals("GET")) {
                    exchange.sendResponseHeaders(405, 0);
                    return;
                }
                String response = "OK";
                exchange.getResponseHeaders().add("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                exchange.close();
            });

            // Validation endpoint
            server.createContext("/validate", exchange -> {
                System.out.println("Received validation request");
                System.out.println("Received request method: " + exchange.getRequestMethod());
                System.out.println("Received headers: " + exchange.getRequestHeaders());

                String response = "Validation received";
                try {
                    if (!exchange.getRequestMethod().equals("POST")) {
                        exchange.sendResponseHeaders(405, 0);
                        return;
                    }

                    // Log more connection details
                    InetSocketAddress remoteAddress = exchange.getRemoteAddress();
                    System.out.println("Received validation request from: " + remoteAddress.getAddress().getHostAddress() +
                            ":" + remoteAddress.getPort());

                    // Read the message body only once
                    String message = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    System.out.println("Received validation message: " + message);

                    handleReceivedValidationMessage(message);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
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
            // Print all bound addresses for verification
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address) {
                            System.out.println("Server listening on: " + addr.getHostAddress() + ":" + VALIDATION_PORT);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start validation server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleReceivedValidationMessage(String message) {
        try {
            if (message == null || message.isEmpty()) {
                throw new IllegalArgumentException("Empty validation message received");
            }

            ValidationMessage validationMessage = new ObjectMapper().readValue(message, ValidationMessage.class);
            System.out.println("\n=== RECEIVED VALIDATION MESSAGE ===");
            System.out.println("Transaction ID: " + validationMessage.getTransactionId());
            System.out.println("From Validator ID: " + validationMessage.getValidatorId());
            System.out.println("Status: " + validationMessage.getStatus());
            System.out.println("================================\n");

            if (!"validated".equals(validationMessage.getStatus())) {
                System.err.println("Invalid validation status: " + validationMessage.getStatus());
                return;
            }

            Transaction transaction = transactionDAO.getTransactionById(validationMessage.getTransactionId());
            if (transaction == null) {
                System.err.println("Transaction not found: " + validationMessage.getTransactionId());
                return;
            }

            // Use getOtherValidators to find the validator
            Validator sourceValidator = findValidatorFromList(validationMessage.getValidatorId());
            if (sourceValidator == null) {
                System.err.println("Validator not found: " + validationMessage.getValidatorId());
                return;
            }

            Platform.runLater(() -> {
                addValidatorVoteForTransaction(transaction, sourceValidator);
            });
        } catch (IOException e) {
            System.err.println("Failed to process validation message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Validator findValidatorFromList(int validatorId) {
        List<Validator> allValidators = getOtherValidators();
        return allValidators.stream()
                .filter(v -> v.getId() == validatorId)
                .findFirst()
                .orElse(null);
    }

    private Validator findValidatorById(int validatorId , int transactionId) {
        // Supposons que vous avez une liste de validateurs (validatorsList)
        List<Validator> validators = transactionValidatorVotes.get(transactionId);
        return validators.stream()
                .filter(v -> v.getId() == validatorId)
                .findFirst()
                .orElse(null);
    }

    private String getSenderPublicKey(String senderId) {
        // Retrieve the sender's public key from the user database
        Client sender = getUserById(Integer.valueOf(senderId));
        System.out.println("voici le le cle public de sender :");
        System.out.println(sender.getPublicKey());
        return sender != null ? sender.getPublicKey() : null;
    }
    private Client getUserById(int userId) {
        return userDAO.getClientFromDatabase(userId); // Adjust this based on your actual DAO implementation
    }
    @FXML
    private void updateClientBalance() throws Exception {
        // Récupération des données de l'interface utilisateur
        String clientIdText = clientIdField.getText();
        String balanceText = clientBalanceField.getText();

        if (clientIdText.isEmpty() || balanceText.isEmpty()) {
            showError("Input Error", "Both fields must be filled.");
            return;
        }

        int clientId;
        double newBalance;

        try {
            clientId = Integer.parseInt(clientIdText);
            newBalance = Double.parseDouble(balanceText);
        } catch (NumberFormatException e) {
            showError("Input Error", "Invalid input format. Please enter valid numbers.");
            return;
        }

        // Validation du solde
        if (newBalance < 0) {
            showError("Update Error", "Balance cannot be negative.");
            return;
        }

        // Trouver l'utilisateur avec clientId
        Client user = userDAO.getClientFromDatabase(clientId); // Utiliser le DAO pour trouver l'utilisateur
        if (user == null) {
            showError("Update Error", "User not found.");
            return;
        }

        // Vérification du solde de l'administrateur
        User currentUser = Session.getCurrentUser();
        this.validator = userDAO.getValidatorFromDatabase(currentUser.getId());
        double adjustmentAmount = newBalance - user.getBalance();

        if (adjustmentAmount > validator.getBalance()) {
            showError("Transaction Error", "Insufficient balance in the administrator's account.");
            return;
        }

        // Vérification du solde du validateur
        if (adjustmentAmount < 0) {
            // If the adjustment is negative (client balance decreases), check if the validator has enough funds
            if (Math.abs(adjustmentAmount) > validator.getBalance()) {
                showError("Transaction Error", "Insufficient balance in the validator's account.");
                return;
            }
        }

        // Création de la transaction
        Transaction transaction = new Transaction();
        transaction.setSenderId(validator.getId());
        transaction.setReceiverKey(user.getPublicKey()); // Clé publique de l'utilisateur (récepteur)
        transaction.setAmount(adjustmentAmount);
        transaction.setStatus(TransactionStatus.VALIDATED);
        String signature = validator.sign(transaction , this.validator);
        transaction.setSignature(signature);
        transactionDAO.saveTransaction(transaction);

        blockchain.addBlock(transaction, signature);


            user.setBalance(newBalance);
            userDAO.updateUserBalance(user, newBalance);

            // Si l'ajustement est négatif, soustraire du solde du validateur
            if (adjustmentAmount < 0) {
                double newValidatorBalance = validator.getBalance() + Math.abs(adjustmentAmount);
                validator.setBalance(newValidatorBalance);
                userDAO.updateValidatorBalance(validator, newValidatorBalance); // Mise à jour du solde du validateur
            }

            // Afficher un message de succès
            showSuccess("Balance updated successfully.", "The balance has been updated and the transaction is complete.");
            updateUserTableView(); // Actualisation de la table des utilisateurs

    }

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

    private void updateConnectionCounts() {
        try {
            int validatorCount = userDAO.getConnectedValidatorsCount();
            int clientCount = userDAO.getConnectedClientsCount();

            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                connectedValidatorsLabel.setText("Connected Validators: " + validatorCount);
                connectedClientsLabel.setText("Connected Clients: " + clientCount);
            });
        } catch (Exception e) {
            System.err.println("Error updating connection counts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            // Mark this validator as disconnected
            if (validator != null) {
                System.out.println("Marking validator " + validator.getId() + " as disconnected");
                userDAO.updateUserConnection(validator.getId(), false);
            }
            
            // Shutdown executor services
            if (connectionUpdateExecutor != null) {
                connectionUpdateExecutor.shutdown();
                if (!connectionUpdateExecutor.awaitTermination(5000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    connectionUpdateExecutor.shutdownNow();
                }
            }
            
            if (executorService != null) {
                executorService.shutdown();
                if (!executorService.awaitTermination(5000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            }
            
            // Close database connections
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted while shutting down executor services: " + e.getMessage());
            if (connectionUpdateExecutor != null) {
                connectionUpdateExecutor.shutdownNow();
            }
            if (executorService != null) {
                executorService.shutdownNow();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
}