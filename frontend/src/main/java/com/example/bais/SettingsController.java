package com.example.bais;

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

public class SettingsController implements Initializable {

    @FXML private VBox settingsRoot;

    private boolean deadlineReminders = true;
    private boolean newGrades = true;
    private boolean newMaterials = true;
    private boolean teacherMessages = false;
    private boolean systemAlerts = true;
    private boolean cyanAccent = true;
    private DashboardController dashboardController;

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildUI();
    }

    private void buildUI() {
        settingsRoot.getChildren().clear();
        settingsRoot.setSpacing(16);
        settingsRoot.setPadding(new Insets(24, 28, 24, 28));

        // Header
        VBox titleBlock = new VBox(8);
        boolean isEng = UserSession.get().isEnglish();

        Label title = new Label(isEng ? "Settings" : "Nastavenia");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(isEng ? "Manage your account, preferences and notifications" : "Spravuj účet, preferencie a upozornenia");
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);

        // Profile card
        VBox profileCard = buildSectionCard(isEng ? "👤  Profile" : "👤  Profil");

        // Avatar + name row
        HBox avatarRow = new HBox(16);
        avatarRow.setAlignment(Pos.CENTER_LEFT);
        avatarRow.setPadding(new Insets(0, 0, 16, 0));

        StackPane avatar = new StackPane();
        avatar.setMinWidth(72);
        avatar.setMinHeight(72);
        Circle circle = new Circle(36, Color.web("#06b6d4"));
        Label initials = new Label("JN");
        initials.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:white;");
        avatar.getChildren().addAll(circle, initials);

        VBox avatarInfo = new VBox(4);
        Label fullName = new Label("Jakub Novák");
        fullName.setStyle("-fx-font-size:16px;-fx-font-weight:bold;");
        fullName.getStyleClass().add("schedule-name");
        Label idLabel = new Label("xnovakj@stuba.sk  •  ID: s241234");
        idLabel.getStyleClass().add("schedule-loc");
        Label progLabel = new Label(isEng ? "Bc. Informatics • 2nd year • FIIT STU" : "Bc. Informatika • 2. ročník • FIIT STU");
        progLabel.getStyleClass().add("schedule-loc");
        Button changePhoto = new Button(isEng ? "Change photo" : "Zmeniť fotku");
        changePhoto.setStyle("-fx-background-color:transparent;-fx-border-color:#06b6d4;" +
            "-fx-border-radius:8;-fx-border-width:1;-fx-text-fill:#06b6d4;" +
            "-fx-font-weight:bold;-fx-padding:5 10;-fx-font-size:11px;-fx-cursor:hand;");
        avatarInfo.getChildren().addAll(fullName, idLabel, progLabel, changePhoto);

        avatarRow.getChildren().addAll(avatar, avatarInfo);
        profileCard.getChildren().add(avatarRow);

        // Profile fields
        profileCard.getChildren().add(separator());
        profileCard.getChildren().add(formField(isEng ? "Full name" : "Celé meno",       "Jakub Novák",             false));
        profileCard.getChildren().add(separator());
        profileCard.getChildren().add(formField(isEng ? "Student ID" : "ID študenta",     "s241234",                 false));
        profileCard.getChildren().add(separator());
        profileCard.getChildren().add(formField("E-mail",          "xnovakj@stuba.sk",        false));
        profileCard.getChildren().add(separator());
        profileCard.getChildren().add(formField(isEng ? "Phone" : "Telefón",         "+421 911 123 456",        true));
        profileCard.getChildren().add(separator());
        profileCard.getChildren().add(formField(isEng ? "Home language" : "Domovský jazyk",  "Slovenčina",              true));

        // Save button
        HBox saveBtnRow = new HBox();
        saveBtnRow.setAlignment(Pos.CENTER_RIGHT);
        saveBtnRow.setPadding(new Insets(12, 0, 0, 0));
        Button saveBtn = new Button(isEng ? "Save changes" : "Uložiť zmeny");
        saveBtn.setStyle("-fx-background-color:#06b6d4;-fx-background-radius:10;" +
            "-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:10 40;-fx-font-size:13px;-fx-cursor:hand;-fx-min-width:200;");
        saveBtnRow.getChildren().add(saveBtn);
        profileCard.getChildren().add(saveBtnRow);

        // Notifications card
        VBox notifCard = buildSectionCard(isEng ? "🔔  Notifications" : "🔔  Upozornenia");
        notifCard.getChildren().addAll(
            toggleRow(isEng ? "Deadline reminders" : "Pripomienky deadlinov",    deadlineReminders,  isEng ? "24h before submission" : "Upozornenie 24h pred odovzdaním", () -> deadlineReminders = !deadlineReminders),
            separator(),
            toggleRow(isEng ? "New grades" : "Nové hodnotenia",           newGrades,  isEng ? "Notification on new grade" : "Notifikácia pri novej známke", () -> newGrades = !newGrades),
            separator(),
            toggleRow(isEng ? "New materials" : "Nové materiály",            newMaterials,  isEng ? "New lectures and files" : "Nové prednášky a súbory", () -> newMaterials = !newMaterials),
            separator(),
            toggleRow(isEng ? "Messages from teachers" : "Správy od pedagógov",       teacherMessages, isEng ? "Email notifications" : "E-mailové notifikácie", () -> teacherMessages = !teacherMessages),
            separator(),
            toggleRow(isEng ? "System alerts" : "Systémové upozornenia",     systemAlerts,  isEng ? "Technical AIS alerts" : "Technické správy systému AIS", () -> systemAlerts = !systemAlerts)
        );

        // Appearance card
        VBox appearCard = buildSectionCard(isEng ? "🎨  Appearance" : "🎨  Vzhľad");

        appearCard.getChildren().addAll(
            toggleRow(isEng ? "English Language" : "Anglický jazyk", isEng, isEng ? "Interface is in English" : "Rozhranie je v angličtine", () -> {
                UserSession.get().setEnglish(!isEng);
                if (dashboardController != null) {
                    dashboardController.toggleLanguage();
                    buildUI(); // Refresh settings UI too
                }
            }),
            separator(),
            toggleRow(isEng ? "Accent Color" : "Farba zvýraznenia", cyanAccent, isEng ? "Cyan Theme" : "Tyrkysová téma", () -> {
                cyanAccent = !cyanAccent;
                if (dashboardController != null) {
                    dashboardController.toggleDarkMode();
                }
            }),
            separator(),
            formField(isEng ? "Aspect Ratio" : "Pomer strán", "9:16", true)
        );

        settingsRoot.getChildren().addAll(titleBlock, profileCard, notifCard, appearCard);
    }

    private VBox buildSectionCard(String titleText) {
        VBox card = new VBox(0);
        card.getStyleClass().add("section-card");
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        title.setPadding(new Insets(0, 0, 12, 0));
        card.getChildren().add(title);
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

    private HBox toggleRow(String label, boolean on, String description, Runnable onToggle) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setStyle("-fx-cursor:hand;");

        VBox labelBox = new VBox(2);
        HBox.setHgrow(labelBox, Priority.ALWAYS);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("schedule-name");
        Label desc = new Label(description);
        desc.getStyleClass().add("schedule-loc");
        labelBox.getChildren().addAll(lbl, desc);

        // Toggle button visual
        StackPane toggle = toggleRowUI(on);

        row.getChildren().addAll(labelBox, toggle);

        row.setOnMouseClicked(e -> {
            if (onToggle != null) {
                onToggle.run();
            }
            buildUI();
        });

        return row;
    }

    private StackPane toggleRowUI(boolean on) {
        StackPane toggle = new StackPane();
        toggle.setMinWidth(44);
        toggle.setMinHeight(24);
        toggle.setStyle("-fx-background-color:" + (on ? "#06b6d4" : "#e2e8f0") + ";-fx-background-radius:12;");
        Circle knob = new Circle(9, Color.WHITE);
        StackPane.setAlignment(knob, on ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        knob.setTranslateX(on ? -3 : 3);
        toggle.getChildren().add(knob);
        return toggle;
    }

    private HBox settingRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setStyle("-fx-cursor:hand;");
        row.setOnMouseClicked(e -> {
            if (!label.equals("Jazyk") && !label.equals("Téma")) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Možnosť '" + label + "' bola kliknutá.", ButtonType.OK);
                a.showAndWait();
            }
        });

        Label lbl = new Label(label);
        lbl.getStyleClass().add("schedule-name");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        Label val = new Label(value);
        val.getStyleClass().add("schedule-loc");

        Label arrow = new Label("›");
        arrow.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:18px;-fx-padding:0 0 0 8;");

        row.getChildren().addAll(lbl, val, arrow);
        return row;
    }

    private Region separator() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color:#e2e8f0;");
        return sep;
    }
}
