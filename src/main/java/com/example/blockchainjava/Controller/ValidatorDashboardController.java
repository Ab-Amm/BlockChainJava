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
    @FXML private TableView<Block> blockTable;
    @FXML private TableView<Transaction> pendingTransactionsTable;
    private Validator validator;
    private BlockChain blockchain;
    private SocketServer socketServer;

    public ValidatorDashboardController(Validator validator, BlockChain blockchain) {
        this.validator = validator;
        this.blockchain = blockchain;
    }

    @FXML
    public void initialize() throws IOException {
        blockchain.addObserver(this);
        ServerSocket serverSocket = new ServerSocket(8080);
        socketServer = new SocketServer(blockchain, serverSocket, validator);
        new Thread(socketServer::start).start();
        updateBlockchainView();
    }

    @Override
    public void onBlockchainUpdate(BlockChain blockchain) {
        updateBlockchainView();
    }

    private void updateBlockchainView() {
        // Update block table
        List<Block> blocks = blockchain.getBlocks();
        ObservableList<Block> blockList = FXCollections.observableArrayList(blocks);
        blockTable.setItems(blockList);

        // Update pending transactions table
        List<Transaction> pendingTransactions = blockchain.getPendingTransactions();
        ObservableList<Transaction> pendingTransactionsList = FXCollections.observableArrayList(pendingTransactions);
        pendingTransactionsTable.setItems(pendingTransactionsList);
    }

    @FXML
    private void validateTransaction() {
        Transaction selectedTransaction = pendingTransactionsTable.getSelectionModel().getSelectedItem();
        if (selectedTransaction != null) {
            String signature = validator.sign(selectedTransaction);
            blockchain.addBlock(selectedTransaction, signature);
        }
    }

    public void setValidator(Validator user) {
        this.validator = user;
    }
}