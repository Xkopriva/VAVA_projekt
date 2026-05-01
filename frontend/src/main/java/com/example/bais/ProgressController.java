package com.example.bais;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ProgressController implements Initializable {

    @FXML private VBox progressRoot;

    record SemCourse(String name, String grade, int credits) {}
    record Semester(String name, String term, int totalCredits, String avg, List<SemCourse> courses) {}

    private final List<Semester> semesters = List.of(
        new Semester("Zimný semester 2023/24", "ZS", 30, "1.2", List.of(
            new SemCourse("Matematika I",          "A",  5),
            new SemCourse("Programovanie základy",  "B+", 6),
            new SemCourse("Fyzika I",              "A-", 4),
            new SemCourse("Logika a diskrétna mat.","A",  5),
            new SemCourse("Angličtina B2",          "B",  4)
        )),
        new Semester("Letný semester 2023/24", "LS", 28, "1.4", List.of(
            new SemCourse("Matematika II",     "B+", 5),
            new SemCourse("Algoritmy a DS",    "A",  6),
            new SemCourse("Siete I",           "B",  4),
            new SemCourse("DB základy",        "A-", 5),
            new SemCourse("Fyzika II",         "B+", 4)
        )),
        new Semester("Zimný semester 2024/25 (prebiehajúci)", "ZS", 25, "—", List.of(
            new SemCourse("PAS",  "A",  6),
            new SemCourse("DBS",  "B+", 4),
            new SemCourse("PSI",  "A",  4),
            new SemCourse("VAVA", "—",  6),
            new SemCourse("ALM",  "—",  5)
        ))
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildUI();
    }

    private void buildUI() {
        progressRoot.getChildren().clear();
        progressRoot.setSpacing(16);
        progressRoot.setPadding(new Insets(24, 28, 24, 28));

        // Study Plan Header
        VBox studyPlanBox = new VBox(12);
        studyPlanBox.getStyleClass().add("section-card");

        Label studyPlanTitle = new Label("Študijný plán");
        studyPlanTitle.setStyle("-fx-font-size:20px;-fx-font-weight:bold;");
        HBox.setHgrow(studyPlanTitle, Priority.ALWAYS);

        Button planButton = new Button("📋 Plánovať štúdium");
        planButton.setStyle("-fx-background-color:#06b6d4;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:8 16;-fx-font-size:12px;-fx-background-radius:8;-fx-cursor:hand;");
        planButton.setOnAction(e -> System.out.println("Plánovanie štúdia - funkcia v príprave"));

        HBox studyPlanHeader = new HBox(studyPlanTitle, planButton);
        studyPlanHeader.setAlignment(Pos.CENTER_LEFT);

        VBox studyInfo = new VBox(6);
        Label studyProgram = new Label("Študium: FIIT B-INFO deň [sem 4, roč 2]");
        studyProgram.setStyle("-fx-font-size:14px;-fx-font-weight:bold;");
        Label studyCredits = new Label("Kredity: 73 získaných z 180 povinných (CHÝBA 107 kr.)");
        studyCredits.setStyle("-fx-font-size:13px;");
        Label studyPeriod = new Label("Počiatočné obdobie: ZS 2024/2025 - FIIT");
        studyPeriod.setStyle("-fx-font-size:13px;");
        studyInfo.getChildren().addAll(studyProgram, studyCredits, studyPeriod);

        studyPlanBox.getChildren().addAll(studyPlanHeader, studyInfo);

        // Title + stats row
        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(4);
        Label title = new Label("Progress štúdium");
        title.setStyle("-fx-font-size:32px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label("Bc. Informatika • 2. ročník • STU Bratislava");
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        HBox statsRow = new HBox(12);
        statsRow.setAlignment(Pos.CENTER_RIGHT);
        statsRow.getChildren().addAll(
            miniStat("58 / 180", "Kreditov"),
            miniStat("1.28",     "GPA priemer"),
            miniStat("32%",      "Hotovo")
        );

        titleRow.getChildren().addAll(titleBlock, statsRow);

        // Overall progress bar
        VBox progressSection = new VBox(8);
        progressSection.getStyleClass().add("section-card");

        HBox barHeader = new HBox();
        barHeader.setAlignment(Pos.CENTER_LEFT);
        Label barLabel = new Label("Celkový progres štúdia");
        barLabel.getStyleClass().add("section-title");
        HBox.setHgrow(barLabel, Priority.ALWAYS);
        Label barPct = new Label("32%  (58 / 180 kreditov)");
        barPct.getStyleClass().add("schedule-date");
        barHeader.getChildren().addAll(barLabel, barPct);

        StackPane barBg = new StackPane();
        barBg.setPrefHeight(10);
        barBg.setStyle("-fx-background-color:#e2e8f0;-fx-background-radius:5;");
        StackPane barFill = new StackPane();
        barFill.setStyle("-fx-background-color:#06b6d4;-fx-background-radius:5;");
        barFill.setPrefHeight(10);
        barFill.setPrefWidth(0);   // set via binding below
        barBg.getChildren().add(barFill);
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
        // Bind to 32% of barBg width
        barFill.prefWidthProperty().bind(barBg.widthProperty().multiply(0.32));

        progressSection.getChildren().addAll(barHeader, barBg);

        // Semester cards
        VBox semCards = new VBox(14);
        for (Semester sem : semesters) {
            semCards.getChildren().add(buildSemCard(sem));
        }

        progressRoot.getChildren().addAll(studyPlanBox, titleRow, progressSection, semCards);
    }

    private VBox buildSemCard(Semester sem) {
        VBox card = new VBox(12);
        card.getStyleClass().add("section-card");

        // Semester header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(sem.term());
        badge.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:3 8 3 8;" +
            "-fx-background-radius:6;-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;");

        Label name = new Label("  " + sem.name());
        name.getStyleClass().add("section-title");
        HBox.setHgrow(name, Priority.ALWAYS);

        Label cr = new Label(sem.totalCredits() + " kreditov");
        cr.getStyleClass().add("schedule-date");
        cr.setPadding(new Insets(0, 12, 0, 0));

        Label avg = new Label("Priemer: " + sem.avg());
        avg.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");

        header.getChildren().addAll(badge, name, cr, avg);
        card.getChildren().add(header);

        // Separator
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#e2e8f0;");
        card.getChildren().add(sep);

        // Column header
        HBox colHeader = courseRow("Predmet", "Kredity", "Zn.", true);
        card.getChildren().add(colHeader);

        // Course rows
        for (int i = 0; i < sem.courses().size(); i++) {
            SemCourse c = sem.courses().get(i);
            boolean last = i == sem.courses().size() - 1;
            HBox row = courseRow(c.name(), String.valueOf(c.credits()), c.grade(), false);
            if (!last) row.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;");
            card.getChildren().add(row);
        }

        return card;
    }

    private HBox courseRow(String name, String credits, String grade, boolean header) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(header ? 4 : 8, 0, header ? 4 : 8, 0));

        Label nameLbl = new Label(name);
        if (header) nameLbl.getStyleClass().add("schedule-date");
        else        nameLbl.getStyleClass().add("schedule-name");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        Label crLbl = new Label(credits);
        if (header) crLbl.getStyleClass().add("schedule-date");
        else        crLbl.getStyleClass().add("schedule-loc");
        crLbl.setMinWidth(70);

        Label gradeLbl = new Label(grade);
        gradeLbl.setMinWidth(50);
        if (header) {
            gradeLbl.getStyleClass().add("schedule-date");
        } else {
            gradeLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + gradeColor(grade) + ";");
        }

        row.getChildren().addAll(nameLbl, crLbl, gradeLbl);
        return row;
    }

    private VBox miniStat(String value, String label) {
        VBox card = new VBox(2);
        card.getStyleClass().add("section-card");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(90);

        Label val = new Label(value);
        val.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("perf-course");
        card.getChildren().addAll(val, lbl);
        return card;
    }

    private String gradeColor(String g) {
        if (g == null || g.equals("—")) return "#94a3b8";
        if (g.startsWith("A")) return "#16a34a";
        if (g.startsWith("B")) return "#2563eb";
        return "#d97706";
    }
}
