package com.example.blockchainjava.Model.Block;

import com.example.blockchainjava.Controller.ValidatorDashboardController;
import com.example.blockchainjava.Model.DAO.BlockDAO;
import com.example.blockchainjava.Model.DAO.TransactionDAO;
import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.Client;
import com.example.blockchainjava.Model.User.User;
import com.example.blockchainjava.Model.User.Validator;
import com.example.blockchainjava.Observer.BlockchainUpdateObserver;
import com.example.blockchainjava.Util.Network.SocketServer;
import com.example.blockchainjava.Util.RedisUtil;
import com.example.blockchainjava.Util.Security.HashUtil;
import com.example.blockchainjava.Util.Security.SecurityUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.PublicKey;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.example.blockchainjava.Model.DAO.DatabaseConnection;

public class BlockChain {
    private final List<Block> chain;
    private final BlockDAO blockDAO;
    private final List<BlockchainUpdateObserver> observers;
    private final Connection connection;
    private long chainVersion;
    private TransactionDAO transactionDAO;
    private UserDAO userDAO;

    private static final String STORAGE_DIR = "blockchain_storage";
    private static final int MAX_VERSIONS = 5;
    private static final Object chainLock = new Object();

    public void loadChainFromDatabase() {
        List<Block> blocksFromDB = blockDAO.getAllBlocks();
        chain.addAll(blocksFromDB);
    }
    public long getChainVersion() {
        return chainVersion;
    }
    public BlockChain() {
        this.chain = new ArrayList<>();
        this.blockDAO = new BlockDAO();
        this.observers = new ArrayList<>();
        this.connection = DatabaseConnection.getConnection();
        this.transactionDAO = new TransactionDAO();
        this.userDAO = new UserDAO();
        loadFromDatabase();

    }

