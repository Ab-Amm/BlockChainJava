package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.DAO.DatabaseConnection;
import com.example.blockchainjava.Model.DAO.TransactionDAO;
import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.*;
import com.example.blockchainjava.Util.Network.SocketClient;
import com.example.blockchainjava.Util.Security.SecurityUtils;
import com.example.blockchainjava.Model.Block.BlockChain;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.security.PrivateKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminDashboardController {

    @FXML
    private Label adminNameLabel;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField ipField;
    @FXML
    private TextField portField;
    @FXML
    private VBox addValidatorForm;
    @FXML
    private VBox editValidatorForm;
    @FXML
    private VBox deleteValidatorForm;
    @FXML
    private VBox validatorActionsPane;
    @FXML
    private Button manageValidatorsButton;
    @FXML
    private ComboBox<Validator> validatorSelectComboBox;
    @FXML
    private TextField newBalanceField;
    @FXML
    private ComboBox<Validator> validatorDeleteComboBox;
    @FXML
    private TableView<Map<String, Object>> transactionTable;
    @FXML
    private TableColumn<Map<String, Object>, Integer> transactionIdColumn; // "id" est un entier
    @FXML
    private TableColumn<Map<String, Object>, String> senderColumn; // "senderName" est une chaîne
    @FXML
    private TableColumn<Map<String, Object>, String> receiverColumn; // "receiverName" est une chaîne
    @FXML
    private TableColumn<Map<String, Object>, Double> amountColumn; // "amount" est un double
    @FXML
    private TableColumn<Map<String, Object>, String> statusColumn; // "status" est une chaîne
    @FXML
    private TableColumn<Map<String, Object>, String> dateColumn; // "createdAt" est une chaîne (convertie depuis LocalDateTime)


    private Admin admin;
    private UserDAO userDAO;
    private ObservableList<Validator> validatorList;
    private final Connection connection;
    private BlockChain blockchain;
    private TransactionDAO transactionDAO;

    @FXML
    public void showEditValidatorForm() {
        resetDynamicForms();
        editValidatorForm.setVisible(true);
        // Initialiser la ComboBox avec les validateurs existants
        validatorSelectComboBox.setItems(validatorList);
    }
    public AdminDashboardController() {
        this.connection = DatabaseConnection.getConnection();
        userDAO = new UserDAO();
        this.blockchain = new BlockChain();
        this.transactionDAO = new TransactionDAO();
        User currentUser = Session.getCurrentUser();
        if (currentUser != null) {
            int Id = currentUser.getId(); // Get the username from the current user;
            this.admin = userDAO.getAdminFromDatabase(Id);
            System.out.println("voici cle prive de l'admin");
            System.out.println(admin.getPrivateKey());
        }else {
            System.err.println("No user is currently logged in.");
        }
        System.out.println("this is the admin connected to this dashboard: " + admin);

    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    @FXML
    public void initialize() {
        transactionIdColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>((Integer) cellData.getValue().get("id")));
        senderColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("senderName")));
        receiverColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("receiverName")));
        amountColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>((Double) cellData.getValue().get("amount")));
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().get("status").toString()));
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().get("createdAt").toString()));

        // Configuration du nom de l'administrateur
        ObservableList<Map<String, Object>> transactionList = FXCollections.observableArrayList(transactionDAO.getAllTransactions());
        transactionTable.setItems(transactionList);
        transactionTable.refresh();
        loadTransactionHistory();
        if (admin != null) {
            adminNameLabel.setText(admin.getUsername());
        }

        // Initialisation de la liste des validateurs
        validatorList = FXCollections.observableArrayList();
        updateValidatorList();

        // Configuration des ComboBox
        setupComboBox(validatorSelectComboBox);
        setupComboBox(validatorDeleteComboBox);
    }

    private void loadTransactionHistory() {
        TransactionDAO transactionDAO = new TransactionDAO();
        try {
            // Récupérer toutes les transactions avec les noms des expéditeurs et destinataires
            List<Map<String, Object>> transactions = transactionDAO.getAllTransactions();
            System.out.println("Transactions récupérées : " + transactions.size());

            // Ajouter les transactions à la liste observable
            ObservableList<Map<String, Object>> transactionList = FXCollections.observableArrayList(transactions);

            // Affecter les données au tableau
            transactionTable.setItems(transactionList);
            transactionTable.refresh();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Erreur lors du chargement des transactions.");
        }
    }



    private void setupComboBox(ComboBox<Validator> comboBox) {
        comboBox.setCellFactory(param -> new ListCell<Validator>() {
            @Override
            protected void updateItem(Validator item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUsername() + " - " + item.getBalance());
                }
            }
        });
        comboBox.setButtonCell(new ListCell<Validator>() {
            @Override
            protected void updateItem(Validator item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUsername() + " - " + item.getBalance());
                }
            }
        });
    }

    private void updateValidatorList() {
        // Rafraîchir la liste des validateurs
        validatorList.clear();
        List<Validator> validatorsFromDB = userDAO.getValidators();
        if (validatorsFromDB != null) {
            validatorList.addAll(validatorsFromDB);
        }

        // Mettre à jour les ComboBox
        validatorSelectComboBox.setItems(FXCollections.observableArrayList(validatorList));
        validatorDeleteComboBox.setItems(FXCollections.observableArrayList(validatorList));
    }

    @FXML
    public void handleAddValidator(ActionEvent actionEvent) {
        String username = usernameField.getText();
        String password = passwordField.getText();
       // String balanceInput = balanceField.getText();
        String ipAddress = ipField.getText();
        String portInput = portField.getText();

        if (username.isEmpty() || password.isEmpty() || ipAddress.isEmpty() || portInput.isEmpty()) {
            showErrorMessage("Tous les champs sont obligatoires !");
            return;
        }

        try {
            int port = Integer.parseInt(portInput);

            // Création de l'utilisateur et du validateur
            User newUser = new User(username, password, UserRole.VALIDATOR );
            Validator validator = new Validator(username, password, ipAddress, port , 0);

            // Enregistrement dans la base de données
            userDAO.saveUser(newUser, 0);
            userDAO.saveValidator(validator, ipAddress, port);

            // Rafraîchir la liste
            updateValidatorList();
            showInfoMessage("Validateur ajouté avec succès !");
        } catch (NumberFormatException e) {
            showErrorMessage("Le solde et le port doivent être des nombres valides !");
        } catch (Exception e) {
            showErrorMessage("Une erreur est survenue lors de l'ajout du validateur.");
            e.printStackTrace();
        }
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

    public void handleEditValidator(ActionEvent actionEvent) {
        Validator selectedValidator = validatorSelectComboBox.getSelectionModel().getSelectedItem();
        if (selectedValidator == null) {
            showErrorMessage("Veuillez sélectionner un validateur !");
            return;
        }

        String newBalanceInput = newBalanceField.getText();
        if (newBalanceInput.isEmpty()) {
            showErrorMessage("Le solde est obligatoire !");
            return;
        }

        try {
            double newBalance = Double.parseDouble(newBalanceInput);

            // Calcul de l'ajustement de balance nécessaire
            double adjustmentAmount = newBalance - selectedValidator.getBalance();
            if (adjustmentAmount == 0) {
                showErrorMessage("Le solde est déjà égal à la valeur saisie.");
                return;
            }

            // Vérification du solde de l'admin
            User currentUser = Session.getCurrentUser();
            if (currentUser.getBalance() < adjustmentAmount) {
                showErrorMessage("Le solde de l'administrateur est insuffisant pour effectuer cette transaction.");
                return;
            }

            // Création de la transaction
            updateValidatorList();
            System.out.println("les validators");
            System.out.println(validatorList);
            Transaction transaction = new Transaction();
            transaction.setSenderId(currentUser.getId()); // ID de l'admin (émetteur)
            transaction.setReceiverKey(selectedValidator.getPublicKey()); // Clé publique du validateur
            transaction.setAmount(adjustmentAmount);
            transaction.setStatus(TransactionStatus.VALIDATED);
            this.admin = userDAO.getAdminFromDatabase(currentUser.getId());
            System.out.println("voici l'admin");
            System.out.println(admin);
            String signature = admin.sign(transaction , this.admin);
            transaction.setSignature(signature);
            System.out.println("Transaction admin : " + transaction);
            // Ajout du bloc à la blockchain
            transactionDAO.saveTransaction(transaction);
            blockchain.addBlock(transaction, signature);

            // Mise à jour du solde du validateur
            selectedValidator.setBalance(selectedValidator.getBalance() + adjustmentAmount);
            userDAO.updateValidatorBalance(selectedValidator, newBalance);

            int Id = currentUser.getId(); // Get the username from the current user;
            this.admin = new Admin( Id ,currentUser.getUsername() , currentUser.getPassword() , currentUser.getBalance());
            userDAO.updateAdminBalance(admin , currentUser.getBalance() - adjustmentAmount);

            currentUser.setBalance(currentUser.getBalance() - adjustmentAmount);
            updateValidatorList();

            // Afficher un message de succès
            showInfoMessage("Transaction réussie et solde du validateur mis à jour !");
            loadTransactionHistory();
        } catch (NumberFormatException e) {
            showErrorMessage("Le solde doit être un nombre valide !");
        } catch (Exception e) {
            showErrorMessage("Une erreur est survenue lors de la modification du validateur.");
            e.printStackTrace();
        }
    }



    @FXML

    // Réinitialiser l'affichage des formulaires dynamiques
    private void resetDynamicForms() {
        addValidatorForm.setVisible(false);
        editValidatorForm.setVisible(false);
        deleteValidatorForm.setVisible(false);
        validatorActionsPane.setVisible(false);
    }

    @FXML
    public void handleManageValidators(ActionEvent actionEvent) {
        // Afficher ou masquer les actions liées aux validateurs
        boolean isVisible = validatorActionsPane.isVisible();
        resetDynamicForms();
        validatorActionsPane.setVisible(!isVisible);
    }


    // Gestion des erreurs
    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Gestion des messages d'information
    private void showInfoMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleManageValidators() {
        // Cacher le bouton "Gérer Validators"
        manageValidatorsButton.setVisible(false);

        // Afficher les actions de gestion des validators
        validatorActionsPane.setVisible(true);
        validatorActionsPane.setManaged(true);
    }

    // Méthode pour revenir à l'état précédent
    @FXML
    private void handleGoBack() {
        // Réafficher le bouton "Gérer Validators"
        manageValidatorsButton.setVisible(true);

        // Cacher les actions de gestion des validators
        validatorActionsPane.setVisible(false);
        validatorActionsPane.setManaged(false);
    }

    @FXML
    public void showAddValidatorForm() {
        resetDynamicForms();
        addValidatorForm.setVisible(true);
        System.out.println("addValidatorForm is visible: " + addValidatorForm.isVisible());
    }

    @FXML
    public void showDeleteValidatorForm() {
        resetDynamicForms();
        deleteValidatorForm.setVisible(true);
        // Initialiser la ComboBox avec les validateurs existants
        validatorDeleteComboBox.setItems(validatorList);
    }

    @FXML
    public void handleDeleteValidator(ActionEvent actionEvent) {
        Validator selectedValidator = validatorDeleteComboBox.getSelectionModel().getSelectedItem();
        if (selectedValidator == null) {
            showErrorMessage("Veuillez sélectionner un validateur !");
            return;
        }

        try {
            // Supprimer le validateur
            userDAO.deleteValidator(selectedValidator);

            // Rafraîchir la liste
            updateValidatorList();
            showInfoMessage("Validateur supprimé avec succès !");
        } catch (Exception e) {
            showErrorMessage("Une erreur est survenue lors de la suppression du validateur.");
            e.printStackTrace();
        }
    }


}
