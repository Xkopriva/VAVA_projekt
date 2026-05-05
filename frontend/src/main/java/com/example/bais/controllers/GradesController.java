package com.example.bais.controllers;
import com.example.bais.*;
import com.example.bais.models.*;
import com.example.bais.services.*;
import com.example.bais.components.*;

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
import java.util.HashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;

public class GradesController implements Initializable {

    @FXML private VBox gradesRoot;

    record Course(int enrollmentId, String code, String name, int credits, String finalGrade, String statusText, boolean done) {}

    private final List<Course>              courses                = new ArrayList<>();
    private final Map<Integer, String>      finalMarksByEnrollment = new HashMap<>();
    private final Map<Integer, SubjectInfo> subjectInfoMap         = new HashMap<>();
    private final List<EnrollmentInfo>      enrollmentInfos        = new ArrayList<>();

    private final AtomicBoolean enrollmentsLoaded = new AtomicBoolean(false);
    private final AtomicBoolean marksLoaded       = new AtomicBoolean(false);

    private String subEnrollments;
    private String subMarks;
    private String subPoints;

    private int expectedPointsResponses = 0;
    private int receivedPointsResponses = 0;

    record SubjectInfo(String code, String name, int credits) {}
    record EnrollmentInfo(int enrollmentId, int subjectId, String status) {}
    record MarkInfo(String title, double points, double maxPoints) {}
    private final Map<Integer, List<MarkInfo>> pointsByEnrollment = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showLoading();

        WebSocketClientService ws = WebSocketClientService.getInstance();
        subEnrollments = ws.subscribe("MY_ENROLLMENTS",   this::handleEnrollments);
        subMarks       = ws.subscribe("MY_INDEX_RECORDS", this::handleMarks);
        subPoints      = ws.subscribe("MY_POINTS_LIST",   this::handlePoints);

        ws.sendAction("GET_MY_ENROLLMENTS", null);
        ws.sendAction("GET_MY_MARKS",       null);

        // Timeout 5s — ak odpovede neprídu, zobraz prázdny stav
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            ws.unsubscribe(subEnrollments);
            ws.unsubscribe(subMarks);
            ws.unsubscribe(subPoints);
            if (!enrollmentsLoaded.get() || !marksLoaded.get()) {
                enrollmentsLoaded.set(true);
                marksLoaded.set(true);
                tryBuild();
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void handleEnrollments(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subEnrollments);
        JsonNode data = node.path("data");
        enrollmentInfos.clear();
        subjectInfoMap.clear();

        if (data.isArray()) {
            for (JsonNode e : data) {
                int    enrollmentId = e.path("id").asInt();
                int    subjectId    = e.path("subjectId").asInt();
                String status       = e.path("status").asText();
                String code         = e.path("subjectCode").asText("");
                String name         = e.path("subjectName").asText("");
                int    credits      = e.path("credits").asInt(0);

                if (code.isEmpty()) code = "Sub " + subjectId;
                if (name.isEmpty()) name = code;

                subjectInfoMap.put(subjectId, new SubjectInfo(code, name, credits));
                enrollmentInfos.add(new EnrollmentInfo(enrollmentId, subjectId, status));
            }
        }
        
        expectedPointsResponses = enrollmentInfos.size();
        if (expectedPointsResponses == 0) {
            enrollmentsLoaded.set(true);
            tryBuild();
        } else {
            WebSocketClientService ws = WebSocketClientService.getInstance();
            for (EnrollmentInfo ei : enrollmentInfos) {
                ws.sendAction("GET_MY_POINTS", java.util.Map.of("enrollmentId", ei.enrollmentId()));
            }
        }
    }

    private void handlePoints(JsonNode node) {
        JsonNode data = node.path("data");
        if (data.isArray()) {
            for (JsonNode m : data) {
                int eid = m.path("enrollmentId").asInt(-1);
                if (eid != -1) {
                    pointsByEnrollment.computeIfAbsent(eid, k -> new ArrayList<>())
                        .add(new MarkInfo(
                            m.path("title").asText("Neznáme hodnotenie"),
                            m.path("points").asDouble(0),
                            m.path("maxPoints").asDouble(0)
                        ));
                }
            }
        }
        receivedPointsResponses++;
        if (receivedPointsResponses >= expectedPointsResponses) {
            WebSocketClientService.getInstance().unsubscribe(subPoints);
            enrollmentsLoaded.set(true);
            tryBuild();
        }
    }

