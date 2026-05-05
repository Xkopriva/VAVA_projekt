module com.example.bais {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.java_websocket;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires java.xml;

    opens com.example.bais to javafx.fxml;
    opens com.example.bais.controllers to javafx.fxml;
    exports com.example.bais.controllers;
    exports com.example.bais.models;
    exports com.example.bais.services;
    exports com.example.bais.components;
    exports com.example.bais;
}
