package com.example.blockchainjava.Model.Block;

import com.example.blockchainjava.Controller.ValidatorDashboardController;
import com.example.blockchainjava.Model.DAO.BlockDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Observer.BlockchainUpdateObserver;
import com.example.blockchainjava.Util.Network.SocketServer;

import java.util.ArrayList;
import java.util.List;


public class BlockChain {
    private final List<Block> chain;
    private final BlockDAO blockDAO;
    private final List<BlockchainUpdateObserver> observers;

    public BlockChain() {
        this.chain = new ArrayList<>();
        this.blockDAO = new BlockDAO();
        this.observers = new ArrayList<>();
        loadChainFromDatabase();
    }

    private void loadChainFromDatabase() {
        List<Block> blocksFromDB = blockDAO.getAllBlocks();
        chain.addAll(blocksFromDB);
    }

    public void addBlock(Transaction transaction, String validatorSignature) {
        String previousHash = chain.isEmpty() ? "0" : chain.getLast().getCurrentHash();
        Block newBlock = new Block(previousHash, transaction, validatorSignature);
        chain.add(newBlock);
        blockDAO.saveBlock(newBlock);
        notifyObservers();
    }

    private void notifyObservers() {
        for (BlockchainUpdateObserver observer : observers) {
            observer.onBlockchainUpdate(this);
        }
    }

    public Block getLatestBlock() {
        return chain.getLast();
    }


    public List<Block> getBlocks() {
        return chain;
    }


    public List<Transaction> getPendingTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        for (Block block : chain) {
            transactions.add(block.getTransaction());
        }
        return transactions;
    }

    public void addBlock(Block block) {
        chain.add(block);
    }

    public void addObserver(ValidatorDashboardController validatorDashboardController) {
        observers.add(validatorDashboardController);
    }

    public Boolean getBalanceBool(Integer sender) {
        for (Block block : chain) {
            if (block.getTransaction().getSenderId().equals(sender)) {
                return true;
            }
        }
        return false;
    }
    public double getBalance(Integer sender) {
        double balance = 0.0;
        for (Block block : chain) {
            if (block.getTransaction().getSenderId().equals(sender)) {
                balance =  balance + block.getTransaction().getAmount();
            }
        }
        return balance;
    }

    public boolean containsTransaction(Transaction transaction) {
        for (Block block : chain) {
            if (block.getTransaction().equals(transaction)) {
                return true;
            }
        }
        return false;
    }

}
