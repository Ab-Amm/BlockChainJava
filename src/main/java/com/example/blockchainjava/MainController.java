package com.example.blockchainjava;

import com.example.blockchainjava.Model.User.*;
import com.example.blockchainjava.Model.DAO.UserDAO;
import com.example.blockchainjava.Util.Security.HashUtil;
import com.example.blockchainjava.Controller.ClientDashboardController;
import com.example.blockchainjava.Controller.AdminDashboardController;
import com.example.blockchainjava.Controller.ValidatorDashboardController;
import com.example.blockchainjava.Controller.TransactionFormController;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.io.IOException;

public class MainController {
    @FXML private TabPane mainTabPane;
    @FXML private Tab loginTab;
    @FXML private Tab signupTab;

    // Login controls
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;

    // Signup controls
    @FXML private TextField signupUsername;
    @FXML private PasswordField signupPassword;
    @FXML private PasswordField signupConfirmPassword;
    @FXML private TextField signupEmail;
    @FXML private ComboBox<UserRole> roleComboBox;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        // Initialize role combo box
        roleComboBox.getItems().addAll(UserRole.CLIENT);
        roleComboBox.setItems(FXCollections.observableArrayList(UserRole.values()));
    }

    @FXML
    private void handleLogin() {
        String username = loginUsername.getText();
        String password = loginPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Login Error", "Please fill in all fields");
            return;
        }

        try {
            User user = userDAO.getUserByUsername(username);
            if (user != null && HashUtil.verifyPassword(password, user.getPassword())) {
                openDashboard(user);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Error", "Invalid username or password");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred during login");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSignup() {
        String username = signupUsername.getText();
        String password = signupPassword.getText();
        String confirmPassword = signupConfirmPassword.getText();
        UserRole role = roleComboBox.getValue();
        // Validate input
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ) {
            showAlert(Alert.AlertType.ERROR, "Signup Error", "Please fill in all fields");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Signup Error", "Passwords do not match");
            return;
        }

        try {
            // Check if username already exists
            if (userDAO.getUserByUsername(username) != null) {
                showAlert(Alert.AlertType.ERROR, "Signup Error", "Username already exists");
                return;
            }

            // Create new user based on role
            User newUser = switch (role) {
                case CLIENT -> new Client(username, password);
                default -> throw new IllegalStateException("Unexpected role: " + role);
            };

            // Save user to database
            userDAO.saveUser(newUser);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully");
            clearSignupFields();
            mainTabPane.getSelectionModel().select(loginTab);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred during signup");
            e.printStackTrace();
        }
    }

    private void openDashboard(User user) {
        try {
            // Définissez le chemin relatif du fichier FXML en fonction du rôle de l'utilisateur
            String fxmlPath = switch (user.getRole()) {
                case CLIENT -> "/com/example/blockchainjava/Client/ClientDashBoard.fxml";
                case VALIDATOR -> "/com/example/blockchainjava/Validator/ValidatorDashboard.fxml";
                case ADMIN -> "/com/example/blockchainjava/Admin/AdminDashboard.fxml";
            };

            // Utilisez getClass().getResource() pour charger le fichier FXML avec le chemin relatif
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent dashboard = loader.load();

            // Affecter l'utilisateur au contrôleur approprié
            switch (user.getRole()) {
                case CLIENT -> ((ClientDashboardController) loader.getController()).setClient((Client) user);
                case VALIDATOR -> ((ValidatorDashboardController) loader.getController()).setValidator((Validator) user);
                case ADMIN -> ((AdminDashboardController) loader.getController()).setAdmin((Admin) user);
            }

            // Créer la nouvelle scène et la nouvelle fenêtre (stage)
            Stage dashboardStage = new Stage();
            dashboardStage.setScene(new Scene(dashboard));
            dashboardStage.setTitle(user.getRole() + " Dashboard - " + user.getUsername());
            dashboardStage.show();

            // Fermer la fenêtre de connexion
            ((Stage) loginUsername.getScene().getWindow()).close();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open dashboard");
            e.printStackTrace();
        }
    }


    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void clearSignupFields() {
        signupUsername.clear();
        signupPassword.clear();
        signupConfirmPassword.clear();
        roleComboBox.setValue(UserRole.CLIENT);
    }
}