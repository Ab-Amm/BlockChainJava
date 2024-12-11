package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.DAO.DatabaseConnection;
import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.DAO.TransactionDAO; // Classe pour récupérer les transactions depuis la base de données.
import com.example.blockchainjava.Model.User.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.example.blockchainjava.Model.DAO.DatabaseConnection;
import javafx.stage.Stage;

public class ClientDashboardController {
    private UserDAO userDAO;
    @FXML
    private Label usernameLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private TableView<Transaction> transactionTable;
    ScheduledExecutorService dashboardUpdateExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService balanceUpdateExecutor = Executors.newSingleThreadScheduledExecutor();

    @FXML
    private TableColumn<Transaction, Integer> transactionIdColumn;

    @FXML
    private TableColumn<Transaction, String> senderReceiverColumn;

    @FXML
    private TableColumn<Transaction, Double> amountColumn;

    @FXML
    private TableColumn<Transaction, String> statusColumn;

    @FXML
    private TableColumn<Transaction, String> dateColumn;
    @FXML
    private TableColumn<Transaction, String> receiverColumn;
    @FXML
    private TableView<Transaction> transactionTable2;

    @FXML
    private TableColumn<Transaction, Integer> transactionIdColumn2;

    @FXML
    private TableColumn<Transaction, String> senderColumn;

    @FXML
    private TableColumn<Transaction, Double> amountColumn2;

    @FXML
    private TableColumn<Transaction, String> dateColumn2;

    private ObservableList<Transaction> receivedTransactionsList;


    private Map<Integer, String> receiverUsernameMap = new HashMap<>();
    private Map<Integer, String> senderUsernameMap = new HashMap<>();
    private TransactionDAO transactionDAO;
    private ObservableList<Transaction> transactionsList;
    private final Connection connection;
    private Client client;

    public ClientDashboardController() {
        this.userDAO = new UserDAO();
        this.transactionDAO = new TransactionDAO(); // Initialisation du DAO
        this.transactionsList = FXCollections.observableArrayList();
        User currentUser = Session.getCurrentUser();
        if (currentUser != null) {
            int Id = currentUser.getId();// Get the username from the current user;
            System.out.println(Id);
            this.client = userDAO.getClientFromDatabase(Id);
        }else {
            System.err.println("No user is currently logged in.");
        }
        this.connection = DatabaseConnection.getConnection();
        System.out.println("this is the CLIENT connected to this dashboard: " + client);
        this.client = userDAO.getClientFromDatabase(currentUser.getId());

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (client != null) {
                userDAO.updateUserConnection(client.getId(), false);
            }
        }));
    }
    @FXML
    public void initialize() {
        User currentUser = Session.getCurrentUser();
        this.client = userDAO.getClientFromDatabase(currentUser.getId());
        System.out.println(client.getId());
        userDAO.updateUserConnection(client.getId(), true);
        receiverColumn.setCellValueFactory(cellData -> {
            Transaction transaction = cellData.getValue();

            // Obtenir les données supplémentaires dans la liste observable
            String receiverUsername = getReceiverUsernameFromTransaction(transaction);

            // Retourner une propriété observable
            return new SimpleStringProperty(receiverUsername != null ? receiverUsername : "N/A");
        });


        // Associer les colonnes aux propriétés du modèle Transaction
        transactionIdColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        //senderReceiverColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getSenderId())));
        amountColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());
        statusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().toString()));
        dateColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCreatedAt() != null
                        ? cellData.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : "N/A"
        ));

        // Charger les transactions dans le tableau
        loadTransactionHistory();
        Platform.runLater(() -> {
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            stage.setOnCloseRequest(event -> stop());
        });
        balanceUpdateExecutor.scheduleAtFixedRate(() -> {
            Platform.runLater(this::updateBalance); // Met à jour l'interface utilisateur sur le thread JavaFX
        }, 0, 2, TimeUnit.SECONDS); // Vérifier toutes les 2 secondes

        Platform.runLater(() -> {
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            stage.setOnCloseRequest(event -> stop());
        });

        dashboardUpdateExecutor.scheduleAtFixedRate(() -> {
            Platform.runLater(this::updateDashboard); // Met à jour l'interface utilisateur sur le thread JavaFX
        }, 0, 5, TimeUnit.SECONDS); // Unité de temps : secondes

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (client != null) {
                userDAO.updateUserConnection(client.getId(), false);
            }
        }));
        receivedTransactionsList = FXCollections.observableArrayList();

        transactionIdColumn2.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        amountColumn2.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());
        dateColumn2.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCreatedAt() != null
                        ? cellData.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : "N/A"
        ));
        senderColumn.setCellValueFactory(cellData -> {
            Transaction transaction = cellData.getValue();

            // Obtenir les données supplémentaires dans la liste observable
            String senderUsername = getsenderUsernameFromTransaction(transaction);

            // Retourner une propriété observable
            return new SimpleStringProperty(senderUsername != null ? senderUsername : "N/A");
        });
