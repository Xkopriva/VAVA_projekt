package com.example.bais;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Teacher view – dynamically loads subjects the teacher guarantees,
 * then for each subject fetches the list of enrolled students with their marks.
 */
public class TeacherGradesController implements Initializable {

    @FXML private VBox teacherGradesRoot;

    // ── Data model ────────────────────────────────────────────────

    record StudentRow(int enrollmentId, int studentId, String status) {}

    record SubjectInfo(int subjectId, String code, String name) {}

    // subjectId → list of enrollments in that subject
    private final Map<Integer, SubjectInfo>    subjectMap   = new HashMap<>();
    private final Map<Integer, List<StudentRow>> enrollmentMap = new HashMap<>();
    // enrollmentId → finalMark
    private final Map<Integer, String>         markMap      = new HashMap<>();

    private final List<SubjectInfo> subjects = new ArrayList<>();

    // How many GET_ENROLLMENTS_FOR_SUBJECT responses we're still waiting for
    private volatile int pendingSubjectLoads = 0;

    private String subTeacherSubjects;
    private final Map<Integer, String> subjectEnrollmentSubs = new HashMap<>();
    private final Map<Integer, String> markSubs              = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showLoading();

        WebSocketClientService ws = WebSocketClientService.getInstance();
        subTeacherSubjects = ws.subscribe("TEACHER_SUBJECTS_LIST", this::handleSubjects);
        ws.sendAction("GET_MY_SUBJECTS", null);

