package com.example.bais;
import com.example.bais.controllers.*;
import com.example.bais.models.*;
import com.example.bais.services.*;
import com.example.bais.components.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;

public class LoginView extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("login-view.fxml"));
        fxmlLoader.setCharset(StandardCharsets.UTF_8);
        Scene scene = new Scene(fxmlLoader.load(), 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/light.css").toExternalForm());

        stage.setTitle("BAIS – Lepší Akademický Systém");
        stage.setScene(scene);

        stage.setMaximized(true);

        UserSession.get().setPrimaryStage(stage);

        stage.show();

        Platform.runLater(() -> {
            stage.setMaximized(false); 
            stage.setMaximized(true);
        });
    }
}
