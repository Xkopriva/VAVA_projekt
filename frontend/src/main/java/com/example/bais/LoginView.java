package com.example.bais;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

public class LoginView extends Application {

    private void handleLogin(String username, String password) {
        if ("test".equals(username) && "test".equals(password)) {
            System.out.println("Login úspešný! Prepínam na Dashboard...");
        } else {
            System.out.println("Nesprávne meno alebo heslo.");
        }
    }
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
        scene.getStylesheets().add(getClass().getResource("/light.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }
}