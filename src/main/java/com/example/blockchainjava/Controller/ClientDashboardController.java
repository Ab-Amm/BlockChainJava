package com.example.blockchainjava.Controller;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import com.example.blockchainjava.Model.User.Client;



public class ClientDashboardController {

    @FXML private Label usernameLabel;
    @FXML private Label balanceLabel;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
        updateDashboard();
    }

    private void updateDashboard() {
        if (client != null) {
            usernameLabel.setText(client.getUsername());
            //balanceLabel.setText(String.format("$%.2f", client.getBalance()));
        }
    }
}

