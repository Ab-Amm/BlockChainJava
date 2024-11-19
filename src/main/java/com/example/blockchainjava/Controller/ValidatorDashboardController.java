package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.Block.Block;
import com.example.blockchainjava.Model.Block.BlockChain;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.User.Validator;
import com.example.blockchainjava.Util.Network.SocketServer;
import com.example.blockchainjava.Observer.BlockchainUpdateObserver;

import javafx.scene.control.Alert;
import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

public class ValidatorDashboardController implements BlockchainUpdateObserver {

    @FXML
    private TableView<Block> blockTable; // Table des blocs dans la blockchain
    @FXML
    private TableView<Transaction> pendingTransactionsTable; // Table des transactions en attente

    private Validator validator; // Le validateur (utilisateur actuel)
    private BlockChain blockchain; // La blockchain associée
    private SocketServer socketServer; // Serveur réseau pour gérer les communications

    public ValidatorDashboardController() {
        // Constructeur sans argument requis pour le chargement FXML
    }

    /**
     * Méthode appelée automatiquement après le chargement de la vue FXML.
     */
    @FXML
    public void initialize() {
        try {
            // Vérification que la blockchain et le validateur sont initialisés
            // Note : Assurez-vous que `blockchain` et `validator` sont définis avant d'appeler `initialize`.
            // Si cette condition n'est pas respectée, certaines fonctionnalités ne fonctionneront pas correctement.
            if (blockchain == null || validator == null) {
                // throw new IllegalStateException("Blockchain and Validator must be initialized before calling initialize.");
                System.out.println("Warning: Blockchain and Validator are not initialized. Some features may not work.");
                return; // Ne pas poursuivre l'initialisation si les dépendances ne sont pas prêtes
            }

            // Ajouter un observateur pour écouter les mises à jour de la blockchain
            blockchain.addObserver(this);

            // Démarrer un serveur socket pour gérer les communications
            ServerSocket serverSocket = new ServerSocket(8080);
            socketServer = new SocketServer(blockchain, serverSocket, validator);
            new Thread(socketServer::start).start();

            // Mettre à jour la vue avec les données actuelles
            updateBlockchainView();

        } catch (IOException e) {
            showError("Error initializing Validator Dashboard", "Failed to start socket server: " + e.getMessage());
        }
    }

    /**
     * Méthode appelée lorsqu'une mise à jour de la blockchain est détectée.
     *
     * @param updatedBlockchain La blockchain mise à jour.
     */
    @Override
    public void onBlockchainUpdate(BlockChain updatedBlockchain) {
        this.blockchain = updatedBlockchain; // Mettre à jour la blockchain locale
        updateBlockchainView(); // Mettre à jour les vues
    }

    /**
     * Met à jour la vue des blocs et des transactions en attente.
     */
    private void updateBlockchainView() {
        if (blockchain != null) {
            // Mettre à jour la table des blocs
            List<Block> blocks = blockchain.getBlocks();
            ObservableList<Block> blockList = FXCollections.observableArrayList(blocks);
            blockTable.setItems(blockList);

            // Mettre à jour la table des transactions en attente
            List<Transaction> pendingTransactions = blockchain.getPendingTransactions();
            ObservableList<Transaction> pendingTransactionsList = FXCollections.observableArrayList(pendingTransactions);
            pendingTransactionsTable.setItems(pendingTransactionsList);
        }
    }

    /**
     * Valide une transaction sélectionnée dans la table des transactions en attente.
     */
    @FXML
    private void validateTransaction() {
        Transaction selectedTransaction = pendingTransactionsTable.getSelectionModel().getSelectedItem();
        if (selectedTransaction != null && blockchain != null && validator != null) {
            try {
                // Signer la transaction et l'ajouter à la blockchain
                String signature = validator.sign(selectedTransaction);
                blockchain.addBlock(selectedTransaction, signature);

                // Mettre à jour la vue après la validation
                updateBlockchainView();

            } catch (Exception e) {
                showError("Validation Error", "Failed to validate transaction: " + e.getMessage());
            }
        } else {
            showError("Validation Error", "No transaction selected or components not initialized.");
        }
    }

    /**
     * Définit le validateur (utilisateur actuel).
     *
     * @param validator L'instance du validateur.
     */
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Définit la blockchain associée.
     *
     * @param blockchain L'instance de la blockchain.
     */
    public void setBlockchain(BlockChain blockchain) {
        this.blockchain = blockchain;
    }

    /**
     * Affiche une boîte de dialogue d'erreur avec un titre et un message.
     *
     * @param title   Le titre de la boîte de dialogue.
     * @param content Le message de contenu.
     */
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