// Charger les transactions reçues
        loadReceivedTransactionHistory();
    }


    private void updateBalance() {
        if (client != null) {
            // Récupérer la balance du client depuis la base de données
            double currentBalance = userDAO.getClientBalance(client.getId());

            // Mettre à jour la balance affichée dans l'interface utilisateur si elle a changé
            Platform.runLater(() -> {
                if (client.getBalance() != currentBalance) {
                    client.setBalance(currentBalance);
                    balanceLabel.setText(String.format("$%.2f", currentBalance));
                }
            });
        }
    }
    /**
     * Définit le client actuel et met à jour les informations du tableau de bord.
     *
     * @param client le client dont les informations doivent être affichées.
     */
    public void setClient(Client client) {
        this.client = client;
        updateDashboard();
        loadTransactionHistory(); // Charger l'historique spécifique au client
    }

    /**
     * Met à jour les informations affichées dans le tableau de bord du client.
     */
    private void updateDashboard() {
        if (client != null) {
            usernameLabel.setText(client.getUsername() != null ? client.getUsername() : "Unknown User");
            balanceLabel.setText(String.format("$%.2f", client.getBalance()));
        } else {
            usernameLabel.setText("No User Connected");
            balanceLabel.setText("$0.00");
        }
    }

    /**
     * Charge l'historique des transactions du client actuel.
     */

    private String getReceiverUsernameFromTransaction(Transaction transaction) {
        return receiverUsernameMap.getOrDefault(transaction.getId(), "N/A");
    }
    private String getsenderUsernameFromTransaction(Transaction transaction) {
        return senderUsernameMap.getOrDefault(transaction.getId(), "N/A");
    }


    private void loadTransactionHistory() {
        if (client == null) {
            transactionsList.clear();
            transactionTable.setItems(transactionsList);
            return;
        }

        try {
            // Récupérer les transactions du client depuis la base de données
            List<Transaction> transactions = transactionDAO.getTransactionsByClient(client.getId());

            // Construire un map pour stocker les receiver_username
            for (Transaction transaction : transactions) {
                int transactionId = transaction.getId();
                String receiverUsername = transactionDAO.getReceiverUsername(transactionId);
                receiverUsernameMap.put(transactionId, receiverUsername);
            }

            // Ajouter les transactions à la liste observable
            transactionsList.setAll(transactions);

            // Lier la liste observable au tableau
            transactionTable.setItems(transactionsList);
        } catch (Exception e) {
            System.err.println("Error loading transaction history: " + e.getMessage());
        }
    }
    private void loadReceivedTransactionHistory() {
        if (client == null) {
            receivedTransactionsList.clear();
            transactionTable2.setItems(receivedTransactionsList);
            return;
        }

        try {
            // Récupérer les transactions reçues par le client depuis la base de données
            List<Transaction> receivedTransactions = transactionDAO.getReceivedTransactionsByClient(client.getPublicKey());

            for (Transaction transaction : receivedTransactions) {
                int transactionId = transaction.getId();
                String senderUsername = transactionDAO.getsenderUsername(transactionId);
                senderUsernameMap.put(transactionId, senderUsername);
            }
            // Ajouter les transactions à la liste observable
            receivedTransactionsList.setAll(receivedTransactions);

            // Lier la liste observable au tableau
            transactionTable2.setItems(receivedTransactionsList);
        } catch (Exception e) {
            System.err.println("Error loading received transaction history: " + e.getMessage());
        }
    }
    public void stop() {

        try {
            if (connection != null && !connection.isClosed()) {
                // Marquer le client comme déconnecté
                if (client != null) {
                    System.out.println("Marking client " + client.getId() + " as disconnected");
                    userDAO.updateUserConnection(client.getId(), false);
                }
            } else {
                System.err.println("Connection is already closed. Cannot update user connection status.");
            }
        } catch (SQLException e) {
            System.err.println("Error checking connection status: " + e.getMessage());
        }

    }

}
