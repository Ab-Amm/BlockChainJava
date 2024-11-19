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
import java.net.URL;

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
        System.out.println("values "+username + " " + password);

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Login Error", "Please fill in all fields");
            return;
        }

        try {
            User user = userDAO.getUserByUsername(username);
            user.toString();
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
            // Vérification 1 : L'utilisateur n'est pas nul
            if (user == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "User object is null");
                System.err.println("User object is null");
                return;
            }

            // Affichage du rôle de l'utilisateur
            System.out.println("User role: " + user.getRole());

            // Définir le chemin relatif du fichier FXML en fonction du rôle de l'utilisateur
            String fxmlPath = switch (user.getRole()) {
                case CLIENT -> "/com/example/blockchainjava/Client/ClientDashBoard.fxml";
                case VALIDATOR -> "/com/example/blockchainjava/Validator/ValidatorDashboard.fxml";
                case ADMIN -> "/com/example/blockchainjava/Admin/AdminDashboard.fxml";
                default -> null; // Pour détecter un rôle inconnu
            };

            // Vérification 2 : Le chemin FXML n'est pas nul
            if (fxmlPath == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Unknown user role: " + user.getRole());
                System.err.println("Unknown user role: " + user.getRole());
                return;
            }

            // Vérification 3 : Vérifier si le fichier FXML existe
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "FXML file not found at path: " + fxmlPath);
                System.err.println("FXML file not found at path: " + fxmlPath);
                return;
            } else {
                System.out.println("FXML file found at: " + fxmlUrl);
            }

            // Charger le fichier FXML
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent dashboard = null;

            // Vérification 4 : Tenter de charger le fichier FXML
            try {
                dashboard = loader.load();
                System.out.println("FXML loaded successfully: " + fxmlPath);
            } catch (Exception e) {
                System.err.println("Failed to load FXML file: " + fxmlPath);
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Could not load FXML file");
                return;
            }

            // Vérification 5 : Affecter l'utilisateur au contrôleur
            try {
                switch (user.getRole()) {
                    case CLIENT -> ((ClientDashboardController) loader.getController()).setClient((Client) user);
                    case VALIDATOR -> ((ValidatorDashboardController) loader.getController()).setValidator((Validator) user);
                    case ADMIN -> ((AdminDashboardController) loader.getController()).setAdmin((Admin) user);
                }
                System.out.println("Controller set successfully for role: " + user.getRole());
            } catch (Exception e) {
                System.err.println("Failed to set controller for role: " + user.getRole());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Could not set controller");
                return;
            }

            // Créer la nouvelle scène et la fenêtre
            Stage dashboardStage = new Stage();
            dashboardStage.setScene(new Scene(dashboard));
            dashboardStage.setTitle(user.getRole() + " Dashboard - " + user.getUsername());
            dashboardStage.show();
            System.out.println("Dashboard opened successfully for: " + user.getUsername());

            // Fermer la fenêtre de connexion
            ((Stage) loginUsername.getScene().getWindow()).close();

        } catch (Exception e) {
            System.err.println("Unexpected error in openDashboard method");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open dashboard");
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