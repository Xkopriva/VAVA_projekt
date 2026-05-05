package com.example.bais.controllers;
import com.example.bais.*;
import com.example.bais.models.*;
import com.example.bais.services.*;
import com.example.bais.components.*;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CoursesController implements Initializable {

    @FXML private VBox coursesRoot;

    record CourseItem(int id, String code, String name, int credits, String status, String accent) {}

    // Farby pre accent pruhy — cyklicky pre rôzne predmety
    private static final String[] ACCENTS = {
        "#93c5fd", "#86efac", "#fca5a5", "#fde68a", "#c4b5fd",
        "#fdba74", "#6ee7b7", "#f9a8d4", "#a5f3fc", "#d9f99d"
    };

    private final List<CourseItem> courses = new ArrayList<>();
    private String subEnrollments;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showLoading();

        WebSocketClientService ws = WebSocketClientService.getInstance();
        subEnrollments = ws.subscribe("MY_ENROLLMENTS", this::handleEnrollments);
        ws.sendAction("GET_MY_ENROLLMENTS", null);

        // Timeout 5s — ak odpoveď nepríde, zobraz prázdny stav
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            ws.unsubscribe(subEnrollments);
            if (courses.isEmpty()) {
                Platform.runLater(this::buildUI);
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void handleEnrollments(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subEnrollments);
        JsonNode data = node.path("data");
        courses.clear();

        if (data.isArray()) {
            int idx = 0;
            for (JsonNode e : data) {
                int    subjectId = e.path("subjectId").asInt(0);
                String code    = e.path("subjectCode").asText("");
                String name    = e.path("subjectName").asText("");
                int    credits = e.path("credits").asInt(0);
                String status  = e.path("status").asText("ACTIVE");

                if (code.isEmpty()) code = "Sub " + subjectId;
                if (name.isEmpty()) name = code;

                // Zobrazujeme len kurzy v aktuálnom semestri (ACTIVE)
                if (!"ACTIVE".equals(status)) continue;

                String accent = ACCENTS[idx % ACCENTS.length];
                courses.add(new CourseItem(subjectId, code, name, credits, status, accent));
                idx++;
            }
        }

        Platform.runLater(this::buildUI);
    }


    private void showLoading() {
        coursesRoot.getChildren().clear();
        coursesRoot.setPadding(new Insets(24, 28, 24, 28));
        Label l = new Label("⏳  Načítavam kurzy...");
        l.setStyle("-fx-font-size:16px;-fx-text-fill:#64748b;");
        coursesRoot.getChildren().add(l);
    }

    private void buildUI() {
        boolean en = UserSession.get().isEnglish();

        // Všetky sú ACTIVE (filtrujeme v handleEnrollments)
        int totalCredits = courses.stream().mapToInt(CourseItem::credits).sum();

        coursesRoot.getChildren().clear();
        coursesRoot.setSpacing(16);
        coursesRoot.setPadding(new Insets(24, 28, 24, 28));

        // Title block
        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(6);
        Label title = new Label(en ? "Current Courses" : "Aktuálne kurzy");
        title.setStyle("-fx-font-size:32px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(
            courses.size() + " " + (en ? "subjects" : "predmetov") + " • " +
            totalCredits   + " " + (en ? "credits"  : "kreditov") + " • " +
            (en ? "Current semester" : "Aktuálny semester"));
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);
        titleRow.getChildren().add(titleBlock);

        // Summary stats
        HBox stats = new HBox(12);
        stats.getChildren().addAll(
            statCard(String.valueOf(courses.size()), en ? "Subjects"    : "Predmetov"),
            statCard(String.valueOf(totalCredits),   en ? "Credits"     : "Kreditov"),
            statCard(String.valueOf(courses.size()), en ? "In Progress" : "Prebieha"),
            statCard("0",                            en ? "Completed"   : "Ukončené")
        );


        // Course cards
        VBox cardList = new VBox(12);
        if (courses.isEmpty()) {
            Label empty = new Label(en
                ? "No courses found. Please contact your administrator."
                : "Žiadne predmety neboli nájdené. Kontaktujte administrátora.");
            empty.getStyleClass().add("schedule-loc");
            empty.setPadding(new Insets(12, 0, 0, 0));
            cardList.getChildren().add(empty);
        } else {
            for (CourseItem c : courses) {
                cardList.getChildren().add(buildCourseCard(c, en));
            }
        }

        coursesRoot.getChildren().addAll(titleRow, stats, cardList);
    }

    private VBox buildCourseCard(CourseItem c, boolean en) {
        VBox wrapper = new VBox(0);
        wrapper.getStyleClass().add("section-card");
        wrapper.setStyle("-fx-background-radius:12;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0));
        header.setStyle("-fx-cursor: hand;");

        // Color accent bar
        Region bar = new Region();
        bar.setMinWidth(6);
        bar.setPrefWidth(6);
        bar.setStyle("-fx-background-color:" + c.accent() + ";-fx-background-radius:12 0 0 12;");

        // Content
        HBox content = new HBox(16);
        content.setPadding(new Insets(16, 18, 16, 18));
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);

        // Code badge
        VBox codeBadge = new VBox();
        codeBadge.setAlignment(Pos.CENTER);
        codeBadge.setMinWidth(60);
        codeBadge.setMinHeight(60);
        codeBadge.setStyle("-fx-background-color:" + c.accent() + ";-fx-background-radius:12;");
        Label codeLbl = new Label(c.code());
        codeLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        codeBadge.getChildren().add(codeLbl);

        // Info
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(c.name());
        name.setStyle("-fx-font-size:15px;-fx-font-weight:bold;");
        name.getStyleClass().add("schedule-name");

        // Status badge
        boolean active = "ACTIVE".equals(c.status());
        boolean passed = "PASSED".equals(c.status());
        String statusText = passed ? (en ? "Completed" : "Ukončené")
                          : active ? (en ? "In Progress" : "Prebieha")
                          : c.status();
        Label statusLbl = new Label(statusText);
        statusLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8 3 8;-fx-background-radius:6;" +
            (passed ? "-fx-background-color:#dcfce7;-fx-text-fill:#15803d;"
                    : "-fx-background-color:#fff7ed;-fx-text-fill:#d97706;"));

        info.getChildren().addAll(name, statusLbl);

        // Right: credits badge
        VBox credBox = new VBox(4);
        credBox.setAlignment(Pos.CENTER);
        credBox.setMinWidth(70);
        Label credNum = new Label(c.credits() > 0 ? String.valueOf(c.credits()) : "—");
        credNum.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");
        Label credLbl = new Label(en ? "credits" : "kreditov");
        credLbl.getStyleClass().add("perf-course");
        credBox.getChildren().addAll(credNum, credLbl);

        content.getChildren().addAll(codeBadge, info, credBox);
        header.getChildren().addAll(bar, content);

        VBox detailsContainer = new VBox(10);
        detailsContainer.setPadding(new Insets(16, 24, 16, 24));
        detailsContainer.setStyle("-fx-background-color: transparent; -fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");
        detailsContainer.setVisible(false);
        detailsContainer.setManaged(false);

        wrapper.getChildren().addAll(header, detailsContainer);

        header.setOnMouseClicked(e -> {
            boolean isExpanded = detailsContainer.isVisible();
            detailsContainer.setVisible(!isExpanded);
            detailsContainer.setManaged(!isExpanded);

            if (!isExpanded && detailsContainer.getChildren().isEmpty()) {
                loadSubjectDetails(c.id(), detailsContainer, en);
            }
        });

        return wrapper;
    }

    private void loadSubjectDetails(int subjectId, VBox container, boolean en) {
        Label loading = new Label(en ? "⏳ Loading details..." : "⏳ Načítavam detaily...");
        loading.setStyle("-fx-text-fill: #64748b;");
        container.getChildren().add(loading);

        WebSocketClientService ws = WebSocketClientService.getInstance();
        String[] sub = new String[1];
        sub[0] = ws.subscribe("SUBJECT_DETAIL", node -> {
            ws.unsubscribe(sub[0]);
            JsonNode data = node.path("data");
            Platform.runLater(() -> {
                container.getChildren().clear();
                if (data.isMissingNode() || data.isNull()) {
                    Label err = new Label(en ? "Failed to load details." : "Nepodarilo sa načítať detaily.");
                    err.setStyle("-fx-text-fill: #dc2626;");
                    container.getChildren().add(err);
                    return;
                }
                
                String syllabus = data.path("syllabus").asText("");
                String breakdown = data.path("assessmentBreakdown").asText("");
                double rating = data.path("avgStudentRating").asDouble(0);
                double diff = data.path("subjectDifficulty").asDouble(0);
                
                if (!syllabus.isBlank()) {
                    Label sylLbl = new Label(en ? "Syllabus:" : "Sylabus:");
                    sylLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
                    Label sylText = new Label(syllabus);
                    sylText.setWrapText(true);
                    sylText.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
                    container.getChildren().addAll(sylLbl, sylText);
                }
                
                if (!breakdown.isBlank()) {
                    Label brkLbl = new Label(en ? "Assessment:" : "Hodnotenie:");
                    brkLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
                    Label brkText = new Label(breakdown);
                    brkText.setWrapText(true);
                    brkText.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
                    if (!container.getChildren().isEmpty()) {
                        Region space = new Region(); space.setMinHeight(8); container.getChildren().add(space);
                    }
                    container.getChildren().addAll(brkLbl, brkText);
                }
                
                HBox stats = new HBox(20);
                stats.setPadding(new Insets(10, 0, 0, 0));
                
                if (rating > 0) {
                    Label r = new Label("⭐ Rating: " + rating + " / 5.0");
                    r.setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");
                    stats.getChildren().add(r);
                }
                if (diff > 0) {
                    Label d = new Label("🔥 " + (en ? "Difficulty:" : "Obtiažnosť:") + " " + diff + " / 5.0");
                    d.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    stats.getChildren().add(d);
                }
                
                if (!stats.getChildren().isEmpty()) {
                    container.getChildren().add(stats);
                }
                
                // Grades
                HBox grades = new HBox(12);
                grades.setPadding(new Insets(10, 0, 0, 0));
                String[] letters = {"A", "B", "C", "D", "E", "FX"};
                String[] pctKeys = {"gradeAPct", "gradeBPct", "gradeCPct", "gradeDPct", "gradeEPct", "gradeFxPct"};
                String[] colors = {"#16a34a", "#2563eb", "#0284c7", "#ca8a04", "#ea580c", "#dc2626"};
                
                for (int i=0; i<letters.length; i++) {
                    double pct = data.path(pctKeys[i]).asDouble(0);
                    if (pct > 0) {
                        VBox gBox = new VBox(2);
                        gBox.setAlignment(Pos.CENTER);
                        Label l = new Label(letters[i]);
                        l.setStyle("-fx-font-weight: bold; -fx-text-fill: " + colors[i] + ";");
                        Label p = new Label(String.format("%.1f%%", pct));
                        p.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                        gBox.getChildren().addAll(l, p);
                        grades.getChildren().add(gBox);
                    }
                }
                if (!grades.getChildren().isEmpty()) {
                    container.getChildren().add(grades);
                }
                
                if (container.getChildren().isEmpty()) {
                    Label noData = new Label(en ? "No details available." : "Bližšie informácie nie sú dostupné.");
                    noData.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
                    container.getChildren().add(noData);
                }
            });
        });
        ws.sendAction("GET_SUBJECT_DETAIL", java.util.Map.of("subjectId", subjectId));
    }

    private VBox statCard(String value, String label) {
        VBox card = new VBox(4);
        card.getStyleClass().add("section-card");
        card.setAlignment(Pos.CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label val = new Label(value);
        val.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("perf-course");
        card.getChildren().addAll(val, lbl);
        return card;
    }
}
