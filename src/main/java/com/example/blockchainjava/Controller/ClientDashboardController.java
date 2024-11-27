package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.DAO.TransactionDAO; // Classe pour récupérer les transactions depuis la base de données.
import com.example.blockchainjava.Model.User.Client;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientDashboardController {

    @FXML
    private Label usernameLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private TableView<Transaction> transactionTable;

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

    private Map<Integer, String> receiverUsernameMap = new HashMap<>();
    private TransactionDAO transactionDAO; // Classe DAO pour accéder aux transactions
    private ObservableList<Transaction> transactionsList;

    private Client client;

    public ClientDashboardController() {
        this.transactionDAO = new TransactionDAO(); // Initialisation du DAO
        this.transactionsList = FXCollections.observableArrayList();
    }
    @FXML
    public void initialize() {
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


}
