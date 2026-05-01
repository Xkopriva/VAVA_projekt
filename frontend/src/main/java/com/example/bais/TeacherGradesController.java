package com.example.bais;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Teacher view – shows each subject the teacher teaches together with
 * a table of student results.  No student's own GPA or personal progress
 * is shown here.
 */
public class TeacherGradesController implements Initializable {

    @FXML private VBox teacherGradesRoot;

    // ── Data model ────────────────────────────────────────────────

    record StudentGrade(String name, String id, String midterm, String finalGrade, String status) {}

    record Subject(String code, String name, int enrolled, List<StudentGrade> students) {}

    private final List<Subject> subjects = List.of(
        new Subject("PAS", "Programovanie a softvér I", 3, List.of(
            new StudentGrade("Adam Novák",    "s241001", "A",  "A",  "done"),
            new StudentGrade("Eva Kováčová",  "s241002", "B+", "B+", "done"),
            new StudentGrade("Peter Sloboda", "s241003", "B",  "—",  "ongoing")
        )),
        new Subject("DBS", "Databázové systémy", 3, List.of(
            new StudentGrade("Adam Novák",    "s241001", "B",  "B+", "done"),
            new StudentGrade("Lucia Mrázová", "s241004", "A-", "A",  "done"),
            new StudentGrade("Tomáš Kráľ",   "s241005", "C",  "—",  "ongoing")
        )),
        new Subject("ALM", "Algebra a matematika", 2, List.of(
            new StudentGrade("Eva Kováčová",  "s241002", "A",  "A",  "done"),
            new StudentGrade("Tomáš Kráľ",   "s241005", "B+", "—",  "ongoing")
        ))
    );

    // ── Init ──────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildUI();
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
            ? "Results for subjects you teach this semester"
            : "Výsledky predmetov, ktoré tento semester vyučujete");
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);

        // ── Summary stat cards ──────────────────────────────────
        HBox stats = new HBox(12);
        int totalStudents = subjects.stream().mapToInt(s -> s.students().size()).sum();
        long totalDone    = subjects.stream()
            .flatMap(s -> s.students().stream())
            .filter(g -> "done".equals(g.status())).count();

        stats.getChildren().addAll(
            statCard("📚", en ? "Subjects"    : "Predmety", String.valueOf(subjects.size())),
            statCard("👥", en ? "Enrollments" : "Zápisov",  String.valueOf(totalStudents)),
            statCard("✅", en ? "Finished"    : "Ukončených", totalDone + " / " + totalStudents)
        );

        teacherGradesRoot.getChildren().addAll(titleBlock, stats);

        // ── One card per subject ────────────────────────────────
        for (Subject subj : subjects) {
            teacherGradesRoot.getChildren().add(buildSubjectCard(subj, en));
        }
    }

    private VBox buildSubjectCard(Subject subj, boolean en) {
        VBox card = new VBox(10);
        card.getStyleClass().add("section-card");

        // Card header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        Label code = new Label(subj.code());
        code.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;" +
            "-fx-background-color:#ecfeff;-fx-padding:3 8;-fx-background-radius:6;");

        Label name = new Label(subj.name());
        name.getStyleClass().add("section-title");
        HBox.setHgrow(name, Priority.ALWAYS);

        Label enrolled = new Label((en ? "Enrolled: " : "Zapísaných: ") + subj.enrolled());
        enrolled.getStyleClass().add("schedule-date");

        header.getChildren().addAll(code, name, enrolled);
        card.getChildren().add(header);

        // Divider
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color:#e2e8f0;");
        card.getChildren().add(divider);

        // Column header row
        HBox colHeader = buildRow(true,
            en ? "Student"  : "Študent",
            en ? "ID"       : "ID",
            en ? "Midterm"  : "Priebežná",
            en ? "Final"    : "Záverečná",
            en ? "Status"   : "Stav");
        colHeader.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;" +
            "-fx-border-width:0 0 1 0;-fx-padding:0 0 6 0;");
        card.getChildren().add(colHeader);

        // Student rows
        List<StudentGrade> list = subj.students();
        for (int i = 0; i < list.size(); i++) {
            StudentGrade sg = list.get(i);
            boolean last = (i == list.size() - 1);

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(9, 0, 9, 0));
            if (!last) row.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;" +
                "-fx-border-width:0 0 1 0;");

            // Avatar circle + name
            Circle dot = new Circle(5, Color.web(avatarColor(sg.name())));
            Label nameLabel = new Label(sg.name());
            nameLabel.getStyleClass().add("schedule-name");
            HBox nameBox = new HBox(6, dot, nameLabel);
            nameBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(nameBox, Priority.ALWAYS);

            Label idLbl = new Label(sg.id());
            idLbl.getStyleClass().add("schedule-loc");
            idLbl.setMinWidth(80);

            Label mid = new Label(sg.midterm());
            mid.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + gradeColor(sg.midterm()) + ";");
            mid.setMinWidth(80);

            Label fin = new Label(sg.finalGrade());
            fin.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + gradeColor(sg.finalGrade()) + ";");
            fin.setMinWidth(80);

            boolean done = "done".equals(sg.status());
            Label statusLbl = new Label(done
                ? (en ? "Finished" : "Ukončené")
                : (en ? "Ongoing"  : "Prebieha"));
            statusLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8 3 8;" +
                "-fx-background-radius:6;" +
                (done ? "-fx-background-color:#dcfce7;-fx-text-fill:#15803d;"
                      : "-fx-background-color:#fff7ed;-fx-text-fill:#d97706;"));

            row.getChildren().addAll(nameBox, idLbl, mid, fin, statusLbl);
            card.getChildren().add(row);
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
        int[] widths = {-1, 80, 80, 80, 90};
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
        if (g.startsWith("A")) return "#16a34a";
        if (g.startsWith("B")) return "#2563eb";
        return "#d97706";
    }

    private String avatarColor(String name) {
        // Pick a consistent colour from the first char
        return switch (name.charAt(0)) {
            case 'A' -> "#06b6d4";
            case 'E' -> "#8b5cf6";
            case 'P' -> "#f59e0b";
            case 'L' -> "#10b981";
            default  -> "#64748b";
        };
    }
}