    private String calculateBlockHash(Block block) {
        return HashUtil.sha256(
                block.getPreviousHash() +
                        block.getTransaction().getId() +
                        block.getTimestamp().toString() +
                        block.getValidatorSignature()
        );
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

    public synchronized Block addBlock(Transaction transaction, String validatorSignature) {
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

                updateTransactionWithBlockIdAndStatus(transaction, newBlock);

                // Update user balances
                TransactionDAO transactionDAO = new TransactionDAO();
                boolean balancesUpdated = transactionDAO.processTransactionBalances(transaction);
                if (!balancesUpdated) {
                    throw new RuntimeException("Failed to update user balances after adding block.");
                }
                saveToLocalStorage();
                notifyObservers();
                return newBlock;
            } catch (Exception e) {
                // Rollback on failure
                chain.remove(chain.size() - 1);
                chainVersion--;
                throw new RuntimeException("Failed to add block: " + e.getMessage(), e);
            }
        }
    }
    public synchronized Block addBlock1(Transaction transaction, String validatorSignature) {
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

                updateTransactionWithBlockIdAndStatus(transaction, newBlock);

                // Update user balances
                TransactionDAO transactionDAO = new TransactionDAO();
                boolean balancesUpdated = transactionDAO.processTransactionBalances3(transaction);
                if (!balancesUpdated) {
                    throw new RuntimeException("Failed to update user balances after adding block.");
                }
                saveToLocalStorage();
                notifyObservers();
                return newBlock;
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
            transaction.setBlockId(newBlock.getBlockId());
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
    private List<Block> loadedBlocks = new ArrayList<>();
    private List<Transaction> loadedTransactions = new ArrayList<>();
    public List<Block> loadFromLocalStorage() {

        System.out.println("[BlockChain] 📂 Starting blockchain load operation...");
        try {
            Path storageDir = Paths.get(System.getProperty("user.dir"), STORAGE_DIR);
            if (!Files.exists(storageDir)) {
                System.out.println("[BlockChain] ℹ️ No local storage found. Starting fresh.");
                return loadedBlocks;  // Returning empty list as no blockchain files are found
            }

            System.out.println("[BlockChain] 🔍 Searching for blockchain files...");
            // Find latest version file
            Optional<Path> latestFile = Files.list(storageDir)
                    .filter(path -> path.toString().matches(".*blockchain_v\\d+\\.json$"))
                    .max((p1, p2) -> {
                        long v1 = extractVersion(p1.getFileName().toString());
                        long v2 = extractVersion(p2.getFileName().toString());
                        return Long.compare(v1, v2);
                    });

            if (latestFile.isPresent()) {
                System.out.println("[BlockChain] 📄 Found latest blockchain file: " + latestFile.get().getFileName());
                String jsonContent = Files.readString(latestFile.get(), StandardCharsets.UTF_8);

                // Parse version
                Pattern versionPattern = Pattern.compile("\"version\":\\s*(\\d+)");
                Matcher versionMatcher = versionPattern.matcher(jsonContent);
                if (versionMatcher.find()) {
                    this.chainVersion = Long.parseLong(versionMatcher.group(1));
                    System.out.println("[BlockChain] 📊 Found blockchain version: " + chainVersion);
                }

                System.out.println("[BlockChain] 🔄 Starting block parsing...");
                // Parse blocks
                Pattern blockPattern = Pattern.compile("\\{\\s*\"blockId\":\\s*(\\d+),\\s*\"previousHash\":\\s*\"([^\"])\",\\s\"currentHash\":\\s*\"([^\"])\",\\s\"timestamp\":\\s*\"([^\"])\",\\s\"validatorSignature\":\\s*\"([^\"])\",\\s\"transaction\":\\s*\\{([^}]+)\\}\\s*\\}");
                Matcher blockMatcher = blockPattern.matcher(jsonContent);

                int blockCount = 0;
                while (blockMatcher.find()) {
                    blockCount++;
                    System.out.println("[BlockChain] 📦 Parsing block " + blockCount);

                    int blockId = Integer.parseInt(blockMatcher.group(1));
                    String previousHash = blockMatcher.group(2);
                    String currentHash = blockMatcher.group(3);
                    String timestamp = blockMatcher.group(4);
                    String validatorSignature = blockMatcher.group(5);
                    String transactionJson = blockMatcher.group(6);

                    System.out.println("[BlockChain] 💳 Parsing transaction for block " + blockId);
                    // Parse transaction
                    Pattern txPattern = Pattern.compile("\"id\":\\s*(\\d+),\\s*\"senderId\":\\s*(\\d+),\\s*\"receiverKey\":\\s*\"([^\"])\",\\s\"amount\":\\s*([\\d.]+),\\s*\"status\":\\s*\"([^\"])\",\\s\"blockId\":\\s*(\\d+),\\s*\"createdAt\":\\s*\"([^\"])\",\\s\"signature\":\\s*\"([^\"]*)\"");
                    Matcher txMatcher = txPattern.matcher(transactionJson);

                    if (txMatcher.find()) {
                        Transaction transaction = new Transaction();
                        transaction.setId(Integer.parseInt(txMatcher.group(1)));
                        transaction.setSenderId(Integer.parseInt(txMatcher.group(2)));
                        transaction.setReceiverKey(txMatcher.group(3));
                        transaction.setAmount(Double.parseDouble(txMatcher.group(4)));
                        transaction.setStatus(TransactionStatus.valueOf(txMatcher.group(5)));
                        transaction.setBlockId(Integer.parseInt(txMatcher.group(6)));
                        transaction.setCreatedAt(LocalDateTime.parse(txMatcher.group(7)));
                        transaction.setSignature(txMatcher.group(8));

                        Block block = new Block(blockId, previousHash, transaction, validatorSignature);
                        block.setCurrentHash(currentHash);
                        block.setTimestamp(LocalDateTime.parse(timestamp));

                        // Add to lists instead of chain
                        loadedBlocks.add(block);
                        loadedTransactions.add(transaction);
                        System.out.println("[BlockChain] ✅ Successfully added block " + blockId + " to loaded blocks");
                    }
                }

                System.out.println("[BlockChain] 🎉 Successfully loaded blockchain version " + chainVersion +
                        " with " + loadedBlocks.size() + " blocks and " + loadedTransactions.size() + " transactions");
            } else {
                System.out.println("[BlockChain] ℹ️ No blockchain files found. Starting fresh.");
            }
        } catch (IOException e) {
            System.err.println("[BlockChain] ❌ Error loading blockchain: " + e.getMessage());
            e.printStackTrace();
        }

        // Returning the lists instead of modifying the current chain
        return loadedBlocks;  // You can also return loadedTransactions if needed
    }

    public List<Transaction> loadFromLocalStorage1() {

        System.out.println("[BlockChain] 📂 Starting blockchain load operation...");
        try {
            Path storageDir = Paths.get(System.getProperty("user.dir"), STORAGE_DIR);
            if (!Files.exists(storageDir)) {
                System.out.println("[BlockChain] ℹ️ No local storage found. Starting fresh.");
                return loadedTransactions;  // Returning empty list as no blockchain files are found
            }

            System.out.println("[BlockChain] 🔍 Searching for blockchain files...");
            // Find latest version file
            Optional<Path> latestFile = Files.list(storageDir)
                    .filter(path -> path.toString().matches(".*blockchain_v\\d+\\.json$"))
                    .max((p1, p2) -> {
                        long v1 = extractVersion(p1.getFileName().toString());
                        long v2 = extractVersion(p2.getFileName().toString());
                        return Long.compare(v1, v2);
                    });

            if (latestFile.isPresent()) {
                System.out.println("[BlockChain] 📄 Found latest blockchain file: " + latestFile.get().getFileName());
                String jsonContent = Files.readString(latestFile.get(), StandardCharsets.UTF_8);

                // Parse version
                Pattern versionPattern = Pattern.compile("\"version\":\\s*(\\d+)");
                Matcher versionMatcher = versionPattern.matcher(jsonContent);
                if (versionMatcher.find()) {
                    this.chainVersion = Long.parseLong(versionMatcher.group(1));
                    System.out.println("[BlockChain] 📊 Found blockchain version: " + chainVersion);
                }

                System.out.println("[BlockChain] 🔄 Starting block parsing...");
                // Parse blocks
                Pattern blockPattern = Pattern.compile("\\{\\s*\"blockId\":\\s*(\\d+),\\s*\"previousHash\":\\s*\"([^\"]+)\",\\s*\"currentHash\":\\s*\"([^\"]+)\",\\s*\"timestamp\":\\s*\"([^\"]+)\",\\s*\"validatorSignature\":\\s*\"([^\"]+)\",\\s*\"transaction\":\\s*\\{([^}]+)\\}\\s*\\}");
                Matcher blockMatcher = blockPattern.matcher(jsonContent);

                int blockCount = 0;
                while (blockMatcher.find()) {
                    blockCount++;
                    System.out.println("[BlockChain] 📦 Parsing block " + blockCount);

                    int blockId = Integer.parseInt(blockMatcher.group(1));
                    String previousHash = blockMatcher.group(2);
                    String currentHash = blockMatcher.group(3);
                    String timestamp = blockMatcher.group(4);
                    String validatorSignature = blockMatcher.group(5);
                    String transactionJson = blockMatcher.group(6);

                    System.out.println("[BlockChain] 💳 Parsing transaction for block " + blockId);
                    // Parse transaction
                    Pattern txPattern = Pattern.compile("\"id\":\\s*(\\d+),\\s*\"senderId\":\\s*(\\d+),\\s*\"receiverKey\":\\s*\"([^\"]+)\",\\s*\"amount\":\\s*([\\d.]+),\\s*\"status\":\\s*\"([^\"]+)\",\\s*\"blockId\":\\s*(\\d+),\\s*\"createdAt\":\\s*\"([^\"]+)\",\\s*\"signature\":\\s*\"([^\"]*)\"");
                    Matcher txMatcher = txPattern.matcher(transactionJson);

                    if (txMatcher.find()) {
                        Transaction transaction = new Transaction();
                        transaction.setId(Integer.parseInt(txMatcher.group(1)));
                        transaction.setSenderId(Integer.parseInt(txMatcher.group(2)));
                        transaction.setReceiverKey(txMatcher.group(3));
                        transaction.setAmount(Double.parseDouble(txMatcher.group(4)));
                        transaction.setStatus(TransactionStatus.valueOf(txMatcher.group(5)));
                        transaction.setBlockId(Integer.parseInt(txMatcher.group(6)));
                        transaction.setCreatedAt(LocalDateTime.parse(txMatcher.group(7)));
                        transaction.setSignature(txMatcher.group(8));

                        Block block = new Block(blockId, previousHash, transaction, validatorSignature);
                        block.setCurrentHash(currentHash);
                        block.setTimestamp(LocalDateTime.parse(timestamp));

                        // Add to lists instead of chain
                        loadedBlocks.add(block);
                        loadedTransactions.add(transaction);
                        System.out.println("[BlockChain] ✅ Successfully added block " + blockId + " and its transaction to loaded lists");
                    }
                }

                System.out.println("[BlockChain] 🎉 Successfully loaded blockchain version " + chainVersion +
                        " with " + loadedBlocks.size() + " blocks and " + loadedTransactions.size() + " transactions");
            } else {
                System.out.println("[BlockChain] ℹ️ No blockchain files found. Starting fresh.");
            }
        } catch (IOException e) {
            System.err.println("[BlockChain] ❌ Error loading blockchain: " + e.getMessage());
            e.printStackTrace();
        }

        // Returning the transactions list instead of blocks
        return loadedTransactions;
    }

    private void updateValidatorVersion(long version) {
        String updateSql  = "UPDATE validators SET pending_update_version = ?";

        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {

            stmt.setLong(1, version);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("[BlockChain] ✅ Validator version updated to " + version);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update transaction status and block_id", e);
        }
    }
    public synchronized void saveToLocalStorage() {
        if (chain == null) {
            System.err.println("[BlockChain] ⚠️ Cannot save: chain is null");
            throw new IllegalStateException("Chain is null");
        }

        System.out.println("[BlockChain] 💾 Starting blockchain save operation...");
        System.out.println("[BlockChain] 📊 Current chain state: Version=" + chainVersion + ", Blocks=" + chain.size());

        synchronized(chainLock) {
            int maxRetries = 5;
            int retryDelayMs = 2000; // 2 seconds delay between retries
            Exception lastException = null;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                Path tempFile = null;
                try {
                    // Ensure storage directory exists
                    Path storageDir = Paths.get(System.getProperty("user.dir"), STORAGE_DIR);
                    if (!Files.exists(storageDir)) {
                        Files.createDirectories(storageDir);
                        System.out.println("[BlockChain] 📁 Created storage directory: " + storageDir);
                    }

                    // Create JSON string manually
                    StringBuilder jsonBuilder = new StringBuilder();
                    jsonBuilder.append("{\n");
                    jsonBuilder.append("  \"version\": ").append(chainVersion).append(",\n");
                    jsonBuilder.append("  \"timestamp\": ").append(System.currentTimeMillis()).append(",\n");
                    jsonBuilder.append("  \"blocks\": [\n");

                    // Add blocks
                    for (int i = 0; i < chain.size(); i++) {
                        Block block = chain.get(i);
                        jsonBuilder.append("    {\n");
                        jsonBuilder.append("      \"blockId\": ").append(block.getBlockId()).append(",\n");
                        jsonBuilder.append("      \"previousHash\": \"").append(block.getPreviousHash()).append("\",\n");
                        jsonBuilder.append("      \"currentHash\": \"").append(block.getCurrentHash()).append("\",\n");
                        jsonBuilder.append("      \"timestamp\": \"").append(block.getTimestamp()).append("\",\n");
                        jsonBuilder.append("      \"validatorSignature\": \"").append(block.getValidatorSignature()).append("\",\n");

                        // Add transaction
                        Transaction tx = block.getTransaction();
                        jsonBuilder.append("      \"transaction\": {\n");
                        jsonBuilder.append("        \"id\": ").append(tx.getId()).append(",\n");
                        jsonBuilder.append("        \"senderId\": ").append(tx.getSenderId()).append(",\n");
                        jsonBuilder.append("        \"receiverKey\": \"").append(tx.getReceiverKey()).append("\",\n");
                        jsonBuilder.append("        \"amount\": ").append(tx.getAmount()).append(",\n");
                        jsonBuilder.append("        \"status\": \"").append(tx.getStatus()).append("\",\n");
                        jsonBuilder.append("        \"blockId\": ").append(tx.getBlockId()).append(",\n");
                        jsonBuilder.append("        \"createdAt\": \"").append(tx.getCreatedAt()).append("\",\n");
                        jsonBuilder.append("        \"signature\": \"").append(tx.getSignature()).append("\"\n");
                        jsonBuilder.append("      }\n");
                        jsonBuilder.append("    }").append(i < chain.size() - 1 ? "," : "").append("\n");
                    }

                    jsonBuilder.append("  ]\n");
                    jsonBuilder.append("}\n");

                    // Create filename with version
                    String filename = String.format("blockchain_v%d.json", chainVersion);
                    Path filePath = storageDir.resolve(filename);

                    // Create temp file in the same directory
                    tempFile = Files.createTempFile(storageDir, "temp_", ".json");

                    // Write to temporary file
                    Files.writeString(tempFile, jsonBuilder.toString(), StandardCharsets.UTF_8);

                    // Try to delete the target file if it exists
                    Files.deleteIfExists(filePath);

                    // Retry loop for atomic move with lock check
                    boolean moveSuccess = false;
                    int moveRetries = 3;
                    for (int moveAttempt = 1; moveAttempt <= moveRetries; moveAttempt++) {
                        try {
                            // Check if the target file is in use
                            if (Files.exists(filePath) && !Files.isWritable(filePath)) {
                                System.err.println("[BlockChain] ⚠️ Target file is locked, retrying move...");
                                Thread.sleep(retryDelayMs * moveAttempt); // Exponential backoff for file move
                                continue;
                            }

                            // Atomic move of the temp file
                            Files.move(tempFile, filePath, StandardCopyOption.ATOMIC_MOVE);
                            moveSuccess = true;
                            break; // Exit loop if move is successful
                        } catch (IOException e) {
                            System.err.println("[BlockChain] ⚠️ Attempt " + moveAttempt + " to move file failed: " + e.getMessage());
                            if (moveAttempt < moveRetries) {
                                Thread.sleep(retryDelayMs * moveAttempt); // Exponential backoff
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    if (!moveSuccess) {
                        throw new IOException("Failed to atomically move the temp file after multiple attempts");
                    }

                    System.out.println("[BlockChain] ✅ Successfully saved blockchain version " + chainVersion);

                    // Clean up old versions after successful save
                    cleanupOldVersions(storageDir, MAX_VERSIONS);
                    cleanupTempFiles();
                    return; // Success - exit the retry loop
                } catch (IOException | InterruptedException e) {
                    lastException = e;
                    System.err.println("[BlockChain] ⚠️ Attempt " + attempt + " failed: " + e.getMessage());

                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(retryDelayMs * attempt); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while retrying save operation", ie);
                        }
                    }
                } finally {
                    // Cleanup temp file if it exists
                    if (tempFile != null && Files.exists(tempFile)) {
                        try {
                            Files.deleteIfExists(tempFile);
                        } catch (IOException e) {
                            System.err.println("[BlockChain] ⚠️ Could not delete temp file: " + e.getMessage());
                        }
                    }
                }
            }

            // If we get here, all retries failed
            System.err.println("[BlockChain] ❌ Failed to save blockchain after " + maxRetries + " attempts");
            throw new RuntimeException("Failed to save blockchain to storage", lastException);
        }
    }



    private void cleanupTempFiles() {
        Path storageDir = Paths.get(System.getProperty("user.dir"), STORAGE_DIR);
        if (!Files.exists(storageDir)) {
            return;
        }

        try {
            // Find all temp files
            List<Path> tempFiles = Files.list(storageDir)
                    .filter(path -> path.getFileName().toString().startsWith("temp_"))
                    .collect(Collectors.toList());

            for (Path tempFile : tempFiles) {
                try {
                    // Try to force delete the file
                    Files.deleteIfExists(tempFile);
                    System.out.println("[BlockChain] 🧹 Cleaned up temp file: " + tempFile.getFileName());
                } catch (IOException e) {
                    // If we can't delete it normally, try to force close any open handles
                    System.gc(); // Suggest garbage collection to release file handles
                    try {
                        Thread.sleep(100); // Give a small delay
                        Files.deleteIfExists(tempFile);
                        System.out.println("[BlockChain] 🧹 Cleaned up temp file after retry: " + tempFile.getFileName());
                    } catch (IOException | InterruptedException ex) {
                        System.out.println("[BlockChain] ⚠️ Could not delete temp file: " + tempFile.getFileName());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[BlockChain] ⚠️ Error during temp file cleanup: " + e.getMessage());
        }
    }

    public boolean verifyBlockchain() {
        try {
            // Étape 1 : Vérifier l'intégrité du stockage local

            // Étape 2 : Récupérer tous les blocs depuis la base de données
            List<Block> blockchain = blockDAO.getAllBlocks();
            if (blockchain.isEmpty()) {
                System.out.println("La blockchain est vide.");
                return true;
            }

            String previousHash = "0"; // Hash initial pour le premier bloc
            boolean allValid = true; // Indique si la blockchain est globalement valide

            // Étape 3 : Vérification bloc par bloc
            for (Block block : blockchain) {
                String recalculatedHash = block.calculateHash();

                // Vérification de l'intégrité du bloc
                if (!block.getCurrentHash().equals(recalculatedHash)) {
                    System.err.println("Le hash du bloc ID " + block.getBlockId() + " est invalide. Tentative de restauration...");
                    if (!verifyLocalStorageIntegrity()) {
                        System.err.println("Le stockage local est corrompu. Restauration impossible.");
                        return false; // Arrêter le processus si le stockage local est invalide
                    }
                    else if (!restoreBlockData(block)) {
                        System.err.println("Échec de la restauration du bloc ID " + block.getBlockId() + ".");
                        allValid = false;
                    }
                    continue;
                }

                // Vérification du previous_hash
                if (!block.getPreviousHash().equals(previousHash)) {
                    System.err.println("Le previous_hash du bloc ID " + block.getBlockId() + " est invalide. Tentative de restauration...");
                    if (!verifyLocalStorageIntegrity()) {
                        System.err.println("Le stockage local est corrompu. Restauration impossible.");
                        return false; // Arrêter le processus si le stockage local est invalide
                    }
                    else if (!restoreBlockData(block)) {
                        System.err.println("Échec de la restauration du bloc ID " + block.getBlockId() + ".");
                        allValid = false;
                    }
                    continue;
                }

                // Étape 4 : Vérification des transactions dans le bloc
                List<Transaction> transactions = transactionDAO.getTransactionsByBlockId(block.getBlockId());
                for (Transaction transaction : transactions) {
                    PublicKey senderPublicKey = SecurityUtils.decodePublicKey(
                            userDAO.getPublicKeyByUserId(transaction.getSenderId())
                    );
                    boolean isSignatureValid = SecurityUtils.verifySignature(
                            transaction.getDataToSign(),
                            transaction.getSignature(),
                            senderPublicKey
                    );

                    if (!isSignatureValid) {
                        System.err.println("La signature de la transaction ID " + transaction.getId() + " est invalide. Tentative de restauration...");
                        if (!verifyLocalStorageIntegrity()) {
                            System.err.println("Le stockage local est corrompu. Restauration impossible.");
                            return false; // Arrêter le processus si le stockage local est invalide
                        }
                        else if (!restoreTransactionData(transaction)) {
                            System.err.println("Échec de la restauration de la transaction ID " + transaction.getId() + ".");
                            allValid = false;
                        }
                        continue;
                    }
                }

                // Mise à jour du hash précédent
                previousHash = block.getCurrentHash();
            }

            // Étape 5 : Résultat final
            if (allValid) {
                System.out.println("La blockchain est valide.");
            } else {
                System.err.println("La blockchain contient des problèmes qui n'ont pas pu être entièrement corrigés.");
            }
            return allValid;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la vérification de la blockchain : " + e.getMessage());
            return false;
        }
    }
    private boolean restoreBlockData(Block block) {
        try {
            // Charger les données correctes depuis le stockage local
            Block localBlock = loadLocalBlockById(block.getBlockId());
            if (localBlock == null) {
                System.err.println("Bloc ID " + block.getBlockId() + " introuvable dans le stockage local.");
                return false;
            }

            // Mettre à jour la base de données avec les données correctes
            blockDAO.updateBlock(localBlock);
            chain.clear();
            loadChainFromDatabase();
            System.out.println("Bloc ID " + block.getBlockId() + " restauré avec succès.");
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de la restauration du bloc ID " + block.getBlockId() + " : " + e.getMessage());
            return false;
        }
    }
    private boolean restoreTransactionData(Transaction transaction) {
        try {
            // Charger les données correctes depuis le stockage local
            Transaction localTransaction = loadLocalTransactionById(transaction.getId());
            if (localTransaction == null) {
                System.err.println("Transaction ID " + transaction.getId() + " introuvable dans le stockage local.");
                return false;
            }

            // Mettre à jour la base de données avec les données correctes
            transactionDAO.updateTransaction(localTransaction);
            chain.clear();
            loadChainFromDatabase();
            System.out.println("Transaction ID " + transaction.getId() + " restaurée avec succès.");
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de la restauration de la transaction ID " + transaction.getId() + " : " + e.getMessage());
            return false;
        }
    }
    private Block loadLocalBlockById(int blockId) {
        // Appel à la méthode pour charger les blocs depuis le stockage local
        loadFromLocalStorage();

        // Parcours la liste des blocs chargés pour trouver celui correspondant à l'ID
        for (Block block : loadedBlocks) {
            if (block.getBlockId() == blockId) {
                System.out.println("[BlockChain] 📦 Block found with ID: " + blockId);
                return block; // Retourne le bloc correspondant
            }
        }
        System.out.println("[BlockChain] ❌ No block found with ID: " + blockId);
        return null; // Retourne null si aucun bloc n'a été trouvé
    }
    private Transaction loadLocalTransactionById(int transactionId) {
        // Appel à la méthode pour charger les transactions depuis le stockage local
        loadFromLocalStorage1();
        System.out.println("[BlockChain] 📦 transactions from local storage..." + loadedTransactions.size());

        // Parcours la liste des transactions chargées pour trouver celle correspondant à l'ID
        for (Transaction transaction : loadedTransactions) {
            if (transaction.getId() == transactionId) {
                System.out.println("[BlockChain] 💳 Transaction found with ID: " + transactionId);
                return transaction; // Retourne la transaction correspondante
            }
        }
        System.out.println("[BlockChain] ❌ No transaction found with ID: " + transactionId);
        return null; // Retourne null si aucune transaction n'a été trouvée
    }


    public boolean verifyLocalStorageIntegrity() {
        System.out.println("[BlockChain] 🔍 Starting verification of local storage integrity...");
        try {
            Path storageDir = Paths.get(System.getProperty("user.dir"), STORAGE_DIR);
            if (!Files.exists(storageDir)) {
                System.err.println("[BlockChain] ⚠️ Local storage directory does not exist.");
                return false;
            }

            // Find the latest blockchain file
            Optional<Path> latestFile = Files.list(storageDir)
                    .filter(path -> path.toString().matches(".*blockchain_v\\d+\\.json$"))
                    .max((p1, p2) -> {
                        long v1 = extractVersion(p1.getFileName().toString());
                        long v2 = extractVersion(p2.getFileName().toString());
                        return Long.compare(v1, v2);
                    });

            if (latestFile.isPresent()) {
                System.out.println("[BlockChain] 📄 Found latest blockchain file: " + latestFile.get().getFileName());
                String jsonContent = Files.readString(latestFile.get(), StandardCharsets.UTF_8);

                // Load blockchain from JSON
                List<Block> loadedChain = parseBlockchainFromJson(jsonContent);

                // Verify each block
                String previousHash = "0"; // Assuming the first block's previous hash is "0"
                for (Block block : loadedChain) {
                    // Check if the block's hash is valid
                    if (!block.getCurrentHash().equals(block.calculateHash())) {
                        System.err.println("[BlockChain] ❌ Invalid hash for block ID: " + block.getBlockId());
                        return false;
                    }

                    // Check if the previous hash matches
                    if (!block.getPreviousHash().equals(previousHash)) {
                        System.err.println("[BlockChain] ❌ Mismatch in previous hash for block ID: " + block.getBlockId());
                        return false;
                    }

                    // Verify the transaction in the block
                    Transaction transaction = block.getTransaction();
                    if (transaction != null) {
                        // Verify the transaction's signature
                        PublicKey senderPublicKey = SecurityUtils.decodePublicKey(
                                userDAO.getPublicKeyByUserId(transaction.getSenderId())
                        );
                        boolean isSignatureValid = SecurityUtils.verifySignature(
                                transaction.getDataToSign(),
                                transaction.getSignature(),
                                senderPublicKey
                        );

                        if (!isSignatureValid) {
                            System.err.println("[BlockChain] ❌ Invalid signature for transaction ID: " + transaction.getId());
                            return false;
                        }

                        // Verify sender's balance (if applicable)
                        if (transaction.getSenderId() != null) {
                            User sender = userDAO.findUserById(transaction.getSenderId());
                            if (sender.getBalance() < transaction.getAmount()) {
                                System.err.println("[BlockChain] ❌ Insufficient balance for transaction ID: " + transaction.getId());
                                return false;
                            }
                        }
                    }

                    // Update the previousHash for the next iteration
                    previousHash = block.getCurrentHash();
                }

                System.out.println("[BlockChain] ✅ Local storage integrity and transactions verified successfully.");
                return true;
            } else {
                System.err.println("[BlockChain] ⚠️ No blockchain files found in local storage.");
                return true;
            }
        } catch (IOException e) {
            System.err.println("[BlockChain] ❌ Error verifying local storage: " + e.getMessage());
            e.printStackTrace();
            return false;
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private List<Block> parseBlockchainFromJson(String jsonContent) {
        List<Block> blocks = new ArrayList<>();
        Pattern blockPattern = Pattern.compile("\\{\\s*\"blockId\":\\s*(\\d+),\\s*\"previousHash\":\\s*\"([^\"])\",\\s\"currentHash\":\\s*\"([^\"])\",\\s\"timestamp\":\\s*\"([^\"])\",\\s\"validatorSignature\":\\s*\"([^\"])\",\\s\"transaction\":\\s*\\{([^}]+)\\}\\s*\\}");
        Matcher blockMatcher = blockPattern.matcher(jsonContent);

        while (blockMatcher.find()) {
            int blockId = Integer.parseInt(blockMatcher.group(1));
            String previousHash = blockMatcher.group(2);
            String currentHash = blockMatcher.group(3);
            String timestamp = blockMatcher.group(4);
            String validatorSignature = blockMatcher.group(5);
            String transactionJson = blockMatcher.group(6);

            Transaction transaction = null;
            if (transactionJson != null && !transactionJson.isBlank()) {
                Pattern txPattern = Pattern.compile("\"id\":\\s*(\\d+),\\s*\"senderId\":\\s*(\\d+),\\s*\"receiverKey\":\\s*\"([^\"])\",\\s\"amount\":\\s*([\\d.]+),\\s*\"status\":\\s*\"([^\"])\",\\s\"blockId\":\\s*(\\d+),\\s*\"createdAt\":\\s*\"([^\"])\",\\s\"signature\":\\s*\"([^\"]*)\"");
                Matcher txMatcher = txPattern.matcher(transactionJson);

                if (txMatcher.find()) {
                    transaction = new Transaction();
                    transaction.setId(Integer.parseInt(txMatcher.group(1)));
                    transaction.setSenderId(Integer.parseInt(txMatcher.group(2)));
                    transaction.setReceiverKey(txMatcher.group(3));
                    transaction.setAmount(Double.parseDouble(txMatcher.group(4)));
                    transaction.setStatus(TransactionStatus.valueOf(txMatcher.group(5)));
                    transaction.setBlockId(Integer.parseInt(txMatcher.group(6)));
                    transaction.setCreatedAt(LocalDateTime.parse(txMatcher.group(7)));
                    transaction.setSignature(txMatcher.group(8));
                } else {
                    System.err.println("[BlockChain] ⚠️ Transaction introuvable ou mal formée pour le bloc " + blockId);
                }
            }
            Block block = new Block(blockId, previousHash, transaction, validatorSignature);
            block.setCurrentHash(currentHash);
            block.setTimestamp(LocalDateTime.parse(timestamp));

            blocks.add(block);
        }
        return blocks;
    }

    private long extractVersion(String filename) {
        try {
            // Extract version number from filename (blockchain_v123.json -> 123)
            Pattern pattern = Pattern.compile("blockchain_v(\\d+)\\.json");
            Matcher matcher = pattern.matcher(filename);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        } catch (Exception e) {
            System.err.println("Error extracting version from filename: " + filename + " - " + e.getMessage());
        }
        return 0;
    }

    private void cleanupOldVersions(Path storageDir, int keepCount) {
        try {
            if (!Files.exists(storageDir)) {
                System.out.println("[BlockChain] ℹ️ No storage directory to clean");
                return;
            }

            System.out.println("[BlockChain] 🔍 Scanning for old blockchain versions...");
            // Get all blockchain files
            List<Path> versionFiles = Files.list(storageDir)
                    .filter(path -> path.toString().matches(".*blockchain_v\\d+\\.json$"))
                    .sorted((p1, p2) -> {
                        long v1 = extractVersion(p1.getFileName().toString());
                        long v2 = extractVersion(p2.getFileName().toString());
                        return Long.compare(v2, v1); // Sort in descending order
                    })
                    .collect(Collectors.toList());

            System.out.println("[BlockChain] 📊 Found " + versionFiles.size() + " version files, keeping newest " + keepCount);

            // Delete old versions
            if (versionFiles.size() > keepCount) {
                for (int i = keepCount; i < versionFiles.size(); i++) {
                    try {
                        Path fileToDelete = versionFiles.get(i);
                        Files.deleteIfExists(fileToDelete);
                        System.out.println("[BlockChain] 🗑️ Deleted old version: " + fileToDelete.getFileName());
                    } catch (IOException e) {
                        System.err.println("[BlockChain] ⚠️ Error deleting old version: " + versionFiles.get(i) + " - " + e.getMessage());
                    }
                }
                System.out.println("[BlockChain] ✅ Cleanup complete. Kept " + keepCount + " newest versions");
            } else {
                System.out.println("[BlockChain] ℹ️ No cleanup needed. Current versions (" + versionFiles.size() +
                        ") <= max versions (" + keepCount + ")");
            }
        } catch (IOException e) {
            System.err.println("[BlockChain] ❌ Error during cleanup: " + e.getMessage());
        }
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
        TransactionDAO transactionDAO = new TransactionDAO();
        boolean balancesUpdated = transactionDAO.processTransactionBalances2(block.getTransaction());
        if (!balancesUpdated) {
            throw new RuntimeException("Failed to update user balances after adding block.");
        }
        saveToLocalStorage();
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

    public void initializeStorage() {
        try {
            Path storagePath = Paths.get(System.getProperty("user.dir"), STORAGE_DIR);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                System.out.println("[BlockChain] 📁 Created blockchain storage directory: " + storagePath.toAbsolutePath());
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
            System.out.println("[BlockChain] ✅ Blockchain reset completed");
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
                System.out.println("[BlockChain] ✅ Pruned blockchain to " + keepCount + " blocks");
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

    public synchronized void synchronizeWith(BlockChain otherChain) {
        System.out.println("🔄 Starting blockchain synchronization");
        try {
            if (otherChain.getVersion() > this.chainVersion) {
                System.out.println("📊 Other chain version " + otherChain.getVersion() +
                        " is newer than local version " + this.chainVersion);

                // Clear and update chain data
                this.chain.clear();
                this.chain.addAll(otherChain.getBlocks());
                this.chainVersion = otherChain.getVersion();

                // Save synchronized chain to storage
                saveToLocalStorage();

                System.out.println("✅ Successfully synchronized to version " + this.chainVersion);
            } else {
                System.out.println("ℹ️ Local chain is already up to date");
            }
        } catch (Exception e) {
            System.err.println("❌ Error during chain synchronization: " + e.getMessage());
            throw new RuntimeException("Failed to synchronize blockchain", e);
        }
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


    public void updateAllClientBalances() {
        try {
            // Retrieve all clients from the database
            List<Client> clients = userDAO.getAllClients();

            for (Client client : clients) {
                double calculatedBalance = 0.0;

                // Calculate the balance from the blockchain
                for (Block block : chain) {
                    Transaction transaction = block.getTransaction();
                    if (transaction.getSenderId() == client.getId()) {
                        calculatedBalance -= transaction.getAmount();
                    }
                    if (transaction.getReceiverKey().equals(client.getPublicKey())) {
                        calculatedBalance += transaction.getAmount();
                    }
                }

                // Update both the database and Redis cache
                client.setBalance(calculatedBalance);

                // Update database
                try {
                    userDAO.updateUserBalance(client, calculatedBalance);
                } catch (Exception e) {
                    System.err.println("Failed to update database balance for client ID: " + client.getId());
                    e.printStackTrace();
                }

                // Update Redis cache
                try {
                    RedisUtil.setUserBalance(client.getId(), calculatedBalance);
                } catch (Exception e) {
                    System.err.println("Failed to update Redis cache for client ID: " + client.getId());
                    e.printStackTrace();
                }

                // Log success
                System.out.println("Updated balance for client " + client.getUsername() +
                        " (ID: " + client.getId() + "): " + calculatedBalance);
            }
        } catch (Exception e) {
            System.err.println("An error occurred while updating client balances:");
            e.printStackTrace();
        }
    }
    public void updateAllValidatorBalances() {
        try {
            // Retrieve all clients from the database
            List<Validator> validators = userDAO.getAllValidators();
            System.out.println("voici");
            System.out.println(validators);

            for (Validator client : validators) {
                double calculatedBalance = 0.0;

                // Calculate the balance from the blockchain
                for (Block block : chain) {
                    Transaction transaction = block.getTransaction();
                    if (transaction.getSenderId() == client.getId()) {
                        calculatedBalance -= transaction.getAmount();
                    }
                    if (transaction.getReceiverKey().equals(client.getPublicKey())) {
                        calculatedBalance += transaction.getAmount();
                    }
                }

                // Update both the database and Redis cache
                client.setBalance(calculatedBalance);

                // Update database
                try {
                    userDAO.updateUserBalance1(client, calculatedBalance);
                } catch (Exception e) {
                    System.err.println("Failed to update database balance for client ID: " + client.getId());
                    e.printStackTrace();
                }

                // Update Redis cache
                try {
                    RedisUtil.setUserBalance(client.getId(), calculatedBalance);
                } catch (Exception e) {
                    System.err.println("Failed to update Redis cache for client ID: " + client.getId());
                    e.printStackTrace();
                }

                // Log success
                System.out.println("Updated balance for client " + client.getUsername() +
                        " (ID: " + client.getId() + "): " + calculatedBalance);
            }
        } catch (Exception e) {
            System.err.println("An error occurred while updating client balances:");
            e.printStackTrace();
        }
    }

}