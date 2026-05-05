package com.example.bais.controllers;

import com.example.bais.*;
import com.example.bais.models.*;
import com.example.bais.services.*;
import com.example.bais.components.*;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Teacher Grades view — subjects guaranteed by teacher, students, marks,
 * broadcast notification.
 * Supports: dark/light mode, SK/EN language, live data via WebSocket.
 */
public class TeacherGradesController implements Initializable {

    @FXML
    private VBox teacherGradesRoot;

    // ── Data model ──────────────────────────────────────────────────────────────
    record StudentRow(int enrollmentId, int studentId, String firstName, String lastName, String status) {
    }

    record SubjectInfo(int subjectId, String code, String name) {
    }

    private final Map<Integer, SubjectInfo> subjectMap = new HashMap<>();
    private final Map<Integer, List<StudentRow>> enrollmentMap = new HashMap<>();
    private final Map<Integer, String> studentNames = new HashMap<>();
    private final Map<Integer, String> markMap = new HashMap<>();
    private final List<SubjectInfo> subjects = new ArrayList<>();

    private volatile int pendingSubjectLoads = 0;
    private String subTeacherSubjects;
    private String subjectEnrollmentSubId;
    private String markSubId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showLoading();
        WebSocketClientService ws = WebSocketClientService.getInstance();
        subTeacherSubjects = ws.subscribe("TEACHER_SUBJECTS_LIST", this::handleSubjects);
        ws.sendAction("GET_MY_SUBJECTS", null);

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            ws.unsubscribe(subTeacherSubjects);
            if (subjectEnrollmentSubId != null)
                ws.unsubscribe(subjectEnrollmentSubId);
            if (markSubId != null)
                ws.unsubscribe(markSubId);
            if (subjects.isEmpty()) {
                Platform.runLater(this::buildUI);
            }
        }, 8, TimeUnit.SECONDS);
    }

    // ── Data loading ────────────────────────────────────────────────────────────

    private void handleSubjects(JsonNode node) {
        WebSocketClientService ws = WebSocketClientService.getInstance();
        ws.unsubscribe(subTeacherSubjects);

        JsonNode data = node.path("data");
        subjects.clear();
        subjectMap.clear();
        enrollmentMap.clear();
        markMap.clear();
        studentNames.clear();

        if (data.isArray()) {
            for (JsonNode s : data) {
                int id = s.path("id").asInt();
                String code = s.path("code").asText("SUB" + id);
                SubjectInfo si = new SubjectInfo(id, code, code);
                subjects.add(si);
                subjectMap.put(id, si);
                enrollmentMap.put(id, new ArrayList<>());
            }
        }

        if (subjects.isEmpty()) {
            Platform.runLater(this::buildUI);
            return;
        }

        // Najprv dotiahni studentov, potom enrollmenty
        String[] studSub = new String[1];
        studSub[0] = ws.subscribe("STUDENTS_LIST", studNode -> {
            ws.unsubscribe(studSub[0]);
            JsonNode students = studNode.path("data");
            if (students.isArray()) {
                for (JsonNode s : students) {
                    int id = s.path("id").asInt();
                    String name = (s.path("firstName").asText("") + " " + s.path("lastName").asText("")).trim();
                    studentNames.put(id, name.isEmpty() ? "Student #" + id : name);
                }
            }
            fetchEnrollmentsAndMarks();
        });
        ws.sendAction("GET_STUDENTS", null);
    }

    private void fetchEnrollmentsAndMarks() {
        WebSocketClientService ws = WebSocketClientService.getInstance();
        subjectEnrollmentSubId = ws.subscribe("SUBJECT_ENROLLMENTS", this::handleEnrollments);
        markSubId = ws.subscribe("INDEX_RECORD_DETAIL", this::handleMark);
        pendingSubjectLoads = subjects.size();
        for (SubjectInfo si : subjects) {
            ws.sendAction("GET_ENROLLMENTS_FOR_SUBJECT", Map.of("subjectId", si.subjectId()));
        }
    }

    private void handleEnrollments(JsonNode node) {
        JsonNode payload = node.path("data");
        int subjectId = payload.path("subjectId").asInt(-1);
        if (subjectId == -1)
            return;

        JsonNode data = payload.path("enrollments");
        List<StudentRow> rows = enrollmentMap.computeIfAbsent(subjectId, k -> new ArrayList<>());
        rows.clear();

        if (data.isArray()) {
            for (JsonNode e : data) {
                int enrollmentId = e.path("id").asInt();
                int studentId = e.path("studentId").asInt();
                String fullName = studentNames.getOrDefault(studentId, "Student #" + studentId);
                String status = e.path("status").asText("ACTIVE");
                rows.add(new StudentRow(enrollmentId, studentId, fullName, "", status));
                WebSocketClientService.getInstance()
                        .sendAction("GET_MARKS_FOR_ENROLLMENT", Map.of("enrollmentId", enrollmentId));
            }
        }

        pendingSubjectLoads--;
        if (pendingSubjectLoads <= 0)
            Platform.runLater(this::buildUI);
    }

    private void handleMark(JsonNode node) {
        JsonNode payload = node.path("data");
        int enrollmentId = payload.path("enrollmentId").asInt(-1);
        if (enrollmentId == -1)
            return;

        JsonNode record = payload.path("record");
        String finalMark = "—";
        if (!record.isNull() && record.has("finalMark")) {
            finalMark = record.path("finalMark").asText("—");
        }
        markMap.put(enrollmentId, finalMark);

        if (pendingSubjectLoads <= 0)
            Platform.runLater(this::buildUI);
    }

    // ── UI building ─────────────────────────────────────────────────────────────

    private void showLoading() {
        teacherGradesRoot.getChildren().clear();
        teacherGradesRoot.setPadding(new Insets(28, 28, 28, 28));
        boolean en = UserSession.get().isEnglish();
        Label l = new Label("⏳  " + (en ? "Loading grades..." : "Načítavam hodnotenia..."));
        l.setStyle("-fx-font-size:16px;-fx-text-fill:#64748b;");
        teacherGradesRoot.getChildren().add(l);
    }

    private void buildUI() {
        boolean en = UserSession.get().isEnglish();
        boolean dark = UserSession.get().isDarkMode();
        String cardBorder = dark ? "#1e293b" : "#f1f5f9";

        teacherGradesRoot.getChildren().clear();
        teacherGradesRoot.setSpacing(20);
        teacherGradesRoot.setPadding(new Insets(28, 28, 28, 28));

        // ── Page title ──────────────────────────────────────────────────
        VBox titleBlock = new VBox(4);
        Label title = new Label(en ? "Student Grades Management" : "Správa hodnotení študentov");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(en
                ? "Subjects you guarantee this semester — view and assess your students"
                : "Predmety, ktoré garantujete tento semester — prehľad a hodnotenie vašich študentov");
        sub.getStyleClass().add("welcome-sub");
        sub.setWrapText(true);
        titleBlock.getChildren().addAll(title, sub);

        // ── Summary stat cards ──────────────────────────────────────────
        int totalEnrollments = enrollmentMap.values().stream().mapToInt(List::size).sum();
        long graded = markMap.values().stream()
                .filter(m -> m != null && !m.equals("—")).count();

        HBox stats = new HBox(12);
        stats.getChildren().addAll(
                statCard("📚", en ? "Subjects" : "Predmety", String.valueOf(subjects.size()), "#6366f1"),
                statCard("👥", en ? "Students" : "Študentov", String.valueOf(totalEnrollments), "#06b6d4"),
                statCard("✅", en ? "Graded" : "Ohodnotené", graded + " / " + totalEnrollments, "#10b981"));

        teacherGradesRoot.getChildren().addAll(titleBlock, stats);

        if (subjects.isEmpty()) {
            Label noData = new Label(en
                    ? "ℹ️  No subjects found. You are not set as guarantor of any subject."
                    : "ℹ️  Žiadne predmety neboli nájdené. Nie ste nastavení ako garant žiadneho predmetu.");
            noData.getStyleClass().add("schedule-loc");
            noData.setWrapText(true);
            noData.setPadding(new Insets(16, 0, 0, 0));
            teacherGradesRoot.getChildren().add(noData);
            return;
        }

        // ── Broadcast notification panel ────────────────────────────────
        teacherGradesRoot.getChildren().add(buildBroadcastCard(en));

        // ── One card per subject ────────────────────────────────────────
        for (SubjectInfo si : subjects) {
            List<StudentRow> rows = enrollmentMap.getOrDefault(si.subjectId(), List.of());
            teacherGradesRoot.getChildren().add(buildSubjectCard(si, rows, en));
        }
    }

    // ── Broadcast notification card ─────────────────────────────────────────────

    private VBox buildBroadcastCard(boolean en) {
        VBox card = new VBox(12);
        card.getStyleClass().add("section-card");

        // Header
        HBox hdr = new HBox(8);
        hdr.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("📢");
        icon.setStyle("-fx-font-size:18px;");
        Label hdrTitle = new Label(en ? "Send Notification to Subject" : "Odoslať upozornenie pre predmet");
        hdrTitle.getStyleClass().add("section-title");
        hdr.getChildren().addAll(icon, hdrTitle);

        Region div1 = new Region();
        div1.setPrefHeight(1);
        div1.setMaxWidth(Double.MAX_VALUE);
        div1.setStyle("-fx-background-color:#e2e8f0;");

        // Subject picker
        Label subjectLbl = new Label(en ? "Subject:" : "Predmet:");
        subjectLbl.getStyleClass().add("text-primary");
        subjectLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");

        ComboBox<String> subjectPicker = new ComboBox<>();
        subjectPicker.setPromptText(en ? "Select subject..." : "Vyber predmet...");
        subjectPicker.setPrefWidth(240);
        for (SubjectInfo si : subjects) {
            subjectPicker.getItems().add(si.code() + " – " + si.subjectId());
        }

        HBox pickerRow = new HBox(10, subjectLbl, subjectPicker);
        pickerRow.setAlignment(Pos.CENTER_LEFT);

        // Message title
        Label msgTitleLbl = new Label(en ? "Title:" : "Nadpis:");
        msgTitleLbl.getStyleClass().add("text-primary");
        msgTitleLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");
        TextField msgTitle = new TextField();
        msgTitle.setPromptText(en ? "e.g. Lecture postponed..." : "napr. Prednáška presunutá...");
        msgTitle.getStyleClass().add("text-field-custom");
        msgTitle.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(msgTitle, Priority.ALWAYS);

        HBox titleRow = new HBox(10, msgTitleLbl, msgTitle);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // Message body
        Label msgBodyLbl = new Label(en ? "Message:" : "Správa:");
        msgBodyLbl.getStyleClass().add("text-primary");
        msgBodyLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");
        TextArea msgBody = new TextArea();
        msgBody.setPromptText(en ? "Details..." : "Podrobnosti...");
        msgBody.setPrefRowCount(2);
        msgBody.setWrapText(true);
        msgBody.getStyleClass().add("text-field-custom");

        Label statusLbl = new Label();
        statusLbl.setStyle("-fx-font-size:12px;");
        statusLbl.setVisible(false);

        Button sendBtn = new Button("  " + (en ? "Send to students" : "Odoslať študentom") + "  📤");
        sendBtn.setStyle("-fx-background-color:#06b6d4;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-padding:8 18;-fx-cursor:hand;-fx-font-size:13px;");
        sendBtn.setOnMouseEntered(e -> sendBtn.setStyle(sendBtn.getStyle().replace("#06b6d4", "#0891b2")));
        sendBtn.setOnMouseExited(e -> sendBtn.setStyle(sendBtn.getStyle().replace("#0891b2", "#06b6d4")));

        sendBtn.setOnAction(e -> {
            String selSubject = subjectPicker.getValue();
            String titleText = msgTitle.getText().trim();
            String bodyText = msgBody.getText().trim();

            if (selSubject == null || titleText.isBlank() || bodyText.isBlank()) {
                statusLbl.setText(en ? "⚠️  Fill in all fields." : "⚠️  Vyplňte všetky polia.");
                statusLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#d97706;");
                statusLbl.setVisible(true);
                return;
            }

            // Parse subjectId from picker text "CODE – subjectId"
            int sid = -1;
            try {
                sid = Integer.parseInt(selSubject.split("–")[1].trim());
            } catch (Exception ignored) {
            }
            if (sid == -1)
                return;

            Map<String, Object> payload = new HashMap<>();
            payload.put("subjectId", sid);
            payload.put("title", titleText);
            payload.put("message", bodyText);

            String[] sub = new String[1];
            sub[0] = WebSocketClientService.getInstance().subscribe("NOTIFICATION_SENT", resp -> {
                WebSocketClientService.getInstance().unsubscribe(sub[0]);
                int sent = resp.path("data").path("sent").asInt(0);
                Platform.runLater(() -> {
                    statusLbl.setText("✅  " + (en
                            ? "Sent to " + sent + " students!"
                            : "Odoslané " + sent + " študentom!"));
                    statusLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#16a34a;");
                    statusLbl.setVisible(true);
                    msgTitle.clear();
                    msgBody.clear();
                    subjectPicker.setValue(null);
                });
            });
            WebSocketClientService.getInstance().sendAction("BROADCAST_NOTIFICATION_TO_SUBJECT", payload);
        });

        HBox btnRow = new HBox(10, sendBtn, statusLbl);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(hdr, div1, pickerRow, titleRow, msgBodyLbl, msgBody, btnRow);
        return card;
    }

    // ── Subject card ────────────────────────────────────────────────────────────

    private VBox buildSubjectCard(SubjectInfo si, List<StudentRow> rows, boolean en) {
        VBox card = new VBox(10);
        card.getStyleClass().add("section-card");

        // Card header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label codeLbl = new Label(si.code());
        codeLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;" +
                "-fx-background-color:rgba(6,182,212,0.12);-fx-padding:3 10;-fx-background-radius:6;");

        Label nameLbl = new Label(si.name().equals(si.code()) ? "– " + si.subjectId() : si.name());
        nameLbl.getStyleClass().add("section-title");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        long gradedInSubject = rows.stream()
                .filter(r -> !"—".equals(markMap.getOrDefault(r.enrollmentId(), "—")))
                .count();
        Label enrolledLbl = new Label("👥 " + rows.size() + "   ✅ " + gradedInSubject + "/" + rows.size());
        enrolledLbl.getStyleClass().add("schedule-date");
        enrolledLbl.setStyle("-fx-font-size:12px;");

        header.getChildren().addAll(codeLbl, nameLbl, enrolledLbl);
        card.getChildren().add(header);

        // Divider
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color:#e2e8f0;");
        card.getChildren().add(divider);

        if (rows.isEmpty()) {
            Label noStudents = new Label(en
                    ? "ℹ️  No students enrolled in this subject."
                    : "ℹ️  Žiadni študenti nie sú zapísaní na tento predmet.");
            noStudents.getStyleClass().add("schedule-loc");
            noStudents.setPadding(new Insets(8, 0, 8, 0));
            card.getChildren().add(noStudents);
            return card;
        }

        // Column header
        HBox colHeader = buildTableHeader(en);
        colHeader.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;" +
                "-fx-border-width:0 0 1 0;-fx-padding:0 0 8 0;");
        card.getChildren().add(colHeader);

        // Student rows
        for (int i = 0; i < rows.size(); i++) {
            StudentRow row = rows.get(i);
            boolean last = (i == rows.size() - 1);
            card.getChildren().add(buildStudentRow(row, en, last));
        }

        return card;
    }

    // ── Table layout constants ──────────────────────────────────────────────────
    private static final double COL_STUDENT_WIDTH = 220;
    private static final double COL_ENROLLMENT_WIDTH = 80;
    private static final double COL_STATUS_WIDTH = 100;
    private static final double COL_GRADE_WIDTH = 180;

    private HBox buildTableHeader(boolean en) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(0);

        String[] labels = {
                en ? "Student" : "Študent",
                en ? "Enrollment" : "Zápis",
                en ? "Status" : "Stav",
                en ? "Final Grade" : "Záverečná"
        };
        double[] widths = { COL_STUDENT_WIDTH, COL_ENROLLMENT_WIDTH, COL_STATUS_WIDTH, COL_GRADE_WIDTH };
        for (int i = 0; i < labels.length; i++) {
            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#94a3b8;");
            lbl.setMinWidth(widths[i]);
            lbl.setMaxWidth(widths[i]);
            lbl.setPrefWidth(widths[i]);
            lbl.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().add(lbl);
        }
        return row;
    }

    private HBox buildStudentRow(StudentRow row, boolean en, boolean last) {
        HBox rowBox = new HBox();
        rowBox.setAlignment(Pos.CENTER_LEFT);
        rowBox.setPadding(new Insets(9, 0, 9, 0));
        rowBox.setSpacing(0);
        if (!last)
            rowBox.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;" +
                    "-fx-border-width:0 0 1 0;");

        // ── Column 1: Avatar + student name ─────────────────────────
        Circle dot = new Circle(5, Color.web(avatarColor(row.studentId())));
        String fullName = (row.firstName() + " " + row.lastName()).trim();
        if (fullName.isEmpty())
            fullName = "Student #" + row.studentId();
        Label nameLabel = new Label(fullName);
        nameLabel.getStyleClass().add("schedule-name");
        HBox nameBox = new HBox(7, dot, nameLabel);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.setMinWidth(COL_STUDENT_WIDTH);
        nameBox.setMaxWidth(COL_STUDENT_WIDTH);
        nameBox.setPrefWidth(COL_STUDENT_WIDTH);

        // ── Column 2: Enrollment ID ──────────────────────────────────
        Label enrollLbl = new Label("#" + row.enrollmentId());
        enrollLbl.getStyleClass().add("schedule-loc");
        enrollLbl.setMinWidth(COL_ENROLLMENT_WIDTH);
        enrollLbl.setMaxWidth(COL_ENROLLMENT_WIDTH);
        enrollLbl.setPrefWidth(COL_ENROLLMENT_WIDTH);
        enrollLbl.setAlignment(Pos.CENTER_LEFT);

        // ── Column 3: Status badge ───────────────────────────────────
        boolean passed = "PASSED".equals(row.status());
        boolean failed = "FAILED".equals(row.status());
        String statusText = passed ? (en ? "Passed" : "Ukončené")
                : failed ? (en ? "Failed" : "Neúspešne")
                        : (en ? "Active" : "Aktívny");
        String bg = passed ? "#dcfce7" : (failed ? "#fee2e2" : "#fff7ed");
        String tc = passed ? "#15803d" : (failed ? "#b91c1c" : "#d97706");

        Label statusLbl = new Label(statusText);
        statusLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 10;" +
                "-fx-background-radius:20;-fx-background-color:" + bg + ";-fx-text-fill:" + tc + ";");
        statusLbl.setMinWidth(COL_STATUS_WIDTH);
        statusLbl.setMaxWidth(COL_STATUS_WIDTH);
        statusLbl.setPrefWidth(COL_STATUS_WIDTH);
        statusLbl.setAlignment(Pos.CENTER_LEFT);

        // ── Column 4: Final mark (editable if not set) ───────────────
        String finalMark = markMap.getOrDefault(row.enrollmentId(), "—");
        javafx.scene.Node markNode;

        if ("—".equals(finalMark)) {
            ComboBox<String> gradeCombo = new ComboBox<>();
            gradeCombo.getItems().addAll("A", "B", "C", "D", "E", "FX");
            gradeCombo.setPromptText(en ? "Grade" : "Známka");
            gradeCombo.setPrefWidth(90);

            Button saveBtn = new Button(en ? "Save" : "Uložiť");
            saveBtn.setStyle("-fx-background-color:#06b6d4;-fx-text-fill:white;-fx-font-size:11px;" +
                    "-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:4 10;-fx-cursor:hand;");

            HBox editBox = new HBox(6, gradeCombo, saveBtn);
            editBox.setAlignment(Pos.CENTER_LEFT);
            editBox.setMinWidth(COL_GRADE_WIDTH);
            editBox.setMaxWidth(COL_GRADE_WIDTH);
            editBox.setPrefWidth(COL_GRADE_WIDTH);

            saveBtn.setOnAction(e -> {
                String selected = gradeCombo.getValue();
                if (selected == null)
                    return;

                Map<String, Object> payload = Map.of(
                        "enrollmentId", row.enrollmentId(),
                        "finalMark", selected);
                WebSocketClientService.getInstance().sendAction("RECORD_FINAL_MARK", payload);

                Label newMarkLbl = new Label(selected);
                newMarkLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" +
                        gradeColor(selected) + ";");
                newMarkLbl.setMinWidth(COL_GRADE_WIDTH);
                newMarkLbl.setMaxWidth(COL_GRADE_WIDTH);
                newMarkLbl.setPrefWidth(COL_GRADE_WIDTH);
                newMarkLbl.setAlignment(Pos.CENTER_LEFT);

                int idx = rowBox.getChildren().indexOf(editBox);
                if (idx != -1)
                    rowBox.getChildren().set(idx, newMarkLbl);

                boolean isFailed = "FX".equals(selected);
                statusLbl.setText(isFailed ? (en ? "Failed" : "Neúspešne") : (en ? "Passed" : "Ukončené"));
                String newBg = isFailed ? "#fee2e2" : "#dcfce7";
                String newTc = isFailed ? "#b91c1c" : "#15803d";
                statusLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 10;" +
                        "-fx-background-radius:20;-fx-background-color:" + newBg + ";-fx-text-fill:" + newTc + ";");
            });

            markNode = editBox;
        } else {
            Label markLbl = new Label(finalMark);
            markLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" +
                    gradeColor(finalMark) + ";");
            markLbl.setMinWidth(COL_GRADE_WIDTH);
            markLbl.setMaxWidth(COL_GRADE_WIDTH);
            markLbl.setPrefWidth(COL_GRADE_WIDTH);
            markLbl.setAlignment(Pos.CENTER_LEFT);
            markNode = markLbl;
        }

        rowBox.getChildren().addAll(nameBox, enrollLbl, statusLbl, markNode);
        return rowBox;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private VBox statCard(String icon, String label, String value, String accent) {
        VBox card = new VBox(4);
        card.getStyleClass().add("section-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(160);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label ic = new Label(icon);
        ic.setStyle("-fx-font-size:24px;");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("perf-course");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + accent + ";");

        card.getChildren().addAll(ic, lbl, val);
        return card;
    }

    private String gradeColor(String g) {
        if (g == null || g.equals("—"))
            return "#94a3b8";
        if (g.equals("A"))
            return "#16a34a";
        if (g.equals("B"))
            return "#2563eb";
        if (g.equals("C") || g.equals("D") || g.equals("E"))
            return "#d97706";
        if (g.equals("FX") || g.equals("FAIL"))
            return "#dc2626";
        if (g.equals("PASS"))
            return "#16a34a";
        return "#64748b";
    }

    private String avatarColor(int studentId) {
        String[] palette = { "#06b6d4", "#8b5cf6", "#f59e0b", "#10b981", "#ef4444", "#3b82f6", "#ec4899" };
        return palette[studentId % palette.length];
    }
}