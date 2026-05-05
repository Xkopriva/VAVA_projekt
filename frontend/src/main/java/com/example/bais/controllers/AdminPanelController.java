package com.example.bais.controllers;
import com.example.bais.*;
import com.example.bais.models.*;
import com.example.bais.services.*;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Admin Panel Overview — dashboard with quick-link tiles to admin sections.
 * Fotka 2: prehľad s dlaždicami.
 * SK/EN + dark/light mode aware.
 */
public class AdminPanelController implements Initializable {

    @FXML private VBox adminRoot;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        adminRoot.getChildren().clear();
        adminRoot.setSpacing(24);
        adminRoot.setPadding(new Insets(32, 32, 32, 32));

        boolean en = UserSession.get().isEnglish();
        buildUI(en);
    }

    private void buildUI(boolean en) {
        // ── Title ─────────────────────────────────────────────────────────
        Label title = new Label(en ? "Admin Panel" : "Administrátorský panel");
        title.setStyle("-fx-font-size:28px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");

        Label sub = new Label(en
            ? "Manage subjects, teachers and guarantors from one place."
            : "Spravujte predmety, učiteľov a garantov z jedného miesta.");
        sub.getStyleClass().add("welcome-sub");
        sub.setWrapText(true);

        VBox titleBlock = new VBox(6, title, sub);
        adminRoot.getChildren().add(titleBlock);

        // ── Stats row ─────────────────────────────────────────────────────
        Label statsTitle = new Label(en ? "System overview" : "Prehľad systému");
        statsTitle.setStyle("-fx-font-size:16px;-fx-font-weight:bold;");
        statsTitle.getStyleClass().add("section-title");
        adminRoot.getChildren().add(statsTitle);

        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getChildren().addAll(
            buildStatCard("📚", en ? "Subjects" : "Predmety", "—", "#6366f1"),
            buildStatCard("👤", en ? "Teachers" : "Učitelia",  "—", "#06b6d4"),
            buildStatCard("🔗", en ? "Guarantors" : "Garanti",  "—", "#f59e0b"),
            buildStatCard("🎓", en ? "Students" : "Študenti",   "—", "#10b981")
        );
        adminRoot.getChildren().add(statsRow);

        // Load counts from backend
        loadStats(statsRow, en);

        // ── Quick links title ─────────────────────────────────────────────
        Label qlTitle = new Label(en ? "Quick navigation" : "Rýchla navigácia");
        qlTitle.setStyle("-fx-font-size:16px;-fx-font-weight:bold;");
        qlTitle.getStyleClass().add("section-title");
        adminRoot.getChildren().add(qlTitle);

        // ── Quick link tiles ──────────────────────────────────────────────
        HBox tilesRow = new HBox(16);
        tilesRow.setAlignment(Pos.CENTER_LEFT);

        tilesRow.getChildren().addAll(
            buildNavTile("📚",
                en ? "Subjects" : "Predmety",
                en ? "View and manage all subjects" : "Zobraziť a spravovať všetky predmety",
                "#6366f1",
                () -> navigateTo("admin-subjects-view.fxml")),
            buildNavTile("🔗",
                en ? "Guarantors" : "Garanti",
                en ? "Assign guarantors to subjects" : "Priradiť garantov k predmetom",
                "#f59e0b",
                () -> navigateTo("admin-guarantors-view.fxml")),
            buildNavTile("👤",
                en ? "Create Teacher" : "Vytvoriť učiteľa",
                en ? "Add a new teacher account" : "Pridať nové konto učiteľa",
                "#06b6d4",
                () -> navigateTo("admin-create-teacher-view.fxml")),
            buildNavTile("📅",
                en ? "Calendar" : "Kalendár",
                en ? "View school calendar" : "Zobraziť školský kalendár",
                "#10b981",
                () -> navigateTo("school-calendar-view.fxml"))
        );
        adminRoot.getChildren().add(tilesRow);
    }

    private VBox buildStatCard(String icon, String label, String value, String color) {
        VBox card = new VBox(8);
        card.getStyleClass().add("section-card");
        card.setMinWidth(160);
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:22px;");
        Label nameLbl = new Label(label);
        nameLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#94a3b8;");
        header.getChildren().addAll(iconLbl, nameLbl);

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        valLbl.setId("stat_" + label.toLowerCase().replace(" ", "_"));

        card.getChildren().addAll(header, valLbl);
        return card;
    }

    private VBox buildNavTile(String icon, String title, String desc, String color, Runnable onClick) {
        VBox tile = new VBox(12);
        tile.getStyleClass().add("dashboard-tile");
        tile.setAlignment(Pos.TOP_LEFT);
        tile.setMinWidth(180);
        tile.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tile, Priority.ALWAYS);
        tile.setCursor(javafx.scene.Cursor.HAND);
        tile.setPadding(new Insets(20));
        tile.setStyle("-fx-background-radius:14;-fx-border-radius:14;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:32px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");

        Label descLbl = new Label(desc);
        descLbl.getStyleClass().add("welcome-sub");
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size:12px;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label arrowLbl = new Label("→");
        arrowLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");

        tile.getChildren().addAll(iconLbl, titleLbl, descLbl, spacer, arrowLbl);
        tile.setOnMouseClicked(e -> onClick.run());

        // Hover effect
        tile.setOnMouseEntered(e -> tile.setStyle(
            "-fx-background-radius:14;-fx-border-radius:14;-fx-effect:dropshadow(gaussian," + color + ",12,0.3,0,2);"));
        tile.setOnMouseExited(e -> tile.setStyle(
            "-fx-background-radius:14;-fx-border-radius:14;"));

        return tile;
    }

    private void loadStats(HBox statsRow, boolean en) {
        WebSocketClientService ws = WebSocketClientService.getInstance();
        String[] subU = new String[1];
        String[] subS = new String[1];

        subU[0] = ws.subscribe("USER_LIST", node -> {
            ws.unsubscribe(subU[0]);
            JsonNode data = node.path("data");
            long teachers = 0, students = 0, guarantors = 0;
            if (data.isArray()) {
                for (JsonNode u : data) {
                    boolean isTeacher = false, isStudent = false;
                    for (JsonNode r : u.path("roles")) {
                        String role = r.asText();
                        if (role.equals("TEACHER")) isTeacher = true;
                        if (role.equals("STUDENT")) isStudent = true;
                    }
                    if (isTeacher) teachers++;
                    if (isStudent) students++;
                }
            }
            final long t = teachers, s = students;
            Platform.runLater(() -> {
                updateStat(statsRow, en ? "teachers" : "učitelia", String.valueOf(t));
                updateStat(statsRow, en ? "students" : "študenti", String.valueOf(s));
            });
        });

        subS[0] = ws.subscribe("TEACHER_SUBJECTS_LIST", node -> {
            ws.unsubscribe(subS[0]);
            JsonNode data = node.path("data");
            long total = data.isArray() ? data.size() : 0;
            long withGuar = 0;
            if (data.isArray()) {
                for (JsonNode s : data) {
                    if (!s.path("guarantorId").isNull()) withGuar++;
                }
            }
            final long tot = total, wg = withGuar;
            Platform.runLater(() -> {
                updateStat(statsRow, en ? "subjects" : "predmety", String.valueOf(tot));
                updateStat(statsRow, en ? "guarantors" : "garanti", String.valueOf(wg));
            });
        });

        ws.sendAction("LIST_USERS", null);
        ws.sendAction("GET_MY_SUBJECTS", null);
    }

    private void updateStat(HBox statsRow, String key, String value) {
        statsRow.getChildren().forEach(node -> {
            if (node instanceof VBox card) {
                card.getChildren().stream()
                    .filter(c -> c instanceof Label lbl && lbl.getId() != null
                            && lbl.getId().equals("stat_" + key))
                    .forEach(c -> ((Label) c).setText(value));
            }
        });
    }

    private void navigateTo(String fxml) {
        // Find the scroll pane in the scene and load the view
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/example/bais/" + fxml));
            javafx.scene.Node view = loader.load();
            // Walk up to find the ScrollPane
            javafx.scene.Node n = adminRoot;
            while (n != null && !(n instanceof ScrollPane)) {
                n = n.getParent();
            }
            if (n instanceof ScrollPane sp) {
                sp.setContent(view);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
