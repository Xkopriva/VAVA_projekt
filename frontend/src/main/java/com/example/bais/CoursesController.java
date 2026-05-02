package com.example.bais;

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

    record CourseItem(String code, String name, int credits, String status, String accent) {}

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
                String code   = e.path("subjectCode").asText("");
                String name   = e.path("subjectName").asText("");
                int    credits = e.path("credits").asInt(0);
                String status = e.path("status").asText("ACTIVE");

                if (code.isEmpty()) code = "Sub " + e.path("subjectId").asInt();
                if (name.isEmpty()) name = code;

                String accent = ACCENTS[idx % ACCENTS.length];
                courses.add(new CourseItem(code, name, credits, status, accent));
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

        // Count only active / passed enrollments
        long activeCount  = courses.stream().filter(c -> "ACTIVE".equals(c.status())).count();
        long passedCount  = courses.stream().filter(c -> "PASSED".equals(c.status())).count();
        int  totalCredits = courses.stream().mapToInt(CourseItem::credits).sum();

        coursesRoot.getChildren().clear();
        coursesRoot.setSpacing(16);
        coursesRoot.setPadding(new Insets(24, 28, 24, 28));

        // Title block
        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(4);
        Label title = new Label(en ? "Enrolled Courses" : "Zapísané kurzy");
        title.setStyle("-fx-font-size:32px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(
            courses.size() + " " + (en ? "subjects" : "predmetov") + " • " +
            totalCredits   + " " + (en ? "credits"  : "kreditov"));
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);
        titleRow.getChildren().add(titleBlock);

        // Summary stats
        HBox stats = new HBox(12);
        stats.getChildren().addAll(
            statCard(String.valueOf(courses.size()), en ? "Subjects"   : "Predmetov"),
            statCard(String.valueOf(totalCredits),   en ? "Credits"    : "Kreditov"),
            statCard(String.valueOf(activeCount),    en ? "In Progress": "Prebieha"),
            statCard(String.valueOf(passedCount),    en ? "Completed"  : "Ukončené")
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

    private HBox buildCourseCard(CourseItem c, boolean en) {
        HBox card = new HBox(0);
        card.getStyleClass().add("section-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(0));
        card.setStyle("-fx-background-radius:12;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

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
        card.getChildren().addAll(bar, content);
        return card;
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
