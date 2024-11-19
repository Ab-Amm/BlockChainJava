package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Model.User.Admin;
import com.example.blockchainjava.Model.User.UserRole;
import com.example.blockchainjava.Model.User.Validator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.security.NoSuchAlgorithmException;

public class AdminDashboardController {
    //@FXML private TableView<Validator> validatorTable;
    @FXML private Label adminNameLabel;
    private Admin admin;
    private UserDAO userDAO;
    //private ObservableList<Validator> validatorList;

    public AdminDashboardController() {
        // Constructeur par défaut requis par JavaFX
        userDAO = new UserDAO();
    }

    public AdminDashboardController(Admin admin) {
        this.admin = admin;
        userDAO = new UserDAO();
        //validatorList = FXCollections.observableArrayList();
    }

   /* @FXML
    private void addValidator() throws NoSuchAlgorithmException {
        // Show dialog to add new validator
        Dialog<Validator> dialog = new Dialog<>();
        dialog.initOwner((Stage) validatorTable.getScene().getWindow());
        dialog.setTitle("Add Validator");
        dialog.setHeaderText("Enter validator details:");
        dialog.getDialogPane().setContent(new ValidatorFormController(dialog));

        dialog.showAndWait().ifPresent(validator -> {
            validator.setRole(UserRole.VALIDATOR);
            userDAO.saveUser(validator);
            updateValidatorTable();
        });
    }

    private void updateValidatorTable() {
        validatorList.clear();
        validatorList.addAll(userDAO.getValidators());
        validatorTable.setItems(validatorList);
    }*/

    @FXML
    public void initialize() {
        TableColumn<Validator, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        if (admin != null) {
            updateDashboard();
        }

        /*TableColumn<Validator, String> publicKeyColumn = new TableColumn<>("Public Key");
        publicKeyColumn.setCellValueFactory(new PropertyValueFactory<>("publicKey"));

        TableColumn<Validator, Boolean> activeColumn = new TableColumn<>("Active");
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("isActive"));

        validatorTable.getColumns().addAll(usernameColumn, publicKeyColumn, activeColumn);

        updateValidatorTable();*/
    }

    public void setAdmin(Admin user) {
        this.admin = user;
        updateDashboard();
    }
    private void updateDashboard() {
        if (admin != null) {
            adminNameLabel.setText(admin.getUsername());
            //balanceLabel.setText(String.format("$%.2f", client.getBalance()));
        }
    }
}
