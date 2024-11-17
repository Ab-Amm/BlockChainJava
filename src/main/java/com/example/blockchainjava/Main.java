package com.example.blockchainjava;

import com.example.blockchainjava.Model.DAO.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/hello-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            primaryStage.setTitle("Blockchain Application");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Initialize database connection
        DatabaseConnection.getConnection();

        // Start the application
        launch(args);
    }

    @Override
    public void stop() {
        // Cleanup resources when application closes
        try {
            if (DatabaseConnection.getConnection() != null) {
                DatabaseConnection.getConnection().close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}