        // Timeout 8s total
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            ws.unsubscribe(subTeacherSubjects);
            subjectEnrollmentSubs.values().forEach(ws::unsubscribe);
            markSubs.values().forEach(ws::unsubscribe);
            if (subjects.isEmpty()) {
                Platform.runLater(this::buildUI);
            }
        }, 8, TimeUnit.SECONDS);
    }

    // Step 1 — receive list of subjects this teacher guarantees
    private void handleSubjects(JsonNode node) {
        WebSocketClientService ws = WebSocketClientService.getInstance();
        ws.unsubscribe(subTeacherSubjects);

        JsonNode data = node.path("data");
        subjects.clear();
        subjectMap.clear();
        enrollmentMap.clear();
        markMap.clear();

        if (data.isArray()) {
            for (JsonNode s : data) {
                int    id   = s.path("id").asInt();
                String code = s.path("code").asText("SUB" + id);
                String name = code; // Subject model has no name — will be filled by translation if available

                SubjectInfo si = new SubjectInfo(id, code, name);
                subjects.add(si);
                subjectMap.put(id, si);
                enrollmentMap.put(id, new ArrayList<>());
            }
        }

        if (subjects.isEmpty()) {
            Platform.runLater(this::buildUI);
            return;
        }

        // Step 2 — fetch enrollments and marks
        fetchEnrollmentsAndMarks();
    }

    // We use a single permanent listener instead of one per request
    private String subjectEnrollmentSubId;
    private String markSubId;

    private void fetchEnrollmentsAndMarks() {
        WebSocketClientService ws = WebSocketClientService.getInstance();
        
        // Single permanent listener for all enrollments
        subjectEnrollmentSubId = ws.subscribe("SUBJECT_ENROLLMENTS", this::handleEnrollments);
        // Single permanent listener for all marks
        markSubId = ws.subscribe("INDEX_RECORD_DETAIL", this::handleMark);

        pendingSubjectLoads = subjects.size();
        for (SubjectInfo si : subjects) {
            ws.sendAction("GET_ENROLLMENTS_FOR_SUBJECT", Map.of("subjectId", si.subjectId()));
        }
    }

    private void handleEnrollments(JsonNode node) {
        WebSocketClientService ws = WebSocketClientService.getInstance();
        JsonNode payload = node.path("data");
        int subjectId = payload.path("subjectId").asInt(-1);
        if (subjectId == -1) return;

        JsonNode data = payload.path("enrollments");
        List<StudentRow> rows = enrollmentMap.computeIfAbsent(subjectId, k -> new ArrayList<>());
        rows.clear();

        if (data.isArray()) {
            for (JsonNode e : data) {
                int enrollmentId = e.path("id").asInt();
                int studentId    = e.path("studentId").asInt();
                String status    = e.path("status").asText("ACTIVE");
                rows.add(new StudentRow(enrollmentId, studentId, status));

                // Mark that we expect a final mark for this enrollment
                markSubs.put(enrollmentId, "pending");
                ws.sendAction("GET_MARKS_FOR_ENROLLMENT", Map.of("enrollmentId", enrollmentId));
            }
        }

        pendingSubjectLoads--;
        if (pendingSubjectLoads <= 0 && markSubs.isEmpty()) {
            // No marks to wait for — build now
            Platform.runLater(this::buildUI);
        }
    }

    private void handleMark(JsonNode node) {
        JsonNode payload = node.path("data");
        int enrollmentId = payload.path("enrollmentId").asInt(-1);
        if (enrollmentId == -1) return;

        JsonNode record = payload.path("record");
        String finalMark = "—";

        if (!record.isNull() && record.has("finalMark")) {
            finalMark = record.path("finalMark").asText("—");
        }
        markMap.put(enrollmentId, finalMark);
        markSubs.remove(enrollmentId);

        // If all marks and subjects loaded, build UI
        if (markSubs.isEmpty() && pendingSubjectLoads <= 0) {
            Platform.runLater(this::buildUI);
        }
    }

    // ── UI ────────────────────────────────────────────────────────

    private void showLoading() {
        teacherGradesRoot.getChildren().clear();
        teacherGradesRoot.setPadding(new Insets(24, 28, 24, 28));
        Label l = new Label("⏳  Načítavam hodnotenia...");
        l.setStyle("-fx-font-size:16px;-fx-text-fill:#64748b;");
        teacherGradesRoot.getChildren().add(l);
    }

    private void buildUI() {
        boolean en = UserSession.get().isEnglish();

        teacherGradesRoot.getChildren().clear();
        teacherGradesRoot.setSpacing(20);
        teacherGradesRoot.setPadding(new Insets(24, 28, 24, 28));

        // ── Page title ──────────────────────────────────────────
        VBox titleBlock = new VBox(4);
        Label title = new Label(en ? "Student Grades" : "Známky študentov");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(en
            ? "Results for subjects you guarantee this semester"
            : "Výsledky predmetov, ktoré tento semester garantujete");
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);

        // ── Summary stat cards ──────────────────────────────────
        int totalEnrollments = enrollmentMap.values().stream().mapToInt(List::size).sum();
        long doneCount = markMap.values().stream()
            .filter(m -> m != null && !m.equals("—") && !m.equals("FX") && !m.equals("FAIL"))
            .count();

        HBox stats = new HBox(12);
        stats.getChildren().addAll(
            statCard("📚", en ? "Subjects"    : "Predmety",   String.valueOf(subjects.size())),
            statCard("👥", en ? "Enrollments" : "Zápisov",    String.valueOf(totalEnrollments)),
            statCard("✅", en ? "Graded"      : "Ohodnotené", doneCount + " / " + totalEnrollments)
        );

        teacherGradesRoot.getChildren().addAll(titleBlock, stats);

        if (subjects.isEmpty()) {
            Label noData = new Label(en
                ? "No subjects found. Make sure you are set as a guarantor of at least one subject."
                : "Žiadne predmety neboli nájdené. Uistite sa, že ste nastavení ako garant aspoň jedného predmetu.");
            noData.getStyleClass().add("schedule-loc");
            noData.setWrapText(true);
            noData.setPadding(new Insets(12, 0, 0, 0));
            teacherGradesRoot.getChildren().add(noData);
            return;
        }

        // ── One card per subject ────────────────────────────────
        for (SubjectInfo si : subjects) {
            List<StudentRow> rows = enrollmentMap.getOrDefault(si.subjectId(), List.of());
            teacherGradesRoot.getChildren().add(buildSubjectCard(si, rows, en));
        }
    }

    private VBox buildSubjectCard(SubjectInfo si, List<StudentRow> rows, boolean en) {
        VBox card = new VBox(10);
        card.getStyleClass().add("section-card");

        // Card header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label code = new Label(si.code());
        code.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;" +
            "-fx-background-color:#ecfeff;-fx-padding:3 8;-fx-background-radius:6;");

        Label name = new Label(si.name());
        name.getStyleClass().add("section-title");
        HBox.setHgrow(name, Priority.ALWAYS);

        Label enrolledLbl = new Label(
            (en ? "Enrolled: " : "Zapísaných: ") + rows.size());
        enrolledLbl.getStyleClass().add("schedule-date");

        header.getChildren().addAll(code, name, enrolledLbl);
        card.getChildren().add(header);

        // Divider
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color:#e2e8f0;");
        card.getChildren().add(divider);

        if (rows.isEmpty()) {
            Label noStudents = new Label(en
                ? "No students enrolled in this subject."
                : "Žiadni študenti nie sú zapísaní na tento predmet.");
            noStudents.getStyleClass().add("schedule-loc");
            noStudents.setPadding(new Insets(8, 0, 0, 0));
            card.getChildren().add(noStudents);
            return card;
        }

        // Column header
        HBox colHeader = buildRow(true,
            en ? "Student ID" : "ID Stud.",
            en ? "Enrollment" : "Zápis",
            en ? "Status"     : "Stav",
            en ? "Final"      : "Záverečná");
        colHeader.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;" +
            "-fx-border-width:0 0 1 0;-fx-padding:0 0 6 0;");
        card.getChildren().add(colHeader);

        // Student rows
        for (int i = 0; i < rows.size(); i++) {
            StudentRow row = rows.get(i);
            boolean last = (i == rows.size() - 1);

            HBox rowBox = new HBox();
            rowBox.setAlignment(Pos.CENTER_LEFT);
            rowBox.setPadding(new Insets(9, 0, 9, 0));
            if (!last) rowBox.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;" +
                "-fx-border-width:0 0 1 0;");

            // Avatar + student ID
            Circle dot = new Circle(5, Color.web(avatarColor(row.studentId())));
            Label idLabel = new Label("ID: " + row.studentId());
            idLabel.getStyleClass().add("schedule-name");
            HBox nameBox = new HBox(6, dot, idLabel);
            nameBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(nameBox, Priority.ALWAYS);

            Label enrollLbl = new Label("#" + row.enrollmentId());
            enrollLbl.getStyleClass().add("schedule-loc");
            enrollLbl.setMinWidth(80);

            boolean passed = "PASSED".equals(row.status());
            boolean active = "ACTIVE".equals(row.status());
            boolean failed = "FAILED".equals(row.status());
            
            String statusText;
            if (passed) statusText = en ? "Finished" : "Ukončené";
            else if (failed) statusText = en ? "Failed" : "Neúspešne";
            else if (active) statusText = en ? "Active" : "Aktívny";
            else statusText = row.status();
            
            Label statusLbl = new Label(statusText);
            
            String bg = passed ? "#dcfce7" : (failed ? "#fee2e2" : "#fff7ed");
            String tc = passed ? "#15803d" : (failed ? "#b91c1c" : "#d97706");
            statusLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8 3 8;" +
                "-fx-background-radius:6;-fx-background-color:" + bg + ";-fx-text-fill:" + tc + ";");
            statusLbl.setMinWidth(90);

            String finalMark = markMap.getOrDefault(row.enrollmentId(), "—");
            Node markNode;
            if ("—".equals(finalMark)) {
                ComboBox<String> gradeCombo = new ComboBox<>();
                gradeCombo.getItems().addAll("A", "B", "C", "D", "E", "FX");
                gradeCombo.setPromptText("Známka");
                gradeCombo.setMinWidth(90);
                
                Button saveBtn = new Button("Uložiť");
                saveBtn.getStyleClass().addAll("btn-primary", "btn-sm");
                
                HBox editBox = new HBox(6, gradeCombo, saveBtn);
                editBox.setAlignment(Pos.CENTER_LEFT);
                
                saveBtn.setOnAction(e -> {
                    String selected = gradeCombo.getValue();
                    if (selected != null) {
                        Map<String, Object> payload = Map.of(
                            "enrollmentId", row.enrollmentId(),
                            "finalMark", selected
                        );
                        WebSocketClientService.getInstance().sendAction("RECORD_FINAL_MARK", payload);
                        
                        // Dynamically update the UI
                        Label newMarkLbl = new Label(selected);
                        newMarkLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" +
                            gradeColor(selected) + ";");
                        newMarkLbl.setMinWidth(80);
                        
                        int index = rowBox.getChildren().indexOf(editBox);
                        if (index != -1) {
                            rowBox.getChildren().set(index, newMarkLbl);
                        }
                        
                        boolean isFailed = "FX".equals(selected) || "FAIL".equals(selected);
                        String newStatusText;
                        if (isFailed) newStatusText = en ? "Failed" : "Neúspešne";
                        else newStatusText = en ? "Finished" : "Ukončené";
                        
                        String newBg = isFailed ? "#fee2e2" : "#dcfce7";
                        String newTc = isFailed ? "#b91c1c" : "#15803d";
                        
                        statusLbl.setText(newStatusText);
                        statusLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8 3 8;" +
                            "-fx-background-radius:6;-fx-background-color:" + newBg + ";-fx-text-fill:" + newTc + ";");
                    }
                });
                
                markNode = editBox;
            } else {
                Label markLbl = new Label(finalMark);
                markLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" +
                    gradeColor(finalMark) + ";");
                markLbl.setMinWidth(80);
                markNode = markLbl;
            }

            rowBox.getChildren().addAll(nameBox, enrollLbl, statusLbl, markNode);
            card.getChildren().add(rowBox);
        }

        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────

    private VBox statCard(String icon, String label, String value) {
        VBox card = new VBox(4);
        card.getStyleClass().add("section-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(180);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label ic  = new Label(icon);  ic.setStyle("-fx-font-size:22px;");
        Label lbl = new Label(label); lbl.getStyleClass().add("perf-course");
        Label val = new Label(value); val.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");

        card.getChildren().addAll(ic, lbl, val);
        return card;
    }

    private HBox buildRow(boolean header, String... cols) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        int[] widths = {-1, 80, 90, 80};
        for (int i = 0; i < cols.length; i++) {
            Label lbl = new Label(cols[i]);
            if (header) {
                lbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;");
                lbl.getStyleClass().add("schedule-date");
            }
            if (widths[i] > 0) lbl.setMinWidth(widths[i]);
            else HBox.setHgrow(lbl, Priority.ALWAYS);
            row.getChildren().add(lbl);
        }
        return row;
    }

    private String gradeColor(String g) {
        if (g == null || g.equals("—")) return "#94a3b8";
        if (g.startsWith("A") || g.equals("PASS"))  return "#16a34a";
        if (g.startsWith("B"))                       return "#2563eb";
        if (g.equals("FX") || g.equals("FAIL"))      return "#dc2626";
        return "#d97706";
    }

    private String avatarColor(int studentId) {
        String[] palette = {"#06b6d4","#8b5cf6","#f59e0b","#10b981","#ef4444","#3b82f6","#ec4899"};
        return palette[studentId % palette.length];
    }
}
