package com.example.blockchainjava.Model.Block;

import com.example.blockchainjava.Controller.ValidatorDashboardController;
import com.example.blockchainjava.Model.DAO.BlockDAO;
import com.example.blockchainjava.Model.DAO.TransactionDAO;
import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.Client;
import com.example.blockchainjava.Model.User.User;
import com.example.blockchainjava.Observer.BlockchainUpdateObserver;
import com.example.blockchainjava.Util.Network.SocketServer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.security.PublicKey;
import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.blockchainjava.Model.DAO.DatabaseConnection;
import com.example.blockchainjava.Util.Security.HashUtil;
import com.example.blockchainjava.Util.Security.SecurityUtils;

public class BlockChain {
    private final List<Block> chain;
    private final BlockDAO blockDAO;
    private final List<BlockchainUpdateObserver> observers;
    private final Connection connection;
    private long chainVersion;
    private final ObjectMapper objectMapper;
    private TransactionDAO transactionDAO;
    private UserDAO userDAO;

    private static final String STORAGE_DIR = "blockchain_storage";
    private static final int MAX_VERSIONS = 5;
    private static final Object chainLock = new Object();

    public void loadChainFromDatabase() {
        List<Block> blocksFromDB = blockDAO.getAllBlocks();
        chain.addAll(blocksFromDB);
    }

    public BlockChain() {
        this.chain = new ArrayList<>();
        this.blockDAO = new BlockDAO();
        this.observers = new ArrayList<>();
        this.connection = DatabaseConnection.getConnection();
        this.transactionDAO = new TransactionDAO();
        this.userDAO = new UserDAO();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Create storage directory if it doesn't exist
        initializeStorage();
        
        // First try to load from local storage
        loadFromLocalStorage();
        
        // If local storage is empty, try loading from database
        if (chain.isEmpty()) {
            loadFromDatabase();
            // Save to local storage after loading from database
            if (!chain.isEmpty()) {
                saveToLocalStorage();
            }
        }
    }

    private String calculateBlockHash(Block block) {
        return HashUtil.sha256(
                block.getPreviousHash() +
                        block.getTransaction().getId() +
                        block.getTimestamp().toString() +
                        block.getValidatorSignature()
        );
    }

    public boolean verifyBlockchain() {
        try {
            List<Block> blockchain = blockDAO.getAllBlocks(); // Récupérer tous les blocs depuis la base de données
            if (blockchain.isEmpty()) {
                System.out.println("La blockchain est vide.");
                return true;
            }

            String previousHash = "0"; // Hash initial pour le premier bloc
            for (Block block : blockchain) {
                // Recalculer le hachage actuel
                String recalculatedHash = block.calculateHash();

                // Vérifier l'intégrité du bloc
                if (!block.getCurrentHash().equals(recalculatedHash)) {
                    System.err.println("Le hash du bloc ID " + block.getBlockId() + " est invalide.");
                    restoreBlockData(block);
                    return false;
                }

                // Vérifier que le previous_hash correspond au current_hash du bloc précédent
                if (!block.getPreviousHash().equals(previousHash)) {
                    System.err.println("Le previous_hash du bloc ID " + block.getBlockId()  + " est invalide.");
                    restoreBlockData(block);
                    return false;
                }

                // Vérifier les transactions dans le bloc
                List<Transaction> transactions = transactionDAO.getTransactionsByBlockId(block.getBlockId() );
                for (Transaction transaction : transactions) {
                    // Vérifier la signature de la transaction
                    PublicKey senderPublicKey = SecurityUtils.decodePublicKey(
                            userDAO.getPublicKeyByUserId(transaction.getSenderId())
                    );
                    boolean isSignatureValid = SecurityUtils.verifySignature(
                            transaction.getDataToSign(),
                            transaction.getSignature(),
                            senderPublicKey
                    );

                    if (!isSignatureValid) {
                        System.err.println("La signature de la transaction ID " + transaction.getId() + " est invalide.");
                        restoreTransactionData(transaction);
                        return false;
                    }

                    // Vérifier que le solde de l'expéditeur est suffisant
                    User sender = userDAO.findUserById(transaction.getSenderId());
                    if (sender.getBalance() < transaction.getAmount()) {
                        System.err.println("Le solde de l'expéditeur pour la transaction ID " + transaction.getId() + " est insuffisant.");
                        return false;
                    }
                }

                // Mettre à jour le hash précédent pour le prochain bloc
                previousHash = block.getCurrentHash();
            }

            System.out.println("La blockchain est valide.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la vérification de la blockchain : " + e.getMessage());
            return false;
        }
    }

