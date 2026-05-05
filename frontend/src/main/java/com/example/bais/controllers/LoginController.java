package com.example.bais.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

import com.example.bais.models.UserSession;
import com.example.bais.services.WebSocketClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class LoginController implements Initializable {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button langButton;
    @FXML
    private ImageView langFlagImage;
    @FXML
    private Button darkModeToggle;
    @FXML
    private Label loginTitle;
    @FXML
    private Label loginSubtitle;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label passwordLabel;
    @FXML
    private Button loginButton;
    @FXML
    private Label loginSystemTitle;
    @FXML
    private Label loginAcademicYear;

    private boolean isDarkMode = false;
    private boolean isEnglish = false;
    private String subLogin;
    private String subError;

    // Lokálne JSON ukladanie
    private static final File SETTINGS_FILE = new File(System.getProperty("user.home"), ".bais-settings.json");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Načítame uložené preferencie hneď pri štarte
        loadSavedPreferences();

        // 2. Aktualizujeme texty a ikony
        updateLanguage();
        updateLanguageButton();
        updateDarkModeButton();

        // 3. Aplikujeme CSS tému (musí byť cez Platform.runLater, aby scéna stihla
        // vzniknúť)
        Platform.runLater(this::updateTheme);
    }

    private void loadSavedPreferences() {
        if (SETTINGS_FILE.exists()) {
            try {
                JsonNode root = JSON_MAPPER.readTree(SETTINGS_FILE);
                isDarkMode = root.path("isDarkMode").asBoolean(false);
                isEnglish = root.path("isEnglish").asBoolean(false);

                // Synchronizujeme globálnu session
                UserSession.get().setEnglish(isEnglish);
            } catch (IOException e) {
                System.err.println("[LOGIN] Nepodarilo sa prečítať nastavenia.");
            }
        }
    }

    private void saveSettingsToJson() {
        try {
            // Pre zachovanie ostatných nastavení (notifikácie) najprv načítame pôvodný
            // súbor
            ObjectNode root;
            if (SETTINGS_FILE.exists()) {
                root = (ObjectNode) JSON_MAPPER.readTree(SETTINGS_FILE);
            } else {
                root = JSON_MAPPER.createObjectNode();
            }

            root.put("isEnglish", isEnglish);
            root.put("isDarkMode", isDarkMode);

            JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(SETTINGS_FILE, root);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        if (!user.matches("^[\\w\\.-]+@(?:fiit\\.)?stuba\\.sk$")) {
            showAlert(Alert.AlertType.WARNING,
                    isEnglish ? "Invalid Email Domain" : "Neplatná doména",
                    isEnglish ? "Please use your university email (@stuba.sk / @fiit.stuba.sk)."
                            : "Prosím použite univerzitný email (@stuba.sk / @fiit.stuba.sk).");
            return;
        }

        WebSocketClientService ws = WebSocketClientService.getInstance();
        ws.connectAsync().thenRun(() -> {
            subLogin = ws.subscribe("LOGIN_SUCCESS", this::handleServerMessage);
            subError = ws.subscribe("ERROR", this::handleServerMessage);
            ws.sendAction("LOGIN", Map.of("email", user, "password", pass));
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR,
                        isEnglish ? "Connection Error" : "Chyba pripojenia",
                        isEnglish ? "Could not connect to server." : "Nepodarilo sa pripojiť k serveru.");
            });
            return null;
        });
    }

    private void handleServerMessage(JsonNode node) {
        String type = node.path("type").asText();
        if ("LOGIN_SUCCESS".equals(type)) {
            WebSocketClientService.getInstance().unsubscribe(subLogin);
            WebSocketClientService.getInstance().unsubscribe(subError);

            JsonNode data = node.path("data");
            Platform.runLater(() -> {
                UserSession session = UserSession.get();
                session.setUserId(data.path("userId").asInt());
                session.setUserEmail(data.path("email").asText());

                String role = data.path("role").asText();
                if ("ADMIN".equalsIgnoreCase(role))
                    session.setRole(UserSession.Role.ADMIN);
                else if ("TEACHER".equalsIgnoreCase(role))
                    session.setRole(UserSession.Role.TEACHER);
                else
                    session.setRole(UserSession.Role.STUDENT);

                session.setEnglish(isEnglish);
                navigateToDashboard();
            });
        } else if ("ERROR".equals(type)) {
            // Backend vždy posiela správy po slovensky — preložíme na frontende
            String message = isEnglish
                    ? "Incorrect email or password. Please try again."
                    : "Nesprávne prihlasovacie údaje. Skúste znova.";
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR,
                    isEnglish ? "Login Failed" : "Prihlásenie zlyhalo",
                    message));
        }
    }

    private void navigateToDashboard() {
        try {
            // SPRÁVNA CESTA pri behu z JARu
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("/com/example/bais/dashboard-view.fxml"));

            fxmlLoader.setCharset(java.nio.charset.StandardCharsets.UTF_8);
            Scene dashboardScene = new Scene(fxmlLoader.load(), 1280, 800);

            String css = isDarkMode ? "/dark.css" : "/light.css";
            dashboardScene.getStylesheets().add(getClass().getResource(css).toExternalForm());

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle(isEnglish ? "Academic System - Dashboard" : "Akademický Systém - Dashboard");
            stage.setScene(dashboardScene);

            Platform.runLater(() -> {
                stage.setMaximized(false);
                stage.setMaximized(true);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleLanguage() {
        isEnglish = !isEnglish;
        UserSession.get().setEnglish(isEnglish);
        updateLanguage();
        updateLanguageButton();
        saveSettingsToJson(); // Uložíme zmenu okamžite
    }

    @FXML
    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        updateTheme();
        updateDarkModeButton();
        saveSettingsToJson(); // Uložíme zmenu okamžite
    }

    private void updateLanguage() {
        if (loginTitle != null) {
            loginTitle.setText(isEnglish ? "Better Academic System" : "Lepší Akademický Systém");
            loginTitle.setAlignment(Pos.CENTER);
        }
        if (loginSubtitle != null) {
            loginSubtitle.setText(isEnglish ? "Enter your credentials" : "Zadaj svoje údaje");
            loginSubtitle.setAlignment(Pos.CENTER);
        }
        if (usernameLabel != null)
            usernameLabel.setText(isEnglish ? "Email" : "Email");
        if (passwordLabel != null)
            passwordLabel.setText(isEnglish ? "Password" : "Heslo");
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
            if (resource != null)
                langFlagImage.setImage(new Image(resource.toExternalForm()));
        }
    }

    private void updateTheme() {
        if (loginButton == null || loginButton.getScene() == null)
            return;
        Scene scene = loginButton.getScene();
        scene.getStylesheets().clear();
        String css = isDarkMode ? "/dark.css" : "/light.css";
        scene.getStylesheets().add(getClass().getResource(css).toExternalForm());
    }

    private void updateDarkModeButton() {
        if (darkModeToggle != null)
            darkModeToggle.setText(isDarkMode ? "☀" : "🌙");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
