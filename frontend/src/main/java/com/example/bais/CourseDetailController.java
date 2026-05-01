package com.example.bais;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CourseDetailController implements Initializable {

    @FXML private VBox courseRoot;

    record Task(String name, String points, String deadline, String status, String statusColor) {}

    private final List<Task> tasks = List.of(
        new Task("DÚ 1 – Základy matíc a operácií",        "10 / 10", "15.10.2024", "Odovzdané",  "#16a34a"),
        new Task("Semestrálna práca – časť 1",              "18 / 20", "05.11.2024", "Hodnotené",  "#2563eb"),
        new Task("DÚ 2 – Lineárne zobrazenia",              "—",       "20.11.2024", "Chýba",      "#dc2626"),
        new Task("Midterm test – písomná skúška",           "35 / 40", "28.11.2024", "Hodnotené",  "#2563eb"),
        new Task("Semestrálna práca – časť 2",              "—",       "10.01.2025", "Otvorené",   "#d97706"),
        new Task("DÚ 3 – Vlastné hodnoty a vektory",        "—",       "15.01.2025", "Otvorené",   "#d97706"),
        new Task("Záverečný projekt – prezentácia",         "—",       "24.01.2025", "Otvorené",   "#d97706")
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildUI();
    }

    private void buildUI() {
        courseRoot.getChildren().clear();
        courseRoot.setSpacing(16);
        courseRoot.setPadding(new Insets(24, 28, 24, 28));

        // Course header card
        HBox headerCard = new HBox(20);
        headerCard.getStyleClass().add("section-card");
        headerCard.setAlignment(Pos.CENTER_LEFT);

        // Left: course info
        VBox info = new VBox(8);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label title = new Label("Odovzdanie súborov");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");

        Label codeLabel = new Label("Nahrajte vaše vypracované zadania");
        codeLabel.getStyleClass().add("welcome-sub");

        // Tags row
        HBox tags = new HBox(8);
        tags.setAlignment(Pos.CENTER_LEFT);
        tags.getChildren().addAll(
            tag("Prednáška",       "#dbeafe", "#1d4ed8"),
            tag("Zimný sem.",      "#dcfce7", "#15803d"),
            tag("5 ECTS",         "#f0f9ff", "#0369a1"),
            tag("prof. Kováčová", "#fef3c7", "#92400e")
        );

        // Progress bar
        VBox progBox = new VBox(4);
        Label progLbl = new Label("Priebežné hodnotenie: 63 / 70 bodov  (90%)");
        progLbl.getStyleClass().add("schedule-date");
        StackPane bar = new StackPane();
        bar.setPrefHeight(6);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-background-color:#e2e8f0;-fx-background-radius:3;");
        StackPane fill = new StackPane();
        fill.setStyle("-fx-background-color:#06b6d4;-fx-background-radius:3;");
        fill.setPrefHeight(6);
        fill.prefWidthProperty().bind(bar.widthProperty().multiply(0.90));
        bar.getChildren().add(fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        progBox.getChildren().addAll(progLbl, bar);

        info.getChildren().addAll(title, codeLabel, tags, progBox);

        // Right: Upload section
        VBox uploadBox = new VBox(12);
        uploadBox.setAlignment(Pos.CENTER);
        uploadBox.setPadding(new Insets(0, 0, 0, 20));
        
        Button uploadBtn = new Button("📤 Nahrať súbor");
        uploadBtn.setStyle("-fx-background-color:#06b6d4;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:12 24;-fx-font-size:14px;-fx-background-radius:10;-fx-cursor:hand;");
        
        Label fileNameLabel = new Label("Žiadny vybraný súbor");
        fileNameLabel.getStyleClass().add("schedule-loc");
        
        uploadBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Vybrať súbor na odovzdanie");
            File selectedFile = fileChooser.showOpenDialog(uploadBtn.getScene().getWindow());
            if (selectedFile != null) {
                fileNameLabel.setText("Vybrané: " + selectedFile.getName());
                System.out.println("Pripravujem upload: " + selectedFile.getAbsolutePath());
                // Tu by sa napojil backend
            }
        });
        
        uploadBox.getChildren().addAll(uploadBtn, fileNameLabel);

        headerCard.getChildren().addAll(info, uploadBox);

        // Assignments card
        VBox assignCard = new VBox(12);
        assignCard.getStyleClass().add("section-card");

        Label assignTitle = new Label("Zadania a úlohy");
        assignTitle.getStyleClass().add("section-title");
        assignCard.getChildren().add(assignTitle);

        // Table header
        HBox tblHeader = taskRow("Zadanie", "Body", "Termín", "Stav", "#94a3b8", true);
        tblHeader.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;-fx-padding:0 0 8 0;");
        assignCard.getChildren().add(tblHeader);

        // Task rows
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            boolean last = i == tasks.size() - 1;
            HBox row = taskRow(t.name(), t.points(), t.deadline(), t.status(), t.statusColor(), false);
            if (!last) row.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;");
            assignCard.getChildren().add(row);
        }

        // Summary stats row
        HBox sumRow = new HBox(12);
        int done  = (int) tasks.stream().filter(t -> !t.points().equals("—")).count();
        int total = tasks.size();
        sumRow.getChildren().addAll(
            sumStat(done + " / " + total, "Odovzdané"),
            sumStat("63",                 "Získané body"),
            sumStat("70",                 "Max. body"),
            sumStat("90%",                "Plnenie")
        );

        courseRoot.getChildren().addAll(headerCard, assignCard, sumRow);
    }

    private HBox taskRow(String name, String pts, String deadline, String status, String statusColor, boolean header) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(header ? 4 : 10, 0, header ? 4 : 10, 0));

        Label nameLbl = new Label(name);
        HBox.setHgrow(nameLbl, Priority.ALWAYS);
        if (header) nameLbl.getStyleClass().add("schedule-date");
        else        nameLbl.getStyleClass().add("schedule-name");

        Label ptsLbl = new Label(pts);
        ptsLbl.setMinWidth(80);
        if (header) {
            ptsLbl.getStyleClass().add("schedule-date");
        } else {
            ptsLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" +
                (pts.equals("—") ? "#94a3b8" : "#06b6d4") + ";");
        }

        Label dlLbl = new Label(deadline);
        dlLbl.setMinWidth(100);
        if (header) dlLbl.getStyleClass().add("schedule-date");
        else        dlLbl.getStyleClass().add("schedule-loc");

        Label stLbl = new Label(status);
        stLbl.setMinWidth(90);
        if (header) {
            stLbl.getStyleClass().add("schedule-date");
        } else {
            stLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8 3 8;" +
                "-fx-background-radius:6;-fx-background-color:" + statusColor + "22;" +
                "-fx-text-fill:" + statusColor + ";");
        }

        row.getChildren().addAll(nameLbl, ptsLbl, dlLbl, stLbl);
        return row;
    }

    private VBox gradeCard(String value, String label) {
        VBox card = new VBox(2);
        card.getStyleClass().add("perf-card");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(80);

        Label val = new Label(value);
        val.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("perf-course");
        card.getChildren().addAll(val, lbl);
        return card;
    }

    private VBox sumStat(String value, String label) {
        VBox card = new VBox(2);
        card.getStyleClass().add("section-card");
        card.setAlignment(Pos.CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label val = new Label(value);
        val.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("perf-course");
        card.getChildren().addAll(val, lbl);
        return card;
    }

    private Label tag(String text, String bg, String fg) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8 3 8;" +
            "-fx-background-radius:6;-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";");
        return lbl;
    }
}