    private void handleMarks(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subMarks);
        JsonNode data = node.path("data");
        finalMarksByEnrollment.clear();
        if (data.isArray()) {
            for (JsonNode r : data) {
                finalMarksByEnrollment.put(
                    r.path("enrollmentId").asInt(),
                    r.path("finalMark").asText("—"));
            }
        }
        marksLoaded.set(true);
        tryBuild();
    }

    private synchronized void tryBuild() {
        if (!enrollmentsLoaded.get() || !marksLoaded.get()) return;

        courses.clear();
        int totalCredits = 0;
        int passedCount  = 0;

        for (EnrollmentInfo ei : enrollmentInfos) {
            SubjectInfo si = subjectInfoMap.getOrDefault(ei.subjectId(),
                    new SubjectInfo("Sub " + ei.subjectId(), "Predmet " + ei.subjectId(), 0));

            String  finalGrade  = finalMarksByEnrollment.getOrDefault(ei.enrollmentId(), "—");
            boolean passed      = "PASSED".equals(ei.status());
            boolean active      = "ACTIVE".equals(ei.status());
            
            // len práve prebiehajúce (aktívne) predmety
            if (!active) continue;

            boolean en = UserSession.get().isEnglish();
            String  statusText  = passed ? (en ? "Passed" : "Absolvované") : (active ? (en ? "In Progress" : "Prebieha") : ei.status());

            courses.add(new Course(ei.enrollmentId(), si.code(), si.name(), si.credits(), finalGrade, statusText, passed));
            totalCredits += si.credits();
            if (passed) passedCount++;
        }

        final int tc = totalCredits, pc = passedCount, tot = courses.size();
        Platform.runLater(() -> buildUI(tc, pc, tot));
    }

    //UI

    private void showLoading() {
        boolean en = UserSession.get().isEnglish();
        gradesRoot.getChildren().clear();
        gradesRoot.setPadding(new Insets(24, 28, 24, 28));
        Label l = new Label(en ? "⏳  Loading grades..." : "⏳  Načítavam hodnotenia...");
        l.setStyle("-fx-font-size:16px;-fx-text-fill:#64748b;");
        gradesRoot.getChildren().add(l);
    }

    private void buildUI(int totalCredits, int passedCount, int totalCount) {
        gradesRoot.getChildren().clear();
        gradesRoot.setSpacing(16);
        gradesRoot.setPadding(new Insets(24, 28, 24, 28));

        boolean en = UserSession.get().isEnglish();

        // Nadpis
        VBox titleBlock = new VBox(4);
        Label title = new Label(en ? "My Grades" : "Moje hodnotenia");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(en ? "Subject results and evaluations" : "Výsledky predmetov a hodnotenie");
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);

        // Štatistiky
        HBox stats = new HBox(12);
        stats.getChildren().addAll(
            statCard("📚", en ? "Subjects" : "Predmety", String.valueOf(totalCount)),
            statCard("📦", en ? "Credits"  : "Kredity",  String.valueOf(totalCredits)),
            statCard("✅", en ? "Finished" : "Ukončené", passedCount + " / " + totalCount)
        );

        // Tabuľka
        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("section-card");
        Label tableTitle = new Label(en ? "Subject Status" : "Stav predmetov");
        tableTitle.getStyleClass().add("section-title");
        tableCard.getChildren().add(tableTitle);

        HBox headerRow = tableRow(true,
            en ? "Code" : "Kód",
            en ? "Subject" : "Predmet",
            en ? "Credits" : "Kredity",
            en ? "Final" : "Záverečná",
            en ? "Status" : "Stav");
        headerRow.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;" +
                "-fx-border-width:0 0 1 0;-fx-padding:0 0 8 0;");
        tableCard.getChildren().add(headerRow);

        if (courses.isEmpty()) {
            Label empty = new Label(en
                    ? "No subjects found in the database."
                    : "Žiadne predmety neboli nájdené v databáze.");
            empty.getStyleClass().add("schedule-loc");
            empty.setPadding(new Insets(12, 0, 0, 0));
            tableCard.getChildren().add(empty);
        }

        for (int i = 0; i < courses.size(); i++) {
            Course  c    = courses.get(i);
            boolean last = i == courses.size() - 1;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 0, 10, 0));
            if (!last) row.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;" +
                    "-fx-border-width:0 0 1 0;");

            Label code = new Label(c.code());
            code.setStyle("-fx-font-weight:bold;-fx-text-fill:#06b6d4;-fx-font-size:13px;");
            code.setMinWidth(80);

            Label name = new Label(c.name());
            name.getStyleClass().add("schedule-name");
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);

            Label cred = new Label(c.credits() > 0 ? String.valueOf(c.credits()) : "—");
            cred.getStyleClass().add("schedule-name");
            cred.setMinWidth(60);

            Label fin = new Label(c.finalGrade());
            fin.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + gradeColor(c.finalGrade()) + ";");
            fin.setMinWidth(90);

            Label status = new Label(c.statusText());
            status.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8 3 8;" +
                    "-fx-background-radius:6;" +
                    (c.done()
                        ? "-fx-background-color:#dcfce7;-fx-text-fill:#15803d;"
                        : "-fx-background-color:#fff7ed;-fx-text-fill:#d97706;"));

            row.getChildren().addAll(code, name, cred, fin, status);
            
            // Sub-box pre body z daného predmetu
            VBox subjectBox = new VBox();
            subjectBox.getChildren().add(row);
            
            List<MarkInfo> points = pointsByEnrollment.getOrDefault(c.enrollmentId, Collections.emptyList());
            if (!points.isEmpty()) {
                VBox pointsBox = new VBox(4);
                pointsBox.setPadding(new Insets(0, 0, 10, 50)); // Odsadenie sprava a zlava pod nazov predmetu
                
                double totalGot = 0;
                double totalMax = 0;

                for (MarkInfo p : points) {
                    HBox pRow = new HBox(10);
                    pRow.setAlignment(Pos.CENTER_LEFT);
                    
                    Label pTitle = new Label("• " + p.title());
                    pTitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
                    pTitle.setPrefWidth(200);

                    Label pPts = new Label(fmt(p.points()) + " / " + fmt(p.maxPoints()) + " b.");
                    pPts.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                    pPts.getStyleClass().add("grade-text");
                    
                    totalGot += p.points();
                    totalMax += p.maxPoints();

                    pRow.getChildren().addAll(pTitle, pPts);
                    pointsBox.getChildren().add(pRow);
                }
                
                // Celkovy sucet bodov za predmet
                HBox sumRow = new HBox(10);
                sumRow.setAlignment(Pos.CENTER_LEFT);
                sumRow.setPadding(new Insets(4, 0, 0, 0));
                Label sumTitle = new Label(en ? "Total:" : "Spolu:");
                sumTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                sumTitle.getStyleClass().add("grade-text");
                sumTitle.setPrefWidth(200);
                Label sumPts = new Label(fmt(totalGot) + " / " + fmt(totalMax) + " b.");
                sumPts.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 12px; -fx-font-weight: bold;");
                sumRow.getChildren().addAll(sumTitle, sumPts);
                pointsBox.getChildren().add(sumRow);
                
                subjectBox.getChildren().add(pointsBox);
            }

            tableCard.getChildren().add(subjectBox);
        }

        // Legenda
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(4, 0, 0, 0));
        legend.getChildren().addAll(
            legendItem("#16a34a", "A / PASS – " + (en ? "Passed"  : "Absolvované")),
            legendItem("#2563eb", "B – "        + (en ? "Good"    : "Veľmi dobrý")),
            legendItem("#d97706", "C/D/E – "    + (en ? "Average" : "Priemerný")),
            legendItem("#dc2626", "FX – "       + (en ? "Failed"  : "Nevyhovel"))
        );

        gradesRoot.getChildren().addAll(titleBlock, stats, tableCard, legend);
    }

    // Helpers

    private VBox statCard(String icon, String label, String value) {
        VBox card = new VBox(4);
        card.getStyleClass().add("section-card");
        card.setAlignment(Pos.CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);
        Label ic  = new Label(icon);   ic.setStyle("-fx-font-size:22px;"); ic.getStyleClass().add("stat-icon");
        Label lbl = new Label(label);  lbl.getStyleClass().add("perf-course");
        Label val = new Label(value);  val.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");
        card.getChildren().addAll(ic, lbl, val);
        return card;
    }

    private HBox tableRow(boolean isHeader, String... cols) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        int[] widths = {80, -1, 60, 90, 90};
        for (int i = 0; i < cols.length; i++) {
            Label lbl = new Label(cols[i]);
            if (isHeader) { lbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;"); lbl.getStyleClass().add("schedule-date"); }
            if (widths[i] > 0) {
                lbl.setMinWidth(widths[i]);
                lbl.setPrefWidth(widths[i]);
            } else {
                lbl.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(lbl, Priority.ALWAYS);
            }
            row.getChildren().add(lbl);
        }
        return row;
    }

    private HBox legendItem(String color, String text) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5, Color.web(color));
        Label lbl = new Label(text); lbl.getStyleClass().add("schedule-loc");
        item.getChildren().addAll(dot, lbl);
        return item;
    }

    private String gradeColor(String g) {
        if (g == null || g.equals("—"))              return "#94a3b8";
        if (g.startsWith("A") || g.equals("PASS"))  return "#16a34a";
        if (g.startsWith("B"))                       return "#2563eb";
        if (g.equals("FX") || g.equals("FAIL"))      return "#dc2626";
        return "#d97706";
    }

    private String fmt(double d) {
        if (d == (long) d) return String.format("%d", (long) d);
        else return String.format("%s", d);
    }
}
