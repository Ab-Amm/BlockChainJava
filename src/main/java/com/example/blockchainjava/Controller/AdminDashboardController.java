package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.DAO.DatabaseConnection;
import com.example.blockchainjava.Model.DAO.TransactionDAO;
import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.Admin;
import com.example.blockchainjava.Model.User.User;
import com.example.blockchainjava.Model.User.UserRole;
import com.example.blockchainjava.Model.User.Validator;
import com.example.blockchainjava.Util.Network.SocketClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.example.blockchainjava.Model.DAO.DatabaseConnection;

public class AdminDashboardController {

    @FXML
    private Label adminNameLabel;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField balanceField;
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
    private Admin admin;
    private UserDAO userDAO;
    private ObservableList<Validator> validatorList;
    private final Connection connection;


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

    }
    private List<SocketClient> getValidators() {
        List<SocketClient> validators = new ArrayList<>();
        String sql = "SELECT ip_address, port FROM validators"; // Table 'validators' avec colonnes 'ip_address' et 'port'

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String ip = rs.getString("ip_address");
                int port = rs.getInt("port");
                validators.add(new SocketClient(ip, port));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error fetching validators from database: " + e.getMessage());
        }

        return validators;
    }
    public void broadcastPendingTransactions() {
        TransactionDAO transactionDAO = new TransactionDAO(); // Créez une instance de TransactionDAO

        // Récupérer les transactions avec le statut PENDING
        List<Transaction> pendingTransactions = transactionDAO.getTransactionsByStatus(TransactionStatus.PENDING);

        for (Transaction transaction : pendingTransactions) {
            for (SocketClient validator : getValidators()) {
                try {
                    // Établir une connexion avec le validateur
                    validator.connect();

                    // Envoyer la transaction au validateur
                    validator.sendTransaction(transaction);

                    // Recevoir la réponse du validateur
                    String response = validator.receiveResponse();

                    // Mettre à jour le statut de la transaction en fonction de la réponse
                    if ("ACCEPTED".equals(response)) {
                        transaction.setStatus(TransactionStatus.VALIDATED);
                    } else {
                        transaction.setStatus(TransactionStatus.REJECTED);
                    }

                    validator.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    // En cas d'échec, rejeter la transaction
                    transaction.setStatus(TransactionStatus.REJECTED);
                } finally {
                    // Mettre à jour la transaction dans la base de données
                    transactionDAO.updateTransaction(transaction);
                }
            }
        }
    }


    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    private List<String> getValidatorsFromDatabase() {
        // Exemple de récupération depuis la base de données
        // Vous devrez adapter cette méthode pour correspondre à votre modèle de données
        List<String> validators = new ArrayList<>();
        // Ajoutez votre logique pour récupérer les validateurs ici
        // Par exemple : `validators.add(validator.getUsername());`
        return validators;
    }

    @FXML
    public void initialize() {
        // Configuration du nom de l'administrateur
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
        String balanceInput = balanceField.getText();
        String ipAddress = ipField.getText();
        String portInput = portField.getText();

        if (username.isEmpty() || password.isEmpty() || balanceInput.isEmpty() || ipAddress.isEmpty() || portInput.isEmpty()) {
            showErrorMessage("Tous les champs sont obligatoires !");
            return;
        }

        try {
            double balance = Double.parseDouble(balanceInput);
            int port = Integer.parseInt(portInput);

            // Création de l'utilisateur et du validateur
            User newUser = new User(username, password, UserRole.VALIDATOR );
            Validator validator = new Validator(username, password, ipAddress, port , balance);

            // Enregistrement dans la base de données
            userDAO.saveUser(newUser, balance);
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

    @FXML
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

            // Mise à jour du solde
            userDAO.updateValidatorBalance(selectedValidator, newBalance);

            // Rafraîchir la liste
            updateValidatorList();
            showInfoMessage("Validateur modifié avec succès !");
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
