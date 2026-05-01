package com.example.bais;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.fasterxml.jackson.databind.JsonNode;

public class GradesController implements Initializable {

    @FXML private VBox gradesRoot;

    record Course(String code, String name, int credits, String midterm, String finalGrade, String statusText, boolean done) {}

    private final List<Course> courses = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadGradesFromBackend();
    }

    private void loadGradesFromBackend() {
        WebSocketClientService ws = WebSocketClientService.getInstance();
        ws.setOnMessageCallback(this::handleServerMessage);
        ws.sendAction("GET_MY_ENROLLMENTS", null);
    }

    private void handleServerMessage(JsonNode node) {
        String type = node.path("type").asText();
        if ("ENROLLMENTS_LIST".equals(type)) {
            JsonNode data = node.path("data");
            courses.clear();
            if (data.isArray()) {
                for (JsonNode enrollment : data) {
                    // Pozor: Backend v ENROLLMENTS_LIST posiela len ID predmetu, 
                    // pre plne prepojenie by backend mal posielat aj detaily predmetu.
                    // Kedze backend NEMENIM, budem zobrazovat co mam.
                    int subId = enrollment.path("subjectId").asInt();
                    String status = enrollment.path("status").asText();
                    int attempts = enrollment.path("attemptNumber").asInt();
                    
                    courses.add(new Course(
                        "ID: " + subId, 
                        "Subject " + subId, 
                        0, 
                        "—", 
                        "—", 
                        status,
                        "PASSED".equals(status)
                    ));
                }
            }
            Platform.runLater(this::buildUI);
        }
    }

    private void buildUI() {
        gradesRoot.getChildren().clear();
        gradesRoot.setSpacing(16);
        gradesRoot.setPadding(new Insets(24, 28, 24, 28));

        // Title
        VBox titleBlock = new VBox(4);
        Label title = new Label("Letný semester 2024/25");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label("Výsledky predmetov a hodnotenie");
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);

        // Stats row
        HBox stats = new HBox(12);
        stats.getChildren().addAll(
            statCard("📚", "Predmety",   "5"),
            statCard("⭐", "Priemer GPA", "1.2"),
            statCard("📦", "Kredity",    "25 / 30"),
            statCard("✅", "Ukončené",   "3 / 5")
        );

        // Course table card
        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("section-card");
        Label tableTitle = new Label("Stav predmetov");
        tableTitle.getStyleClass().add("section-title");
        tableCard.getChildren().add(tableTitle);

        // Header row
        HBox headerRow = tableRow(true,
            "Kód", "Predmet", "Kredity", "Priebežná", "Záverečná", "Stav");
        headerRow.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;-fx-padding:0 0 8 0;");
        tableCard.getChildren().add(headerRow);

        // Data rows
        for (int i = 0; i < courses.size(); i++) {
            Course c = courses.get(i);
            boolean last = i == courses.size() - 1;

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 0, 10, 0));
            if (!last) row.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;");

            Label code = new Label(c.code());
            code.setStyle("-fx-font-weight:bold;-fx-text-fill:#06b6d4;-fx-font-size:13px;");
            code.setMinWidth(50);

            Label name = new Label(c.name());
            name.getStyleClass().add("schedule-name");
            HBox.setHgrow(name, Priority.ALWAYS);

            Label cred = new Label(String.valueOf(c.credits()));
            cred.setStyle("-fx-font-size:13px;");
            cred.getStyleClass().add("schedule-name");
            cred.setMinWidth(60);

            Label mid = new Label(c.midterm());
            mid.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + gradeColor(c.midterm()) + ";");
            mid.setMinWidth(80);

            Label fin = new Label(c.finalGrade());
            fin.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + gradeColor(c.finalGrade()) + ";");
            fin.setMinWidth(80);

            Label status = new Label(c.statusText());
            status.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8 3 8;-fx-background-radius:6;" +
                (c.done()
                    ? "-fx-background-color:#dcfce7;-fx-text-fill:#15803d;"
                    : "-fx-background-color:#fff7ed;-fx-text-fill:#d97706;"));

            row.getChildren().addAll(code, name, cred, mid, fin, status);
            tableCard.getChildren().add(row);
        }

        // Grade legend
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(4, 0, 0, 0));
        legend.getChildren().addAll(
            legendItem("#16a34a", "A – Výborný (1.0)"),
            legendItem("#2563eb", "B – Veľmi dobrý (1.5)"),
            legendItem("#d97706", "C – Dobrý (2.0)")
        );

        gradesRoot.getChildren().addAll(titleBlock, stats, tableCard, legend);
    }

    private VBox statCard(String icon, String label, String value) {
        VBox card = new VBox(4);
        card.getStyleClass().add("section-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(160);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label ic = new Label(icon);
        ic.setStyle("-fx-font-size:22px;");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("perf-course");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");

        card.getChildren().addAll(ic, lbl, val);
        return card;
    }

    private HBox tableRow(boolean isHeader, String... cols) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        int[] widths = {50, -1, 60, 80, 80, 80};
        for (int i = 0; i < cols.length; i++) {
            Label lbl = new Label(cols[i]);
            if (isHeader) {
                lbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;");
                lbl.getStyleClass().add("schedule-date");
            }
            if (widths[i] > 0) lbl.setMinWidth(widths[i]);
            else HBox.setHgrow(lbl, Priority.ALWAYS);
            row.getChildren().add(lbl);
        }
        return row;
    }

    private HBox legendItem(String color, String text) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5, Color.web(color));
        Label lbl = new Label(text);
        lbl.getStyleClass().add("schedule-loc");
        item.getChildren().addAll(dot, lbl);
        return item;
    }

    private String gradeColor(String g) {
        if (g == null || g.equals("—")) return "#94a3b8";
        if (g.startsWith("A")) return "#16a34a";
        if (g.startsWith("B")) return "#2563eb";
        return "#d97706";
    }
}
