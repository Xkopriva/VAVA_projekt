package com.example.bais;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SettingsController implements Initializable {

    @FXML private VBox settingsRoot;

    private UserProfile currentUserProfile;
    private boolean deadlineReminders = true;
    private boolean newGrades         = true;
    private boolean newMaterials      = true;
    private boolean teacherMessages   = false;
    private boolean systemAlerts      = true;
    private boolean cyanAccent        = true;

    private DashboardController dashboardController;
    private String  subscriptionId;
    private boolean profileReceived = false;

    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Ihneď zobraz dáta zo session (email + meno ak sú)
        loadFromSession();

        // 2. Pokús sa získať kompletný profil z backendu
        WebSocketClientService ws = WebSocketClientService.getInstance();
        subscriptionId = ws.subscribe("USER_PROFILE", this::handleProfile);
        ws.sendAction("GET_USER_PROFILE", null);

        // 3. Timeout 3s — ak backend nereaguje (napr. akcia neexistuje), unsubscribe
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            if (!profileReceived) ws.unsubscribe(subscriptionId);
        }, 3, TimeUnit.SECONDS);
    }

    private void loadFromSession() {
        String email    = UserSession.get().getUserEmail();
        String first    = UserSession.get().getFirstName();
        String last     = UserSession.get().getLastName();
        String fullName = (first + " " + last).trim();
        if (fullName.isEmpty()) fullName = email != null ? email : "—";

        currentUserProfile = new UserProfile(
                fullName,
                String.valueOf(UserSession.get().getUserId()),
                email != null ? email : "—",
                "N/A", "N/A");
        Platform.runLater(this::buildUI);
    }

    private void handleProfile(JsonNode node) {
        profileReceived = true;
        WebSocketClientService.getInstance().unsubscribe(subscriptionId);

        JsonNode data    = node.path("data");
        String firstName = data.path("firstName").asText("");
        String lastName  = data.path("lastName").asText("");
        String fullName  = (firstName + " " + lastName).trim();
        String email     = data.path("email").asText(UserSession.get().getUserEmail());

        UserSession.get().setFirstName(firstName);
        UserSession.get().setLastName(lastName);

        currentUserProfile = new UserProfile(
                fullName.isEmpty() ? email : fullName,
                String.valueOf(UserSession.get().getUserId()),
                email, "N/A", "N/A");
        Platform.runLater(this::buildUI);
    }

    private void buildUI() {
        settingsRoot.getChildren().clear();
        settingsRoot.setSpacing(16);
        settingsRoot.setPadding(new Insets(24, 28, 24, 28));

        boolean en = UserSession.get().isEnglish();

        VBox titleBlock = new VBox(8);
        Label title = new Label(en ? "Settings" : "Nastavenia");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(en ? "Manage your account, preferences and notifications"
                                 : "Spravuj účet, preferencie a upozornenia");
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);

        // ── Profil ────────────────────────────────────────────────
        VBox profileCard = buildSectionCard(en ? "👤  Profile" : "👤  Profil");

        String fullName  = currentUserProfile.getFullName();
        String studentId = currentUserProfile.getStudentId();
        String email     = currentUserProfile.getEmail();

        String initials = "?";
        if (!fullName.isEmpty() && !fullName.equals("—")) {
            String[] parts = fullName.split(" ");
            initials = parts.length >= 2
                    ? ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase()
                    : String.valueOf(parts[0].charAt(0)).toUpperCase();
        }

        StackPane avatar = new StackPane();
        avatar.setMinWidth(72); avatar.setMinHeight(72);
        Circle circle = new Circle(36, Color.web("#06b6d4"));
        Label initialsLbl = new Label(initials);
        initialsLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:white;");
        avatar.getChildren().addAll(circle, initialsLbl);

        VBox avatarInfo = new VBox(4);
        Label nameLbl    = new Label(fullName);
        nameLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;");
        nameLbl.getStyleClass().add("schedule-name");
        Label idEmailLbl = new Label(email + "  •  ID: " + studentId);
        idEmailLbl.getStyleClass().add("schedule-loc");
        Label progLabel  = new Label(en ? "Bc. Informatics • FIIT STU" : "Bc. Informatika • FIIT STU");
        progLabel.getStyleClass().add("schedule-loc");
        avatarInfo.getChildren().addAll(nameLbl, idEmailLbl, progLabel);

        HBox avatarRow = new HBox(16, avatar, avatarInfo);
        avatarRow.setAlignment(Pos.CENTER_LEFT);
        avatarRow.setPadding(new Insets(0, 0, 16, 0));
        profileCard.getChildren().add(avatarRow);
        profileCard.getChildren().addAll(
                separator(), formField(en ? "Full name"  : "Celé meno",  fullName,  false),
                separator(), formField(en ? "Student ID" : "ID študenta", studentId, false),
                separator(), formField("E-mail",                          email,     false)
        );

        HBox saveBtnRow = new HBox();
        saveBtnRow.setAlignment(Pos.CENTER_RIGHT);
        saveBtnRow.setPadding(new Insets(12, 0, 0, 0));
        Button saveBtn = new Button(en ? "Save changes" : "Uložiť zmeny");
        saveBtn.setStyle("-fx-background-color:#06b6d4;-fx-background-radius:10;-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-padding:10 40;-fx-font-size:13px;-fx-cursor:hand;");
        saveBtnRow.getChildren().add(saveBtn);
        profileCard.getChildren().add(saveBtnRow);

        // ── Upozornenia ───────────────────────────────────────────
        VBox notifCard = buildSectionCard(en ? "🔔  Notifications" : "🔔  Upozornenia");
        notifCard.getChildren().addAll(
            toggleRow(en ? "Deadline reminders"     : "Pripomienky deadlinov",
                      deadlineReminders, en ? "24h before submission" : "Upozornenie 24h pred odovzdaním",
                      () -> { deadlineReminders = !deadlineReminders; buildUI(); }),
            separator(),
            toggleRow(en ? "New grades"             : "Nové hodnotenia",
                      newGrades, en ? "On new grade" : "Pri novej známke",
                      () -> { newGrades = !newGrades; buildUI(); }),
            separator(),
            toggleRow(en ? "New materials"          : "Nové materiály",
                      newMaterials, en ? "New lectures and files" : "Nové prednášky a súbory",
                      () -> { newMaterials = !newMaterials; buildUI(); }),
            separator(),
            toggleRow(en ? "Messages from teachers" : "Správy od pedagógov",
                      teacherMessages, en ? "Email notifications" : "E-mailové notifikácie",
                      () -> { teacherMessages = !teacherMessages; buildUI(); }),
            separator(),
            toggleRow(en ? "System alerts"          : "Systémové upozornenia",
                      systemAlerts, en ? "Technical AIS alerts" : "Technické správy AIS",
                      () -> { systemAlerts = !systemAlerts; buildUI(); })
        );

        // ── Vzhľad ────────────────────────────────────────────────
        VBox appearCard = buildSectionCard(en ? "🎨  Appearance" : "🎨  Vzhľad");
        appearCard.getChildren().addAll(
            toggleRow(en ? "English Language" : "Anglický jazyk",
                      en, en ? "Interface in English" : "Rozhranie v angličtine",
                      () -> { UserSession.get().setEnglish(!UserSession.get().isEnglish());
                              if (dashboardController != null) dashboardController.toggleLanguage();
                              buildUI(); }),
            separator(),
            toggleRow(en ? "Dark / Light theme" : "Tmavý / Svetlý režim",
                      cyanAccent, en ? "Switch theme" : "Prepnúť tému",
                      () -> { cyanAccent = !cyanAccent;
                              if (dashboardController != null) dashboardController.toggleDarkMode(); })
        );

        settingsRoot.getChildren().addAll(titleBlock, profileCard, notifCard, appearCard);
    }

    // ── Helpers ───────────────────────────────────────────────────

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
        if (editable) {
            TextField tf = new TextField(value);
            tf.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:8;-fx-padding:7 10;" +
                    "-fx-border-color:#e2e8f0;-fx-border-radius:8;-fx-border-width:1;-fx-font-size:13px;");
            HBox.setHgrow(tf, Priority.ALWAYS);
            row.getChildren().addAll(lbl, tf);
        } else {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label val = new Label(value);
            val.getStyleClass().add("schedule-loc");
            row.getChildren().addAll(lbl, spacer, val);
        }
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
        t.setStyle("-fx-background-color:" + (on ? "#06b6d4" : "#e2e8f0") + ";-fx-background-radius:12;");
        Circle knob = new Circle(9, Color.WHITE);
        StackPane.setAlignment(knob, on ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        knob.setTranslateX(on ? -3 : 3);
        t.getChildren().add(knob);
        return t;
    }

    private Region separator() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color:#e2e8f0;");
        return sep;
    }
}
