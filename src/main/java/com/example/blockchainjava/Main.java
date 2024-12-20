package com.example.blockchainjava;

import com.example.blockchainjava.Model.Block.BlockChain;
import com.example.blockchainjava.Model.DAO.DatabaseConnection;
import com.example.blockchainjava.Model.User.Validator;
import com.example.blockchainjava.Util.Network.SocketClient;
import com.example.blockchainjava.Util.Network.SocketServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/example/blockchainjava/hello-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/example/blockchainjava/CSS/style.css")).toExternalForm());

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
        BlockChain blockChain = new BlockChain();
        boolean isBlockchainValid = blockChain.verifyBlockchain();
        if (!isBlockchainValid) {
            System.err.println("La blockchain est invalide. Vérifiez les données avant de continuer.");
            System.exit(1); // Arrêter l'application si la blockchain est invalide
        }

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