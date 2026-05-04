package com.example.bais;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.layout.*;

import java.net.URL;

public class CourseDetailController implements Initializable {

    @FXML private VBox courseRoot;

    record Task(String name, String points, String maxPoints, String deadline,
                String status, String statusColor) {}
    record EnrollmentData(int enrollmentId, int subjectId, String subjectCode,
                          String subjectName, int credits, String status) {}

    private final List<Task> tasks = new ArrayList<>();
    private final List<EnrollmentData> enrollments = new ArrayList<>();
    private final Map<Integer, String> marksByEnrollment = new HashMap<>();

    private String subEnrollments;
    private String subMarks;
    private final AtomicBoolean enrollLoaded = new AtomicBoolean(false);
    private final AtomicBoolean marksLoaded  = new AtomicBoolean(false);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showLoading();
        loadData();
    }

    private void showLoading() {
        courseRoot.getChildren().clear();
        courseRoot.setPadding(new Insets(24, 28, 24, 28));
        Label l = new Label("⏳  Načítavam zadania...");
        l.setStyle("-fx-font-size:16px;-fx-text-fill:#64748b;");
        courseRoot.getChildren().add(l);
    }

    private void loadData() {
        WebSocketClientService ws = WebSocketClientService.getInstance();

        subEnrollments = ws.subscribe("MY_ENROLLMENTS", this::handleEnrollments);
        subMarks       = ws.subscribe("MY_INDEX_RECORDS", this::handleMarks);

        ws.sendAction("GET_MY_ENROLLMENTS", null);
        ws.sendAction("GET_MY_MARKS", null);

        // Timeout 5s
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            ws.unsubscribe(subEnrollments);
            ws.unsubscribe(subMarks);
            enrollLoaded.set(true);
            marksLoaded.set(true);
            tryBuild();
        }, 5, TimeUnit.SECONDS);
    }

    private void handleEnrollments(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subEnrollments);
        JsonNode data = node.path("data");
        enrollments.clear();
        if (data.isArray()) {
            for (JsonNode e : data) {
                enrollments.add(new EnrollmentData(
                    e.path("id").asInt(),
                    e.path("subjectId").asInt(),
                    e.path("subjectCode").asText(""),
                    e.path("subjectName").asText(""),
                    e.path("credits").asInt(0),
                    e.path("status").asText("ACTIVE")
                ));
            }
        }
        enrollLoaded.set(true);
        tryBuild();
    }

    private void handleMarks(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subMarks);
        JsonNode data = node.path("data");
        marksByEnrollment.clear();
        if (data.isArray()) {
            for (JsonNode r : data) {
                marksByEnrollment.put(r.path("enrollmentId").asInt(),
                                      r.path("finalMark").asText("—"));
            }
        }
        marksLoaded.set(true);
        tryBuild();
    }

    private synchronized void tryBuild() {
        if (!enrollLoaded.get() || !marksLoaded.get()) return;

        // Build task list from active enrollments
        tasks.clear();
        for (EnrollmentData enr : enrollments) {
            if ("ACTIVE".equals(enr.status())) {
                String mark = marksByEnrollment.getOrDefault(enr.enrollmentId(), "—");
                // Active enrollment = ongoing, no final mark yet
                tasks.add(new Task(
                    enr.subjectName().isEmpty() ? enr.subjectCode() : enr.subjectName(),
                    mark.equals("—") ? "—" : mark,
                    "—",
                    "—",
                    UserSession.get().isEnglish() ? "In Progress" : "Prebieha",
                    "#d97706"
                ));
            } else if ("PASSED".equals(enr.status())) {
                String mark = marksByEnrollment.getOrDefault(enr.enrollmentId(), "—");
                tasks.add(new Task(
                    enr.subjectName().isEmpty() ? enr.subjectCode() : enr.subjectName(),
                    mark,
                    "—",
                    "—",
                    UserSession.get().isEnglish() ? "Completed" : "Ukončené",
                    "#16a34a"
                ));
            }
        }

        Platform.runLater(this::buildUI);
    }

    private void buildUI() {
        boolean en = UserSession.get().isEnglish();
        courseRoot.getChildren().clear();
        courseRoot.setSpacing(16);
        courseRoot.setPadding(new Insets(24, 28, 24, 28));

        // ── Header card ───────────────────────────────────────────
        HBox headerCard = new HBox(20);
        headerCard.getStyleClass().add("section-card");
        headerCard.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(8);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label title = new Label(en ? "My Assignments & Subjects" : "Moje zadania a predmety");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");

        long activeCount = enrollments.stream().filter(e -> "ACTIVE".equals(e.status())).count();
        long passedCount = enrollments.stream().filter(e -> "PASSED".equals(e.status())).count();
        int totalCredits = enrollments.stream().mapToInt(EnrollmentData::credits).sum();

        Label codeLabel = new Label(
            (en ? "Active subjects this semester: " : "Aktívne predmety: ") + activeCount +
            (en ? " • Completed: " : " • Ukončené: ") + passedCount +
            (en ? " • Total credits: " : " • Celkom kreditov: ") + totalCredits
        );
        codeLabel.getStyleClass().add("welcome-sub");

        // Tags
        HBox tags = new HBox(8);
        tags.setAlignment(Pos.CENTER_LEFT);
        tags.getChildren().addAll(
            tag(en ? "Current Semester" : "Aktuálny semester", "#dbeafe", "#1d4ed8"),
            tag(en ? activeCount + " Active" : activeCount + " Aktívnych", "#dcfce7", "#15803d"),
            tag(en ? totalCredits + " ECTS" : totalCredits + " kreditov", "#f0f9ff", "#0369a1")
        );

        info.getChildren().addAll(title, codeLabel, tags);

        // Upload section (static – ties into real backend upload when available)
        VBox uploadBox = new VBox(12);
        uploadBox.setAlignment(Pos.CENTER);
        uploadBox.setPadding(new Insets(0, 0, 0, 20));

        Button uploadBtn = new Button(en ? "📤 Upload File" : "📤 Nahrať súbor");
        uploadBtn.setStyle("-fx-background-color:#06b6d4;-fx-text-fill:white;-fx-font-weight:bold;" +
            "-fx-padding:12 24;-fx-font-size:14px;-fx-background-radius:10;-fx-cursor:hand;");

        Label fileNameLabel = new Label(en ? "No file selected" : "Žiadny vybraný súbor");
        fileNameLabel.getStyleClass().add("schedule-loc");

        uploadBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle(en ? "Select file to submit" : "Vybrať súbor na odovzdanie");
            File selected = fc.showOpenDialog(uploadBtn.getScene().getWindow());
            if (selected != null) {
                fileNameLabel.setText((en ? "Selected: " : "Vybrané: ") + selected.getName());
            }
        });

        uploadBox.getChildren().addAll(uploadBtn, fileNameLabel);
        headerCard.getChildren().addAll(info, uploadBox);

        // ── Subject/Task table ────────────────────────────────────
        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("section-card");

        Label tableTitle = new Label(en ? "Enrollment Status" : "Stav zápisov");
        tableTitle.getStyleClass().add("section-title");
        tableCard.getChildren().add(tableTitle);

        // Table header
        HBox hdr = taskRow(en ? "Subject" : "Predmet", en ? "Final" : "Záverečná",
                           en ? "Credits" : "Kredity", en ? "Status" : "Stav", "#94a3b8", true);
        hdr.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;" +
                     "-fx-border-width:0 0 1 0;-fx-padding:0 0 8 0;");
        tableCard.getChildren().add(hdr);

        if (tasks.isEmpty()) {
            Label empty = new Label(en
                ? "No enrollments found. Contact your administrator."
                : "Žiadne záznamy. Kontaktujte administrátora.");
            empty.getStyleClass().add("schedule-loc");
            empty.setPadding(new Insets(12, 0, 0, 0));
            tableCard.getChildren().add(empty);
        } else {
            for (int i = 0; i < tasks.size(); i++) {
                Task t = tasks.get(i);
                boolean last = i == tasks.size() - 1;
                HBox row = taskRow(t.name(), t.points(), "—", t.status(), t.statusColor(), false);
                if (!last) row.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;" +
                                        "-fx-border-width:0 0 1 0;");
                tableCard.getChildren().add(row);
            }
        }

        // ── Summary stats ─────────────────────────────────────────
        HBox sumRow = new HBox(12);
        long passed = tasks.stream().filter(t -> t.status().equals("Ukončené") || t.status().equals("Completed")).count();
        sumRow.getChildren().addAll(
            sumStat(String.valueOf(enrollments.size()), en ? "Enrolled"   : "Zapísané"),
            sumStat(String.valueOf(activeCount),        en ? "In Progress": "Prebieha"),
            sumStat(String.valueOf(passedCount),        en ? "Completed"  : "Ukončené"),
            sumStat(String.valueOf(totalCredits),       en ? "Total ECTS" : "Celkom kreditov")
        );

        courseRoot.getChildren().addAll(headerCard, tableCard, sumRow);
    }

    private HBox taskRow(String name, String pts, String cred, String status,
                         String statusColor, boolean header) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(header ? 4 : 10, 0, header ? 4 : 10, 0));

        Label nameLbl = new Label(name);
        HBox.setHgrow(nameLbl, Priority.ALWAYS);
        nameLbl.setWrapText(true);
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

        Label credLbl = new Label(cred);
        credLbl.setMinWidth(70);
        if (header) credLbl.getStyleClass().add("schedule-date");
        else        credLbl.getStyleClass().add("schedule-loc");

        Label stLbl = new Label(status);
        stLbl.setMinWidth(100);
        if (header) {
            stLbl.getStyleClass().add("schedule-date");
        } else {
            stLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8;" +
                "-fx-background-radius:6;-fx-background-color:" + statusColor + "22;" +
                "-fx-text-fill:" + statusColor + ";");
        }

        row.getChildren().addAll(nameLbl, ptsLbl, credLbl, stLbl);
        return row;
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
        lbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8;" +
            "-fx-background-radius:6;-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";");
        return lbl;
    }
}
