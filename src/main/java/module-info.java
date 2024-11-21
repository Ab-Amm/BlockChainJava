module com.example.blockchainjava {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    exports com.example.blockchainjava.Controller to javafx.fxml;
    opens com.example.blockchainjava.Controller to javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;
    requires jbcrypt;
    requires annotations;
    requires mysql.connector.java;

    exports com.example.blockchainjava;
    opens com.example.blockchainjava to javafx.fxml;
    opens com.example.blockchainjava.Model.User to javafx.base;
}