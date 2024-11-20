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
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.security.NoSuchAlgorithmException;
import java.util.List;

public class AdminDashboardController {

    @FXML private Label adminNameLabel;
    @FXML private TextField usernameField;
    @FXML private ComboBox<UserRole> roleComboBox;
    @FXML private PasswordField passwordField;
    @FXML private TextField balanceField;
    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private TableView<Validator> validatorTable;

    private Admin admin;
    private UserDAO userDAO;
    private ObservableList<Validator> validatorList;

    public AdminDashboardController() {
        userDAO = new UserDAO();
    }

    public AdminDashboardController(Admin admin) {
        this();
        this.admin = admin;
    }

    @FXML
    public void initialize() {
        // Configuration de roleComboBox
        roleComboBox.setItems(FXCollections.observableArrayList(UserRole.values())); // Ajout des rôles disponibles

        // Configuration des colonnes de validatorTable
        TableColumn<Validator, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<Validator, String> publicKeyColumn = new TableColumn<>("Public Key");
        publicKeyColumn.setCellValueFactory(new PropertyValueFactory<>("publicKey"));

        TableColumn<Validator, Boolean> activeColumn = new TableColumn<>("Active");
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("isActive"));

        //validatorTable.getColumns().addAll(usernameColumn, publicKeyColumn, activeColumn);

        // Initialisation des données
        validatorList = FXCollections.observableArrayList();
        //updateValidatorTable();

        if (admin != null) {
            adminNameLabel.setText(admin.getUsername());
        }
    }

    private void updateValidatorTable() {
        validatorList.clear();
        List<Validator> validators = userDAO.getValidators();
        if (validators != null) {
            validatorList.addAll(validators);
        }
        //validatorTable.setItems(validatorList);
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    @FXML
    public void handleAddValidator(ActionEvent actionEvent) {
        Button addButton = (Button) actionEvent.getSource();
        addButton.setDisable(true);
        String username = usernameField.getText();
        UserRole role = roleComboBox.getValue();
        String password = passwordField.getText();
        String balanceInput = balanceField.getText();
        String ipAddress = ipField.getText();
        String portInput = portField.getText();

        if (username.isEmpty() || role == null || password.isEmpty() || balanceInput.isEmpty() || ipAddress.isEmpty() || portInput.isEmpty()) {
            showErrorMessage("All fields are required!");
            return;
        }

        try {
            double balance = Double.parseDouble(balanceInput);
            int port = Integer.parseInt(portInput);

            // Create user and validator
            User newUser = new User(username, password, role);
            Validator validator = new Validator(username, newUser.getPublicKey(), ipAddress, port);

            // Save user first
            userDAO.saveUser(newUser , balance );

            // Save validator (without inserting the user again)
            userDAO.saveValidator(validator, ipAddress, port);

            // Refresh table
            updateValidatorTable();

        } catch (NumberFormatException e) {
            showErrorMessage("Balance and Port must be valid numbers!");
        } catch (Exception e) {
            showErrorMessage("An error occurred while adding the validator!");
            e.printStackTrace();
        }
    }

}
