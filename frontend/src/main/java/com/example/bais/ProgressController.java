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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProgressController implements Initializable {

    @FXML private VBox progressRoot;

    record SemCourse(String name, String grade, int credits) {}
    record Semester(String name, String term, int totalCredits, List<SemCourse> courses) {}

    private final List<Semester>           semesters      = new ArrayList<>();
    private final Map<Integer, String>     finalMarks     = new HashMap<>();
    private final List<JsonNode>           rawEnrollments = new ArrayList<>();

    private final AtomicBoolean enrollmentsLoaded = new AtomicBoolean(false);
    private final AtomicBoolean marksLoaded       = new AtomicBoolean(false);

    private String subEnrollments;
    private String subMarks;
    private String subProfile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showLoading();

        WebSocketClientService ws = WebSocketClientService.getInstance();

        // Subscribe na všetky typy ktoré nás zaujímajú
        subEnrollments = ws.subscribe("MY_ENROLLMENTS",   this::handleEnrollments);
        subMarks       = ws.subscribe("MY_INDEX_RECORDS", this::handleMarks);
        subProfile     = ws.subscribe("USER_PROFILE",     this::handleProfile);

        ws.sendAction("GET_MY_ENROLLMENTS", null);
        ws.sendAction("GET_MY_MARKS",       null);
        ws.sendAction("GET_USER_PROFILE",   null);

        // Timeout 5s — ak niektorá odpoveď nepríde, pokračujeme s tým čo máme
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            ws.unsubscribe(subProfile);
            // Ak enrollmenty alebo marks neprisli, zobraz čo máme
            if (!enrollmentsLoaded.get() || !marksLoaded.get()) {
                enrollmentsLoaded.set(true);
                marksLoaded.set(true);
                tryBuild();
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void handleProfile(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subProfile);
        JsonNode data    = node.path("data");
        String firstName = data.path("firstName").asText("");
        String lastName  = data.path("lastName").asText("");
        UserSession.get().setFirstName(firstName);
        UserSession.get().setLastName(lastName);
        // Ak sú dáta hotové, prekreslíme UI s menom
        if (enrollmentsLoaded.get() && marksLoaded.get()) {
            Platform.runLater(this::buildUI);
        }
    }

    private void handleEnrollments(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subEnrollments);
        JsonNode data = node.path("data");
        rawEnrollments.clear();
        if (data.isArray()) {
            for (JsonNode e : data) rawEnrollments.add(e);
        }
        enrollmentsLoaded.set(true);
        tryBuild();
    }

    private void handleMarks(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subMarks);
        JsonNode data = node.path("data");
        finalMarks.clear();
        if (data.isArray()) {
            for (JsonNode r : data) {
                finalMarks.put(r.path("enrollmentId").asInt(), r.path("finalMark").asText("—"));
            }
        }
        marksLoaded.set(true);
        tryBuild();
    }

    private synchronized void tryBuild() {
        if (!enrollmentsLoaded.get() || !marksLoaded.get()) return;

        // Zoraď enrollmenty do semestrov
        Map<Integer, List<JsonNode>> bySemester = new HashMap<>();
        for (JsonNode e : rawEnrollments) {
            int semId = e.path("semesterId").asInt();
            bySemester.computeIfAbsent(semId, k -> new ArrayList<>()).add(e);
        }

        semesters.clear();
        List<Integer> semIds = new ArrayList<>(bySemester.keySet());
        semIds.sort(Integer::compareTo);

        for (Integer semId : semIds) {
            List<SemCourse> courses = new ArrayList<>();
            int semCredits = 0;

            for (JsonNode e : bySemester.get(semId)) {
                int enrollmentId = e.path("id").asInt();
                int subjectId    = e.path("subjectId").asInt();
                String status    = e.path("status").asText();
                int credits      = e.path("credits").asInt(0);

                String code = e.path("subjectCode").asText("");
                String name = e.path("subjectName").asText("");
                if (code.isEmpty()) code = "Sub " + subjectId;
                if (name.isEmpty()) name = code;

                String grade = finalMarks.getOrDefault(enrollmentId, "—");
                if ("PASSED".equals(status)) semCredits += credits;
                courses.add(new SemCourse(name, grade, credits));
            }
            semesters.add(new Semester("Semester " + semId, "SEM", semCredits, courses));
        }

        Platform.runLater(this::buildUI);
    }

    // ── UI ────────────────────────────────────────────────────────

    private void showLoading() {
        Platform.runLater(() -> {
            progressRoot.getChildren().clear();
            progressRoot.setPadding(new Insets(24, 28, 24, 28));
            Label l = new Label("⏳  Načítavam progres...");
            l.setStyle("-fx-font-size:16px;-fx-text-fill:#64748b;");
            progressRoot.getChildren().add(l);
        });
    }

    private void buildUI() {
        progressRoot.getChildren().clear();
        progressRoot.setSpacing(16);
        progressRoot.setPadding(new Insets(24, 28, 24, 28));

        // Vypočítaj kredity
        int earnedCredits = 0;
        for (Semester sem : semesters) earnedCredits += sem.totalCredits();
        final int TOTAL = 180;
        double pct    = Math.min(1.0, (double) earnedCredits / TOTAL);
        String pctStr = String.format("%.0f%%", pct * 100);

        String name  = UserSession.get().getFullName();
        String email = UserSession.get().getUserEmail();

        // ── Štúdijný plán ─────────────────────────────────────────
        VBox studyPlanBox = new VBox(12);
        studyPlanBox.getStyleClass().add("section-card");

        Label spTitle = new Label("Študijný plán");
        spTitle.setStyle("-fx-font-size:20px;-fx-font-weight:bold;");
        HBox.setHgrow(spTitle, Priority.ALWAYS);

        Button planBtn = new Button("📋 Plánovať štúdium");
        planBtn.setStyle("-fx-background-color:#06b6d4;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-padding:8 16;-fx-font-size:12px;-fx-background-radius:8;-fx-cursor:hand;");

        HBox spHeader = new HBox(spTitle, planBtn);
        spHeader.setAlignment(Pos.CENTER_LEFT);

        Label creditsLbl = new Label(String.format(
                "Kredity: %d získaných z %d povinných (CHÝBA %d kr.)",
                earnedCredits, TOTAL, Math.max(0, TOTAL - earnedCredits)));
        creditsLbl.setStyle("-fx-font-size:13px;");

        studyPlanBox.getChildren().addAll(spHeader, creditsLbl);

        // ── Nadpis + štatistiky ───────────────────────────────────
        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(4);
        Label title = new Label("Progres štúdium");
        title.setStyle("-fx-font-size:32px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(name + (email != null && !name.equals(email) ? " • " + email : ""));
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        HBox statsRow = new HBox(12);
        statsRow.setAlignment(Pos.CENTER_RIGHT);
        statsRow.getChildren().addAll(
                miniStat(earnedCredits + " / " + TOTAL, "Kreditov"),
                miniStat(pctStr, "Hotovo")
        );
        titleRow.getChildren().addAll(titleBlock, statsRow);

        // ── Progress bar ──────────────────────────────────────────
        VBox progressSection = new VBox(8);
        progressSection.getStyleClass().add("section-card");

        HBox barHeader = new HBox();
        barHeader.setAlignment(Pos.CENTER_LEFT);
        Label barLabel = new Label("Celkový progres štúdia");
        barLabel.getStyleClass().add("section-title");
        HBox.setHgrow(barLabel, Priority.ALWAYS);
        Label barPct = new Label(pctStr + "  (" + earnedCredits + " / " + TOTAL + " kreditov)");
        barPct.getStyleClass().add("schedule-date");
        barHeader.getChildren().addAll(barLabel, barPct);

        StackPane barBg = new StackPane();
        barBg.setPrefHeight(10);
        barBg.setStyle("-fx-background-color:#e2e8f0;-fx-background-radius:5;");
        StackPane barFill = new StackPane();
        barFill.setStyle("-fx-background-color:#06b6d4;-fx-background-radius:5;");
        barFill.setPrefHeight(10);
        barFill.prefWidthProperty().bind(barBg.widthProperty().multiply(pct));
        barBg.getChildren().add(barFill);
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
        progressSection.getChildren().addAll(barHeader, barBg);

        // ── Karty semestrov ───────────────────────────────────────
        VBox semCards = new VBox(14);
        for (Semester sem : semesters) semCards.getChildren().add(buildSemCard(sem));

        if (semesters.isEmpty()) {
            Label noData = new Label("Žiadne záznamy o štúdiu neboli nájdené v databáze.");
            noData.getStyleClass().add("schedule-loc");
            noData.setPadding(new Insets(8, 0, 0, 0));
            semCards.getChildren().add(noData);
        }

        progressRoot.getChildren().addAll(studyPlanBox, titleRow, progressSection, semCards);
    }

    private VBox buildSemCard(Semester sem) {
        VBox card = new VBox(12);
        card.getStyleClass().add("section-card");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(sem.term());
        badge.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:3 8;" +
                "-fx-background-radius:6;-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;");
        Label name = new Label("  " + sem.name());
        name.getStyleClass().add("section-title");
        HBox.setHgrow(name, Priority.ALWAYS);
        Label cr = new Label(sem.totalCredits() + " kreditov");
        cr.getStyleClass().add("schedule-date");
        cr.setPadding(new Insets(0, 12, 0, 0));
        header.getChildren().addAll(badge, name, cr);
        card.getChildren().add(header);

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#e2e8f0;");
        card.getChildren().add(sep);

        card.getChildren().add(courseRow("Predmet", "Kredity", "Zn.", true));

        for (int i = 0; i < sem.courses().size(); i++) {
            SemCourse c = sem.courses().get(i);
            boolean last = i == sem.courses().size() - 1;
            HBox row = courseRow(c.name(), c.credits() > 0 ? String.valueOf(c.credits()) : "—", c.grade(), false);
            if (!last) row.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;");
            card.getChildren().add(row);
        }
        return card;
    }

    private HBox courseRow(String name, String credits, String grade, boolean header) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(header ? 4 : 8, 0, header ? 4 : 8, 0));
        Label nl = new Label(name);
        if (header) nl.getStyleClass().add("schedule-date"); else nl.getStyleClass().add("schedule-name");
        HBox.setHgrow(nl, Priority.ALWAYS);
        Label cl = new Label(credits);
        if (header) cl.getStyleClass().add("schedule-date"); else cl.getStyleClass().add("schedule-loc");
        cl.setMinWidth(70);
        Label gl = new Label(grade);
        gl.setMinWidth(50);
        if (header) gl.getStyleClass().add("schedule-date");
        else gl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + gradeColor(grade) + ";");
        row.getChildren().addAll(nl, cl, gl);
        return row;
    }

    private VBox miniStat(String value, String label) {
        VBox card = new VBox(2);
        card.getStyleClass().add("section-card");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(110);
        Label val = new Label(value);
        val.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("perf-course");
        card.getChildren().addAll(val, lbl);
        return card;
    }

    private String gradeColor(String g) {
        if (g == null || g.equals("—"))          return "#94a3b8";
        if (g.startsWith("A") || g.equals("PASS")) return "#16a34a";
        if (g.startsWith("B"))                  return "#2563eb";
        if (g.equals("FX") || g.equals("FAIL")) return "#dc2626";
        return "#d97706";
    }
}