    private void restoreTransactionData(Transaction transaction) {
        try {
            System.out.println("Restauration des données de la transaction ID " + transaction.getId());

            // Récupérer la transaction originale depuis la base de données (sauvegarde initiale)
            Transaction originalTransaction = transactionDAO.getTransactionById(transaction.getId());

            if (originalTransaction != null) {
                // Décoder la clé publique de l'expéditeur
                PublicKey senderPublicKey = SecurityUtils.decodePublicKey(
                        userDAO.getPublicKeyByUserId(originalTransaction.getSenderId())
                );

                // Vérifier la signature de la transaction
                boolean isSignatureValid = SecurityUtils.verifySignature(
                        originalTransaction.getDataToSign(),  // Les données à signer doivent être les mêmes
                        originalTransaction.getSignature(),
                        senderPublicKey
                );

                if (!isSignatureValid) {
                    System.err.println("La signature de la transaction ID " + transaction.getId() + " est invalide.");
                    return;  // Ne pas restaurer la transaction si la signature est invalide
                }

                // Restaurer les valeurs initiales de la transaction
                transaction.setSenderId(originalTransaction.getSenderId());
                transaction.setReceiverKey(originalTransaction.getReceiverKey());
                transaction.setAmount(originalTransaction.getAmount());
                transaction.setSignature(originalTransaction.getSignature());
                transaction.setCreatedAt(originalTransaction.getCreatedAt());

                // Mettre à jour la transaction restaurée dans la base de données
                transactionDAO.updateTransaction(transaction);

                System.out.println("Transaction restaurée avec succès ID " + transaction.getId());
            } else {
                System.err.println("Impossible de restaurer la transaction ID " + transaction.getId() + ": introuvable.");
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la restauration de la transaction ID " + transaction.getId() + ": " + e.getMessage());
        }
    }

    private void restoreBlockData(Block block) {
        System.out.println("Restoration du bloc ID " + block.getBlockId());
        try {
            // Récupérer les transactions liées au bloc depuis la base de données
            List<Transaction> transactions = transactionDAO.getTransactionsByBlockId(block.getBlockId());

            // Vérifier et restaurer chaque transaction si nécessaire
            List<Transaction> validTransactions = new ArrayList<>();
            for (Transaction transaction : transactions) {
                if (validateTransaction(transaction)) {
                    validTransactions.add(transaction);
                } else {
                    System.out.println("Restauration de la transaction ID " + transaction.getId());
                    restoreTransactionData(transaction);
                    Transaction restoredTransaction = transactionDAO.getTransactionById(transaction.getId());
                    if (restoredTransaction != null) {
                        validTransactions.add(restoredTransaction);
                    }
                }
            }

            // Recalculer le hash du bloc à partir des transactions valides
            String recalculatedHash = HashUtil.sha256(
                    block.getPreviousHash() +
                            validTransactions.stream()
                                    .map(Transaction::getId)
                                    .map(String::valueOf) // Assurez-vous que chaque ID est converti en String
                                    .collect(Collectors.joining(",")) + // Utilisation d'une virgule comme séparateur entre les IDs
                            block.getValidatorSignature()
            );

            // Mettre à jour le hash du bloc dans la base de données si nécessaire
            if (!recalculatedHash.equals(block.getCurrentHash())) {
                System.out.println("Mise à jour du hash du bloc ID " + block.getBlockId());
                block.setCurrentHash(recalculatedHash);
                blockDAO.updateBlock(block);
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la restauration du bloc ID " + block.getBlockId() + ": " + e.getMessage());
        }
    }

    public boolean validateTransaction(Transaction transaction) {
        try {
            // Vérifier la signature de la transaction
            PublicKey senderPublicKey = SecurityUtils.decodePublicKey(
                    userDAO.getPublicKeyByUserId(transaction.getSenderId())
            );
            boolean isSignatureValid = SecurityUtils.verifySignature(
                    transaction.getDataToSign(),
                    transaction.getSignature(),
                    senderPublicKey
            );

            if (!isSignatureValid) {
                System.err.println("La signature de la transaction ID " + transaction.getId() + " est invalide.");
                return false;
            }

            // Vérifier que le solde de l'expéditeur est suffisant
            User sender = userDAO.findUserById(transaction.getSenderId());
            if (sender.getBalance() < transaction.getAmount()) {
                System.err.println("Le solde de l'expéditeur pour la transaction ID " + transaction.getId() + " est insuffisant.");
                return false;
            }

            // Vérifier que la transaction n'est pas déjà validée ou annulée
            if (transaction.getStatus() != TransactionStatus.PENDING) {
                System.err.println("La transaction ID " + transaction.getId() + " n'est pas dans un état valide (elle est déjà " + transaction.getStatus() + ").");
                return false;
            }

            // Si tout est valide
            System.out.println("La transaction ID " + transaction.getId() + " est valide.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la validation de la transaction ID " + transaction.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private int generateNewBlockId() {
        // Vous pouvez récupérer l'ID du dernier bloc de la base de données et l'incrémenter
        return blockDAO.getLastBlockId() + 1;
    }

    public synchronized void addBlock(Transaction transaction, String validatorSignature) {
        validateBlockParameters(transaction, validatorSignature);
        
        synchronized(chainLock) {
            String previousHash = chain.isEmpty() ? "0" : chain.getLast().getCurrentHash();
            int newBlockId = generateNewBlockId();
            Block newBlock = new Block(newBlockId, previousHash, transaction, validatorSignature);
            
            // Add to chain and increment version
            chain.add(newBlock);
            chainVersion++;
            
            try {
                // Save to both database and local storage
                blockDAO.saveBlock(newBlock);
                saveToLocalStorage();
                
                updateTransactionWithBlockIdAndStatus(transaction, newBlock);

                // Update user balances
                TransactionDAO transactionDAO = new TransactionDAO();
                boolean balancesUpdated = transactionDAO.processTransactionBalances(transaction);
                if (!balancesUpdated) {
                    throw new RuntimeException("Failed to update user balances after adding block.");
                }

                notifyObservers();
            } catch (Exception e) {
                // Rollback on failure
                chain.remove(chain.size() - 1);
                chainVersion--;
                throw new RuntimeException("Failed to add block: " + e.getMessage(), e);
            }
        }
    }

    private void updateTransactionWithBlockIdAndStatus(Transaction transaction, Block newBlock) {
        System.out.println("voici bach necrasiw");
        System.out.println(transaction);
        String updateSql = "UPDATE transactions SET sender_id= ?, receiver_key = ?, amount = ?, block_id = ?, status = ? , signature=? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            // Récupérer l'id du bloc créé et le mettre à jour dans la transaction

            stmt.setInt(1, transaction.getSenderId());  // sender_id
            stmt.setString(2, transaction.getReceiverKey());  // receiver_key
            stmt.setDouble(3, transaction.getAmount());  // amount
            stmt.setInt(4, newBlock.getBlockId());  // block_id (nouvel ID de bloc)
            stmt.setString(5, TransactionStatus.VALIDATED.name());
            stmt.setString(6,transaction.getSignature());
            stmt.setInt(7, transaction.getId());  // id (transaction ID)

            int rowsUpdated = stmt.executeUpdate();  // Utiliser executeUpdate() pour les requêtes de mise à jour

            if (rowsUpdated > 0) {
                System.out.println("Transaction mise à jour avec succès");
            } else {
                System.out.println("Aucune transaction mise à jour avec l'ID : " + transaction.getId());
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update transaction status and block_id", e);
        }
    }

    public void loadFromDatabase() {
        try {
            List<Block> blocksFromDB = blockDAO.getAllBlocks();
            if (!blocksFromDB.isEmpty()) {
                chain.clear();
                chain.addAll(blocksFromDB);
                chainVersion = blocksFromDB.size(); // Use size as initial version
                System.out.println("Loaded " + blocksFromDB.size() + " blocks from database");
            }
        } catch (Exception e) {
            System.err.println("Error loading from database: " + e.getMessage());
        }
    }

    public synchronized void saveToLocalStorage() {
        if (chain == null) {
            throw new IllegalStateException("Chain is null");
        }
        
        synchronized(chainLock) {
            try {
                // Create the blockchain data structure
                Map<String, Object> blockchainData = new HashMap<>();
                blockchainData.put("version", chainVersion);
                blockchainData.put("blocks", chain);
                blockchainData.put("timestamp", System.currentTimeMillis());
                
                // Ensure directory exists
                Path storageDir = Paths.get(STORAGE_DIR);
                if (!Files.exists(storageDir)) {
                    Files.createDirectories(storageDir);
                }
                
                // Create filename with version
                String filename = String.format("blockchain_v%d.json", chainVersion);
                Path filePath = storageDir.resolve(filename);
                
                // Write to temporary file first
                Path tempFile = Files.createTempFile(storageDir, "temp_", ".json");
                objectMapper.writerWithDefaultPrettyPrinter()
                          .writeValue(tempFile.toFile(), blockchainData);
                
                // Atomic move to final location
                Files.move(tempFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                
                // Cleanup old versions
                cleanupOldVersions(storageDir, MAX_VERSIONS);
                
                System.out.println("Saved blockchain version " + chainVersion + " to " + filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save blockchain to storage", e);
            }
        }
    }

    public void loadFromLocalStorage() {
        try {
            Path storageDir = Paths.get(STORAGE_DIR);
            if (!Files.exists(storageDir)) {
                System.out.println("No local storage found. Starting fresh.");
                return;
            }

            // Find the latest version file
            Optional<Path> latestFile = Files.list(storageDir)
                .filter(path -> path.toString().matches(".*blockchain_v\\d+\\.json$"))
                .max((p1, p2) -> {
                    long v1 = extractVersion(p1.getFileName().toString());
                    long v2 = extractVersion(p2.getFileName().toString());
                    return Long.compare(v1, v2);
                });

            if (latestFile.isPresent()) {
                // Read the file
                Map<String, Object> blockchainData = objectMapper.readValue(
                    latestFile.get().toFile(),
                    new TypeReference<Map<String, Object>>() {}
                );

                // Extract version
                this.chainVersion = ((Number) blockchainData.get("version")).longValue();

                // Extract blocks
                List<Block> loadedBlocks = objectMapper.convertValue(
                    blockchainData.get("blocks"),
                    new TypeReference<List<Block>>() {}
                );

                // Validate the loaded chain
                if (validateLoadedChain(loadedBlocks)) {
                    chain.clear();
                    chain.addAll(loadedBlocks);
                    System.out.println("Loaded blockchain version " + chainVersion + 
                                     " with " + chain.size() + " blocks");
                } else {
                    System.err.println("Loaded chain validation failed. Starting fresh.");
                    chain.clear();
                    chainVersion = 0;
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading blockchain: " + e.getMessage());
            chain.clear();
            chainVersion = 0;
        }
    }

    private void cleanupOldVersions(Path storageDir, int keepCount) {
        try {
            // Get all blockchain files
            List<Path> versionFiles = Files.list(storageDir)
                .filter(path -> path.toString().matches(".*blockchain_v\\d+\\.json$"))
                .sorted((p1, p2) -> {
                    long v1 = extractVersion(p1.getFileName().toString());
                    long v2 = extractVersion(p2.getFileName().toString());
                    return Long.compare(v2, v1); // Sort in descending order
                })
                .collect(Collectors.toList());

            // Delete old versions
            if (versionFiles.size() > keepCount) {
                for (int i = keepCount; i < versionFiles.size(); i++) {
                    Files.deleteIfExists(versionFiles.get(i));
                    System.out.println("Deleted old version: " + versionFiles.get(i));
                }
            }
        } catch (IOException e) {
            System.err.println("Error cleaning up old versions: " + e.getMessage());
        }
    }

    private long extractVersion(String filename) {
        Pattern pattern = Pattern.compile("blockchain_v(\\d+)\\.json");
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return 0;
    }

    public synchronized void notifyObservers() {
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
            Transaction transaction = block.getTransaction();
            // Vérifier si la transaction est en attente (PENDING)
            if (transaction.getStatus() == TransactionStatus.PENDING) {
                transactions.add(transaction);
            }
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

    public CompareResult compareChain(List<Block> otherChain, long otherVersion) {
        CompareResult result = new CompareResult();
        result.setLocalVersion(this.chainVersion);
        result.setOtherVersion(otherVersion);
        
        if (otherVersion > this.chainVersion) {
            if (validateLoadedChain(otherChain)) {
                result.setNeedsUpdate(true);
                result.setValidChain(true);
                System.out.println("Found newer valid chain version: " + otherVersion);
            } else {
                result.setNeedsUpdate(false);
                result.setValidChain(false);
                System.err.println("Newer chain version " + otherVersion + " failed validation");
            }
        } else {
            result.setNeedsUpdate(false);
            result.setValidChain(validateLoadedChain(otherChain));
        }
        
        return result;
    }

    private boolean validateLoadedChain(List<Block> loadedChain) {
        if (loadedChain == null || loadedChain.isEmpty()) {
            return false;
        }

        String previousHash = "0"; // Genesis block has no previous hash
        
        for (Block block : loadedChain) {
            // Verify block hash
            if (!block.getCurrentHash().equals(block.calculateHash())) {
                System.err.println("Invalid block hash for block ID: " + block.getBlockId());
                return false;
            }

            // Verify block link
            if (!block.getPreviousHash().equals(previousHash)) {
                System.err.println("Invalid block link for block ID: " + block.getBlockId());
                return false;
            }

            // Verify transaction signature
            Transaction transaction = block.getTransaction();
            if (!validateTransaction(transaction)) {
                System.err.println("Invalid transaction in block ID: " + block.getBlockId());
                return false;
            }

            previousHash = block.getCurrentHash();
        }
        
        return true;
    }

    private void initializeStorage() {
        try {
            Path storagePath = Paths.get(STORAGE_DIR);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                System.out.println("Created blockchain storage directory: " + storagePath.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize blockchain storage directory", e);
        }
    }

    public synchronized void reset() {
        synchronized(chainLock) {
            chain.clear();
            chainVersion = 0;
            saveToLocalStorage();
            System.out.println("Blockchain reset completed");
        }
    }

    public synchronized void pruneOldBlocks(int keepCount) {
        if (keepCount <= 0) {
            throw new IllegalArgumentException("Keep count must be positive");
        }
        synchronized(chainLock) {
            if (chain.size() > keepCount) {
                chain.subList(0, chain.size() - keepCount).clear();
                chainVersion = chain.size();
                saveToLocalStorage();
                System.out.println("Pruned blockchain to " + keepCount + " blocks");
            }
        }
    }

    private void validateBlockParameters(Transaction transaction, String validatorSignature) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (validatorSignature == null || validatorSignature.trim().isEmpty()) {
            throw new IllegalArgumentException("Validator signature cannot be null or empty");
        }
    }

    public long getVersion() {
        return this.chainVersion;
    }

    public void setVersion(long version) {
        this.chainVersion = version;
    }


    // Static inner class for chain comparison results
    public static class CompareResult {
        private boolean needsUpdate;
        private boolean validChain;
        private long localVersion;
        private long otherVersion;

        public boolean isNeedsUpdate() {
            return needsUpdate;
        }

        public void setNeedsUpdate(boolean needsUpdate) {
            this.needsUpdate = needsUpdate;
        }

        public boolean isValidChain() {
            return validChain;
        }

        public void setValidChain(boolean validChain) {
            this.validChain = validChain;
        }

        public long getLocalVersion() {
            return localVersion;
        }

        public void setLocalVersion(long localVersion) {
            this.localVersion = localVersion;
        }

        public long getOtherVersion() {
            return otherVersion;
        }

        public void setOtherVersion(long otherVersion) {
            this.otherVersion = otherVersion;
        }

        @Override
        public String toString() {
            return "CompareResult{" +
                    "needsUpdate=" + needsUpdate +
                    ", validChain=" + validChain +
                    ", localVersion=" + localVersion +
                    ", otherVersion=" + otherVersion +
                    '}';
        }
    }
}