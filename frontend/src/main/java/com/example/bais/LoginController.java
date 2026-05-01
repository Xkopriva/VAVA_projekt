package com.example.bais;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

import com.fasterxml.jackson.databind.JsonNode;

public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button langButton;
    @FXML private ImageView langFlagImage;
    @FXML private Button darkModeToggle;
    @FXML private Label loginTitle;
    @FXML private Label loginSubtitle;
    @FXML private Label usernameLabel;
    @FXML private Label passwordLabel;
    @FXML private Button loginButton;
    @FXML private Label loginSystemTitle;
    @FXML private Label loginAcademicYear;

    private boolean isDarkMode = false;
    private boolean isEnglish = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        isDarkMode = false;
        isEnglish = false;
        updateLanguage();
        updateLanguageButton();
        updateDarkModeButton();
    }

    @FXML
    private void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, 
                isEnglish ? "Missing Credentials" : "Chýbajúce údaje",
                isEnglish ? "Please enter email and password." : "Prosím zadajte email a heslo.");
            return;
        }

        WebSocketClientService ws = WebSocketClientService.getInstance();
        ws.connectAsync().thenRun(() -> {
            ws.setOnMessageCallback(this::handleServerMessage);
            ws.sendAction("LOGIN", Map.of("email", user, "password", pass));
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, 
                    isEnglish ? "Connection Error" : "Chyba pripojenia",
                    isEnglish ? "Could not connect to the server." : "Nepodarilo sa pripojiť k serveru.");
            });
            return null;
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleServerMessage(JsonNode node) {
        String type = node.path("type").asText();
        if ("LOGIN_SUCCESS".equals(type)) {
            JsonNode data = node.path("data");
            Platform.runLater(() -> {
                UserSession session = UserSession.get();
                session.setUserId(data.path("userId").asInt());
                session.setUserEmail(data.path("email").asText());
                String role = data.path("role").asText();
                if ("ADMIN".equalsIgnoreCase(role)) {
                    session.setRole(UserSession.Role.ADMIN);
                } else if ("TEACHER".equalsIgnoreCase(role)) {
                    session.setRole(UserSession.Role.TEACHER);
                } else {
                    session.setRole(UserSession.Role.STUDENT);
                }
                session.setEnglish(isEnglish);
                navigateToDashboard();
            });
        } else if ("ERROR".equals(type)) {
            String message = node.path("message").asText();
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, 
                    isEnglish ? "Login Failed" : "Prihlásenie zlyhalo", 
                    message);
            });
        }
    }

    private void navigateToDashboard() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("dashboard-view.fxml"));
            Scene dashboardScene = new Scene(fxmlLoader.load(), 1280, 850);
            if (isDarkMode) {
                dashboardScene.getStylesheets().add(getClass().getResource("/dark.css").toExternalForm());
            } else {
                dashboardScene.getStylesheets().add(getClass().getResource("/light.css").toExternalForm());
            }
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(dashboardScene);
            stage.setTitle(isEnglish ? "Academic System - Dashboard" : "Akademický Systém - Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleLanguage() {
        isEnglish = !isEnglish;
        updateLanguage();
        updateLanguageButton();
    }

    @FXML
    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        updateTheme();
        updateDarkModeButton();
    }

    private void updateLanguage() {
        if (loginTitle != null)
            loginTitle.setText(isEnglish ? "Better Academic System" : "Lepší Akademický Systém");
        if (loginSubtitle != null)
            loginSubtitle.setText(isEnglish
                ? "Enter your credentials to view your grades"
                : "zadaj svoje údaje aby si zistil svoje známky");
        if (usernameLabel != null)
            usernameLabel.setText(isEnglish ? "Name" : "Meno");
        if (passwordLabel != null)
            passwordLabel.setText(isEnglish ? "Password" : "Heslo");
        if (usernameField != null)
            usernameField.setPromptText(isEnglish ? "username" : "xpriezvisko");
        if (passwordField != null)
            passwordField.setPromptText("•••••••");
        if (loginButton != null)
            loginButton.setText(isEnglish ? "Sign In" : "Prihlásiť sa");
        if (loginSystemTitle != null)
            loginSystemTitle.setText(isEnglish ? "Better Academic System" : "Lepší Akademický Systém");
        if (loginAcademicYear != null)
            loginAcademicYear.setText(isEnglish ? "Academic Year 2026" : "Akademický rok 2026");
    }

    private void updateLanguageButton() {
        if (langFlagImage != null) {
            String path = isEnglish ? "/images/United_Kingdom.png" : "/images/Slovakia.png";
            URL resource = getClass().getResource(path);
            if (resource != null) {
                langFlagImage.setImage(new Image(resource.toExternalForm()));
            }
        }
    }

    private void updateTheme() {
        Scene scene = darkModeToggle.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            if (isDarkMode) {
                scene.getStylesheets().add(getClass().getResource("/dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/light.css").toExternalForm());
            }
        }
    }

    private void updateDarkModeButton() {
        if (darkModeToggle != null) {
            darkModeToggle.setText(isDarkMode ? "☀" : "🌙");
        }
    }
}
