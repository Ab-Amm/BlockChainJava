package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.User.Admin;
import com.example.blockchainjava.Model.User.User;
import com.example.blockchainjava.Model.User.UserRole;
import com.example.blockchainjava.Model.User.Validator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController {

    @FXML private Label adminNameLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField balanceField;
    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private VBox addValidatorForm;
    @FXML private VBox editValidatorForm;
    @FXML private VBox deleteValidatorForm;
    @FXML private VBox validatorActionsPane;
    @FXML
    private Button manageValidatorsButton;
    @FXML
    private ComboBox<Validator> validatorSelectComboBox;
    @FXML
    private TextField newBalanceField;
    @FXML
    private ComboBox<Validator> validatorDeleteComboBox;


    @FXML
    public void showEditValidatorForm() {
        resetDynamicForms();
        editValidatorForm.setVisible(true);
        // Initialiser la ComboBox avec les validateurs existants
        validatorSelectComboBox.setItems(validatorList);
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

            // Mise à jour du solde dans la base de données
            userDAO.updateValidatorBalance(selectedValidator, newBalance);

            // Actualisation de la liste des validateurs
            updateValidatorTable();

            showInfoMessage("Validateur modifié avec succès !");
        } catch (NumberFormatException e) {
            showErrorMessage("Le solde doit être un nombre valide !");
        } catch (Exception e) {
            showErrorMessage("Une erreur est survenue lors de la modification du validateur.");
            e.printStackTrace();
        }
    }


    private Admin admin;
    private UserDAO userDAO;
    private ObservableList<Validator> validatorList;

    public AdminDashboardController() {
        userDAO = new UserDAO();
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
        // Configuration de l'affichage du nom de l'administrateur
        if (admin != null) {
            adminNameLabel.setText(admin.getUsername());
        }

        // Initialisation de la liste des validateurs
        validatorList = FXCollections.observableArrayList();
        updateValidatorTable();

        // Récupérer la liste des validateurs depuis la base de données
        List<Validator> validatorsFromDB = userDAO.getValidators();

        // Ajouter les validateurs dans la ComboBox
        validatorList.addAll(validatorsFromDB);
        validatorSelectComboBox.setItems(validatorList);
        validatorDeleteComboBox.setItems(validatorList);

        // Définir le rendu des validateurs dans les ComboBox
        setUpValidatorComboBox();
    }

    // Définir le rendu des validateurs dans les ComboBox
    private void setUpValidatorComboBox() {
        // Configuration du cellFactory pour validatorSelectComboBox
        validatorSelectComboBox.setCellFactory(param -> new ListCell<Validator>() {
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

        // Configuration du buttonCell pour validatorSelectComboBox
        validatorSelectComboBox.setButtonCell(new ListCell<Validator>() {
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

        // Configuration du cellFactory pour validatorDeleteComboBox
        validatorDeleteComboBox.setCellFactory(param -> new ListCell<Validator>() {
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

        // Configuration du buttonCell pour validatorDeleteComboBox
        validatorDeleteComboBox.setButtonCell(new ListCell<Validator>() {
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


    // Actualisation de la table des validateurs
    private void updateValidatorTable() {
        validatorList.clear();
        List<Validator> validators = userDAO.getValidators();
        if (validators != null) {
            validatorList.addAll(validators);
        }
    }

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



    // Gestion de l'ajout d'un validateur
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
            User newUser = new User(username, password, UserRole.VALIDATOR);
            Validator validator = new Validator(username, newUser.getPublicKey(), ipAddress, port);

            // Enregistrement dans la base de données
            userDAO.saveUser(newUser, balance);
            userDAO.saveValidator(validator, ipAddress, port);

            // Actualisation de la table
            updateValidatorTable();
            showInfoMessage("Validateur ajouté avec succès !");
        } catch (NumberFormatException e) {
            showErrorMessage("Le solde et le port doivent être des nombres valides !");
        } catch (Exception e) {
            showErrorMessage("Une erreur est survenue lors de l'ajout du validateur.");
            e.printStackTrace();
        }
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
            // Supprimer le validateur de la base de données
            userDAO.deleteValidator(selectedValidator);

            // Actualisation de la liste des validateurs
            updateValidatorTable();

            showInfoMessage("Validateur supprimé avec succès !");
        } catch (Exception e) {
            showErrorMessage("Une erreur est survenue lors de la suppression du validateur.");
            e.printStackTrace();
        }
    }

}
