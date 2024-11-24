package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.Block.Block;
import com.example.blockchainjava.Model.Block.BlockChain;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.User.Client;
import com.example.blockchainjava.Model.User.User;
import com.example.blockchainjava.Model.User.Validator;
import com.example.blockchainjava.Util.Network.SocketServer;
import com.example.blockchainjava.Observer.BlockchainUpdateObserver;
import com.example.blockchainjava.Model.DAO.UserDAO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ValidatorDashboardController implements BlockchainUpdateObserver {
    private UserDAO userDAO;

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


    private Validator validator;
    private BlockChain blockchain;
    private SocketServer socketServer;

    public ValidatorDashboardController() throws NoSuchAlgorithmException {
        this.userDAO = new UserDAO(); // Initialisation du DAO
        this.blockchain = new BlockChain();
        this.validator = new Validator();
    }

    @FXML
    public void initialize() {
        try {
            id.setCellValueFactory(cellData -> cellData.getValue().getIdProperty().asObject());
            clientNameColumn.setCellValueFactory(cellData -> cellData.getValue().getNameProperty());
            clientBalanceColumn.setCellValueFactory(cellData -> cellData.getValue().getBalanceProperty().asObject());

            if (blockchain == null || validator == null) {
                System.out.println("Warning: Blockchain and Validator are not initialized.");
                return;
            }

            blockchain.addObserver(this);


            new Thread(() -> startSocketServer(8080)).start();

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

                // Lancer un nouveau thread pour chaque connexion client
                new Thread(() -> {
                    try (InputStream inputStream = clientSocket.getInputStream()) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            dataStream.write(buffer, 0, bytesRead);
                        }

                        String transactionJson = dataStream.toString("UTF-8").trim();
                        if (transactionJson.isBlank()) {
                            throw new IllegalArgumentException("Received empty JSON data from client.");
                        }

                        if (transactionJson.startsWith("Sending transaction JSON:")) {
                            transactionJson = transactionJson.substring("Sending transaction JSON:".length()).trim();
                        }

                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.readTree(transactionJson); // Validation du JSON

                        Transaction transaction = objectMapper.readValue(transactionJson, Transaction.class);
                        System.out.println("Transaction parsed: " + transaction);

                        // Ajoutez la transaction à la liste des transactions en attente
                        //blockchain.addPendingTransaction(transaction);

                        // Mise à jour de l'interface utilisateur
                        Platform.runLater(this::updatePendingTransactionsTable);

                    } catch (Exception e) {
                        System.err.println("Error processing client request: " + e.getMessage());
                        e.printStackTrace();
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
        if (selectedTransaction != null && blockchain != null && validator != null) {
            try {
                String signature = validator.sign(selectedTransaction);
                blockchain.addBlock(selectedTransaction, signature);
                updateBlockchainView();
            } catch (Exception e) {
                showError("Validation Error", "Failed to validate transaction: " + e.getMessage());
            }
        } else {
            showError("Validation Error", "No transaction selected or components not initialized.");
        }
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
    private void updatePendingTransactionsTable() {
        if (blockchain != null) {
            ObservableList<Transaction> pendingTransactionsList = FXCollections.observableArrayList(blockchain.getPendingTransactions());
            pendingTransactionsTable.setItems(pendingTransactionsList);
        }
    }


}