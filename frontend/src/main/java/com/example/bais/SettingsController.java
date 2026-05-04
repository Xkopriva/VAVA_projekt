package com.example.bais;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SettingsController implements Initializable {

    @FXML private VBox settingsRoot;

    // Prednastavené (default) hodnoty
    private boolean deadlineReminders = true;
    private boolean newGrades         = true;
    private boolean newMaterials      = true;
    private boolean teacherMessages   = false;
    private boolean systemAlerts      = true;
    private boolean isDarkMode        = false;

    private UserProfile currentUserProfile;
    private DashboardController dashboardController;
    private String subscriptionId;
    private boolean profileReceived = false;

    private static final File SETTINGS_FILE = new File(System.getProperty("user.home"), ".bais-settings.json");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
        // Po priradení kontroléra skúsime aplikovať dark mode, ak je zapnutý
        if (isDarkMode && dashboardController != null) {
            Platform.runLater(() -> dashboardController.toggleDarkMode());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadSettingsFromJson();
        loadFromSession();

        WebSocketClientService ws = WebSocketClientService.getInstance();
        subscriptionId = ws.subscribe("USER_PROFILE", this::handleProfile);
        ws.sendAction("GET_USER_PROFILE", null);

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            if (!profileReceived) ws.unsubscribe(subscriptionId);
        }, 3, TimeUnit.SECONDS);
    }

    private void loadSettingsFromJson() {
        if (!SETTINGS_FILE.exists()) {
            // Ak súbor neexistuje, vytvoríme ho s default hodnotami
            saveSettingsToJson();
            return;
        }

        try {
            JsonNode root = JSON_MAPPER.readTree(SETTINGS_FILE);
            deadlineReminders = root.path("deadlineReminders").asBoolean(true);
            newGrades         = root.path("newGrades").asBoolean(true);
            newMaterials      = root.path("newMaterials").asBoolean(true);
            teacherMessages   = root.path("teacherMessages").asBoolean(false);
            systemAlerts      = root.path("systemAlerts").asBoolean(true);
            isDarkMode        = root.path("isDarkMode").asBoolean(false);

            if (root.has("isEnglish")) {
                UserSession.get().setEnglish(root.path("isEnglish").asBoolean(false));
            }
        } catch (IOException e) {
            System.err.println("Nepodarilo sa načítať nastavenia, používam predvolené.");
            e.printStackTrace();
        }
    }

    private void saveSettingsToJson() {
        try {
            ObjectNode root = JSON_MAPPER.createObjectNode();
            root.put("deadlineReminders", deadlineReminders);
            root.put("newGrades", newGrades);
            root.put("newMaterials", newMaterials);
            root.put("teacherMessages", teacherMessages);
            root.put("systemAlerts", systemAlerts);
            root.put("isEnglish", UserSession.get().isEnglish());
            root.put("isDarkMode", isDarkMode);

            JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(SETTINGS_FILE, root);
            System.out.println("Nastavenia uložené do: " + SETTINGS_FILE.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Chyba pri ukladaní nastavení.");
            e.printStackTrace();
        }
    }

    private void loadFromSession() {
        String email = UserSession.get().getUserEmail();
        String first = UserSession.get().getFirstName();
        String last  = UserSession.get().getLastName();
        String fullName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        if (fullName.isEmpty()) fullName = email != null ? email : "—";

        currentUserProfile = new UserProfile(fullName, String.valueOf(UserSession.get().getUserId()), email != null ? email : "—", "N/A", "N/A");
        Platform.runLater(this::buildUI);
    }

    private void handleProfile(JsonNode node) {
        profileReceived = true;
        WebSocketClientService.getInstance().unsubscribe(subscriptionId);
        JsonNode data = node.path("data");
        String firstName = data.path("firstName").asText("");
        String lastName  = data.path("lastName").asText("");
        String email     = data.path("email").asText(UserSession.get().getUserEmail());

        UserSession.get().setFirstName(firstName);
        UserSession.get().setLastName(lastName);

        String fullName = (firstName + " " + lastName).trim();
        currentUserProfile = new UserProfile(fullName.isEmpty() ? email : fullName, String.valueOf(UserSession.get().getUserId()), email, "N/A", "N/A");
        Platform.runLater(this::buildUI);
    }

    private void buildUI() {
        if (settingsRoot == null) return;

        settingsRoot.getChildren().clear();
        settingsRoot.setSpacing(16);
        settingsRoot.setPadding(new Insets(24, 28, 24, 28));

        boolean en = UserSession.get().isEnglish();

        // Nadpis
        VBox titleBlock = new VBox(8);
        Label title = new Label(en ? "Settings" : "Nastavenia");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(en ? "Manage your account, preferences and notifications" : "Spravuj účet, preferencie a upozornenia");
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);

        // Profil
        VBox profileCard = buildSectionCard(en ? "👤  Profile" : "👤  Profil");
        setupProfileSection(profileCard, en);

        // Notifikácie
        VBox notifCard = buildSectionCard(en ? "🔔  Notifications" : "🔔  Upozornenia");
        notifCard.getChildren().addAll(
                toggleRow(en ? "Deadline reminders" : "Pripomienky deadlinov", deadlineReminders, en ? "24h before submission" : "Upozornenie 24h pred odovzdaním", () -> { deadlineReminders = !deadlineReminders; updateAndSave(); }),
                separator(),
                toggleRow(en ? "New grades" : "Nové hodnotenia", newGrades, en ? "On new grade" : "Pri novej známke", () -> { newGrades = !newGrades; updateAndSave(); }),
                separator(),
                toggleRow(en ? "New materials" : "Nové materiály", newMaterials, en ? "New lectures and files" : "Nové prednášky a súbory", () -> { newMaterials = !newMaterials; updateAndSave(); }),
                separator(),
                toggleRow(en ? "Messages from teachers" : "Správy od pedagógov", teacherMessages, en ? "Email notifications" : "E-mailové notifikácie", () -> { teacherMessages = !teacherMessages; updateAndSave(); }),
                separator(),
                toggleRow(en ? "System alerts" : "Systémové upozornenia", systemAlerts, en ? "Technical AIS alerts" : "Technické správy AIS", () -> { systemAlerts = !systemAlerts; updateAndSave(); })
        );

        // Vzhľad
        VBox appearCard = buildSectionCard(en ? "🎨  Appearance" : "🎨  Vzhľad");
        appearCard.getChildren().addAll(
                toggleRow(en ? "English Language" : "Anglický jazyk", UserSession.get().isEnglish(), en ? "Interface in English" : "Rozhranie v angličtine", () -> {
                    UserSession.get().setEnglish(!UserSession.get().isEnglish());
                    if (dashboardController != null) dashboardController.toggleLanguage();
                    updateAndSave();
                }),
                separator(),
                toggleRow(en ? "Dark mode" : "Tmavý režim", isDarkMode, en ? "Switch between dark and light theme" : "Prepínanie medzi tmavou a svetlou témou", () -> {
                    isDarkMode = !isDarkMode;
                    if (dashboardController != null) dashboardController.toggleDarkMode();
                    updateAndSave();
                })
        );

        settingsRoot.getChildren().addAll(titleBlock, profileCard, notifCard, appearCard);
    }

    private void updateAndSave() {
        saveSettingsToJson();
        buildUI();
    }

    private void setupProfileSection(VBox card, boolean en) {
        String fullName = currentUserProfile.getFullName();
        String initials = "??";
        if (fullName != null && !fullName.isEmpty() && !fullName.equals("—")) {
            String[] parts = fullName.split("\\s+");
            initials = parts.length >= 2 ? (parts[0].substring(0,1) + parts[parts.length-1].substring(0,1)).toUpperCase() : parts[0].substring(0,1).toUpperCase();
        }

        HBox avatarRow = new HBox(16);
        avatarRow.setAlignment(Pos.CENTER_LEFT);
        avatarRow.setPadding(new Insets(0, 0, 16, 0));

        StackPane avatar = new StackPane();
        Circle circle = new Circle(36, Color.web("#06b6d4"));
        Label initialsLbl = new Label(initials);
        initialsLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:white;");
        avatar.getChildren().addAll(circle, initialsLbl);

        VBox info = new VBox(4);
        Label nameLbl = new Label(fullName);
        nameLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;");
        nameLbl.getStyleClass().add("schedule-name");
        Label subLbl = new Label(currentUserProfile.getEmail() + "  •  ID: " + currentUserProfile.getStudentId());
        subLbl.getStyleClass().add("schedule-loc");
        info.getChildren().addAll(nameLbl, subLbl);

        avatarRow.getChildren().addAll(avatar, info);
        card.getChildren().addAll(avatarRow, separator(),
                formField(en ? "Full name" : "Celé meno", fullName, false), separator(),
                formField("E-mail", currentUserProfile.getEmail(), false));
    }

    private VBox buildSectionCard(String titleText) {
        VBox card = new VBox(0);
        card.getStyleClass().add("section-card");
        Label t = new Label(titleText);
        t.getStyleClass().add("section-title");
        t.setPadding(new Insets(0, 0, 12, 0));
        card.getChildren().add(t);
        return card;
    }

    private HBox formField(String label, String value, boolean editable) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        Label lbl = new Label(label);
        lbl.getStyleClass().add("schedule-name");
        lbl.setMinWidth(160);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label val = new Label(value);
        val.getStyleClass().add("schedule-loc");
        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }

    private HBox toggleRow(String label, boolean on, String desc, Runnable onToggle) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setStyle("-fx-cursor:hand;");
        VBox box = new VBox(2);
        HBox.setHgrow(box, Priority.ALWAYS);
        Label l = new Label(label); l.getStyleClass().add("schedule-name");
        Label d = new Label(desc);  d.getStyleClass().add("schedule-loc");
        box.getChildren().addAll(l, d);
        StackPane toggle = buildToggle(on);
        row.getChildren().addAll(box, toggle);
        row.setOnMouseClicked(e -> { if (onToggle != null) onToggle.run(); });
        return row;
    }

    private StackPane buildToggle(boolean on) {
        StackPane t = new StackPane();
        t.setMinWidth(44); t.setMinHeight(24);
        t.setMaxWidth(44);
        t.setStyle("-fx-background-color:" + (on ? "#06b6d4" : "#e2e8f0") + ";-fx-background-radius:12;");
        Circle knob = new Circle(9, Color.WHITE);
        StackPane.setAlignment(knob, on ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        knob.setTranslateX(on ? -4 : 4);
        t.getChildren().add(knob);
        return t;
    }

    private Region separator() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#e2e8f0;");
        return sep;
    }
}